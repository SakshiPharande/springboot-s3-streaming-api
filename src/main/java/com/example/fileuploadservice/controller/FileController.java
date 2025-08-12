package com.example.fileuploadservice.controller;


import com.example.fileuploadservice.dto.FileInfo;
import com.example.fileuploadservice.dto.FileUploadResponse;
import com.example.fileuploadservice.dto.ProgressInfo;
import com.example.fileuploadservice.service.FileStreamingService;
import com.example.fileuploadservice.service.ProgressTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStreamingService fileStreamingService;

    @Autowired
    private ProgressTrackingService progressTrackingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            logger.info("Received upload request for file: {} ({})", file.getOriginalFilename(), file.getSize());

            FileUploadResponse response = fileStreamingService.uploadFileWithProgress(file);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload-async")
    public ResponseEntity<?> uploadFileAsync(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String uploadId = UUID.randomUUID().toString();

            // Start async upload
            CompletableFuture.runAsync(() -> {
                try {
                    fileStreamingService.uploadFileWithProgress(file);
                } catch (Exception e) {
                    logger.error("Async upload failed", e);
                    progressTrackingService.setStatus(uploadId, "FAILED");
                }
            });

            return ResponseEntity.ok().body(new FileUploadResponse(
                    file.getOriginalFilename(),
                    null,
                    file.getSize(),
                    uploadId,
                    "STARTED",
                    "Upload started. Check progress at /api/files/progress/" + uploadId
            ));

        } catch (Exception e) {
            logger.error("Async upload initialization failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileName) {
        try {
            if (!fileStreamingService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();
            }

            String downloadId = UUID.randomUUID().toString();
            InputStream fileStream = fileStreamingService.downloadFileWithProgress(fileName, downloadId);
            long fileSize = fileStreamingService.getFileSize(fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            headers.add("X-Download-Id", downloadId);
            headers.setContentLength(fileSize);

            logger.info("Starting download for file: {} with ID: {}", fileName, downloadId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(fileStream));

        } catch (Exception e) {
            logger.error("Download failed for file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download-with-progress/{fileName}")
    public ResponseEntity<?> downloadFileWithProgress(@PathVariable String fileName) {
        try {
            if (!fileStreamingService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();

            }

            String downloadId = UUID.randomUUID().toString();
            long fileSize = fileStreamingService.getFileSize(fileName);

            // Initialize progress tracking
            progressTrackingService.initializeProgress(downloadId, fileName, fileSize);

            return ResponseEntity.ok().body(new FileUploadResponse(
                    fileName,
                    "/api/files/download/" + fileName,
                    fileSize,
                    downloadId,
                    "READY",
                    "Download ready. Check progress at /api/files/progress/" + downloadId
            ));

        } catch (Exception e) {
            logger.error("Download preparation failed for file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Download preparation failed: " + e.getMessage());
        }
    }

    @GetMapping("/progress/{id}")
    public ResponseEntity<ProgressInfo> getProgress(@PathVariable String id) {
        ProgressInfo progress = progressTrackingService.getProgress(id);

        if (progress == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(progress);
    }

    @DeleteMapping("/progress/{id}")
    public ResponseEntity<String> clearProgress(@PathVariable String id) {
        progressTrackingService.removeProgress(id);
        return ResponseEntity.ok("Progress cleared for ID: " + id);
    }

    @GetMapping("/exists/{fileName}")
    public ResponseEntity<Boolean> fileExists(@PathVariable String fileName) {
        boolean exists = fileStreamingService.fileExists(fileName);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/size/{fileName}")
    public ResponseEntity<?> getFileSize(@PathVariable String fileName) {
        try {
            if (!fileStreamingService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();
            }

            long size = fileStreamingService.getFileSize(fileName);
            return ResponseEntity.ok().body(new FileInfo(fileName, size));

        } catch (Exception e) {
            logger.error("Failed to get file size for: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get file size: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File service is running");
    }


}
