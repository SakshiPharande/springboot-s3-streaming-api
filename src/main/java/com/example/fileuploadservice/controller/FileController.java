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
/*
    @GetMapping("/download/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileName) {
        try {
            if (!fileService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();
            }

            String downloadId = UUID.randomUUID().toString();
            var fileStream = fileService.downloadFile(fileName, downloadId);
            long fileSize = fileService.getFileSize(fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            headers.add("X-Download-Id", downloadId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(fileStream));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
*/
    @GetMapping("/download/{fileName}")
    public ResponseEntity<FileDownloadResponse> downloadAndSaveFile(@PathVariable String fileName) {
        try {
            if (!fileService.fileExists(fileName)) {
                return ResponseEntity.notFound().build();
            }

            String downloadId = UUID.randomUUID().toString();
            FileDownloadResponse response = fileService.downloadFileAndSave(fileName, downloadId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileDownloadResponse(fileName, "0 B", null, "Error: " + e.getMessage()));
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