package com.example.fileuploadservice.controller;


import com.example.fileuploadservice.dto.FileDownloadResponse;
import com.example.fileuploadservice.dto.Progress;
import com.example.fileuploadservice.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//controller
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String uploadId = UUID.randomUUID().toString();
            String fileName = fileService.uploadFile(file, uploadId);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("uploadId", uploadId);
            response.put("fileSize", file.getSize());
            response.put("message", "Upload completed successfully");
            response.put("status", "SUCCESS");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

// Method 1
    @GetMapping("/download-save/{fileName}")
    public ResponseEntity<StreamingResponseBody> downloadAndSaveFile(@PathVariable String fileName) {
        try {
            if (!fileService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();
            }

            StreamingResponseBody stream = outputStream -> {
                try {
                    fileService.downloadFileAndSaveAndStream(fileName, outputStream);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

// Method 2
    @GetMapping("/download/{fileName}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String fileName) {
        try {
            if (!fileService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();
            }

            StreamingResponseBody stream = outputStream -> {
                try {
                    fileService.downloadFileFromMinio(fileName, outputStream);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }



    @GetMapping("/progress/{id}")
    public ResponseEntity<Progress> getProgress(@PathVariable String id) {
        Progress progress = fileService.getProgress(id);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }

    @DeleteMapping("/progress/{id}")
    public ResponseEntity<String> clearProgress(@PathVariable String id) {
        fileService.removeProgress(id);
        return ResponseEntity.ok("Progress cleared for ID: " + id);
    }

    @GetMapping("/exists/{fileName}")
    public ResponseEntity<Boolean> fileExists(@PathVariable String fileName) {
        boolean exists = fileService.fileExists(fileName);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File service is running");
    }
}