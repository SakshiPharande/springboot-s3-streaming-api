package com.example.fileuploadservice.service;

import com.example.fileuploadservice.dto.FileUploadResponse;
import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class FileStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(FileStreamingService.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private ProgressTrackingService progressTrackingService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${app.progress.update-interval:1024}")
    private int progressUpdateInterval;

    public void initializeBucket() throws Exception {
        // Create bucket if it doesn't exist
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Created bucket: {}", bucketName);
        }
    }

    public FileUploadResponse uploadFileWithProgress(MultipartFile file) throws Exception {
        String uploadId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();

        logger.info("Starting upload for file: {} with ID: {}", fileName, uploadId);

        // Initialize progress tracking
        progressTrackingService.initializeProgress(uploadId, fileName, fileSize);

        try {
            initializeBucket();

            // Create progress tracking input stream
            ProgressTrackingInputStream progressStream = new ProgressTrackingInputStream(
                    file.getInputStream(), uploadId, progressTrackingService, progressUpdateInterval
            );

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(progressStream, fileSize, -1)
                            .contentType(file.getContentType())
                            .build()
            );

            progressTrackingService.setStatus(uploadId, "COMPLETED");
            logger.info("Upload completed for file: {}", fileName);

            return new FileUploadResponse(
                    fileName,
                    "/api/files/download/" + fileName,
                    fileSize,
                    uploadId,
                    "SUCCESS",
                    "File uploaded successfully"
            );

        } catch (Exception e) {
            progressTrackingService.setStatus(uploadId, "FAILED");
            logger.error("Upload failed for file: {}", fileName, e);
            throw e;
        }
    }

    public InputStream downloadFileWithProgress(String fileName, String downloadId) throws Exception {
        try {
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );

            // Get file size for progress tracking
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );

            long fileSize = stat.size();
            progressTrackingService.initializeProgress(downloadId, fileName, fileSize);

            return new ProgressTrackingInputStream(response, downloadId, progressTrackingService, progressUpdateInterval);

        } catch (Exception e) {
            logger.error("Download failed for file: {}", fileName, e);
            throw e;
        }
    }

    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getFileSize(String fileName) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
        return stat.size();
    }
}

