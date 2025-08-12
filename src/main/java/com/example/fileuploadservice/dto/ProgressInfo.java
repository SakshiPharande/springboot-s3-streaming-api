package com.example.fileuploadservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressInfo {

    private String fileName;
    private long totalBytes;
    private long processedBytes;
    private double percentageComplete;
    private String status;
    private String speed;
    private long timeElapsed;
    private long estimatedTimeRemaining;

    public ProgressInfo(String fileName, long totalBytes, long processedBytes, String status) {
        this.fileName = fileName;
        this.totalBytes = totalBytes;
        this.processedBytes = processedBytes;
        this.status = status;
        this.percentageComplete = totalBytes > 0 ? (double) processedBytes / totalBytes * 100 : 0;
    }

    public String getFormattedSize() {
        return formatBytes(processedBytes) + " / " + formatBytes(totalBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}