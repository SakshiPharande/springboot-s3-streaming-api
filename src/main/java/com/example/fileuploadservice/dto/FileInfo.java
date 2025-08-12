package com.example.fileuploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class FileInfo {
    private String fileName;
    private long size;
    private String formattedSize;

    public FileInfo(String fileName, long size) {
        this.fileName = fileName;
        this.size = size;
        this.formattedSize = formatBytes(size);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
