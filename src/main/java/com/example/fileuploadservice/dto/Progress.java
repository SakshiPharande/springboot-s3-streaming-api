package com.example.fileuploadservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Progress {

    private String fileName;
    private long totalBytes;
    private long uploadedBytes;
    private double percentage;
    private String status;

    public Progress(String fileName, long totalBytes) {
        this.fileName = fileName;
        this.totalBytes = totalBytes;
        this.uploadedBytes = 0;
        this.percentage = 0.0;
        this.status = "STARTING";
    }

    public void updateProgress(long uploadedBytes) {
        this.uploadedBytes = uploadedBytes;
        this.percentage = totalBytes > 0 ? (double) uploadedBytes / totalBytes * 100 : 0;
        this.status = uploadedBytes >= totalBytes ? "COMPLETED" : "IN_PROGRESS";
    }

    public String getSizeInfo() {
        return formatBytes(uploadedBytes) + " / " + formatBytes(totalBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
