package com.example.fileuploadservice.service;

import com.example.fileuploadservice.dto.Progress;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final Map<String, Progress> progressMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            System.out.println("Checking bucket: " + bucketName);
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                System.out.println("Creating bucket: " + bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                System.out.println("Bucket created successfully: " + bucketName);
            } else {
                System.out.println("Bucket already exists: " + bucketName);
            }
        } catch (Exception e) {
            System.err.println("Error with bucket operations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create bucket: " + e.getMessage(), e);
        }
    }

    public String uploadFile(MultipartFile file, String uploadId) throws Exception {
        String fileName = file.getOriginalFilename();
        Progress progress = new Progress(fileName, file.getSize());
        progressMap.put(uploadId, progress);

        System.out.println("Starting upload for: " + fileName + " (Size: " + file.getSize() + " bytes)");

        // Create progress tracking stream
        ProgressInputStream progressStream = new ProgressInputStream(
                file.getInputStream(), uploadId, progressMap
        );

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(progressStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        progress.setStatus("COMPLETED");
        System.out.println("Upload completed for: " + fileName);
        return fileName;
    }

    public InputStream downloadFile(String fileName, String downloadId) throws Exception {
        GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );

        long fileSize = getFileSize(fileName);
        Progress progress = new Progress(fileName, fileSize);
        progressMap.put(downloadId, progress);

        return new ProgressInputStream(response, downloadId, progressMap);
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

    public Progress getProgress(String id) {
        return progressMap.get(id);
    }

    public void removeProgress(String id) {
        progressMap.remove(id);
    }
}