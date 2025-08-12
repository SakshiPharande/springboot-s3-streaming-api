package com.example.fileuploadservice.service;

import com.example.fileuploadservice.dto.ProgressInfo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressTrackingService {

    private final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();
    private final Map<String, Long> startTimeMap = new ConcurrentHashMap<>();

    public void initializeProgress(String id, String fileName, long totalSize) {
        ProgressInfo progress = new ProgressInfo(fileName, totalSize, 0, "INITIALIZING");
        progressMap.put(id, progress);
        startTimeMap.put(id, System.currentTimeMillis());
    }

    public void updateProgress(String id, long processedBytes) {
        ProgressInfo progress = progressMap.get(id);
        if (progress != null) {
            progress.setProcessedBytes(processedBytes);
            progress.setPercentageComplete(
                    progress.getTotalBytes() > 0 ?
                            (double) processedBytes / progress.getTotalBytes() * 100 : 0
            );

            // Calculate speed and time estimates
            long currentTime = System.currentTimeMillis();
            Long startTime = startTimeMap.get(id);

            if (startTime != null) {
                long timeElapsed = currentTime - startTime;
                progress.setTimeElapsed(timeElapsed / 1000); // in seconds

                if (timeElapsed > 0) {
                    double bytesPerSecond = (double) processedBytes / (timeElapsed / 1000.0);
                    progress.setSpeed(formatSpeed(bytesPerSecond));

                    if (bytesPerSecond > 0) {
                        long remainingBytes = progress.getTotalBytes() - processedBytes;
                        long estimatedTimeRemaining = (long) (remainingBytes / bytesPerSecond);
                        progress.setEstimatedTimeRemaining(estimatedTimeRemaining);
                    }
                }
            }

            if (processedBytes >= progress.getTotalBytes()) {
                progress.setStatus("COMPLETED");
            } else {
                progress.setStatus("IN_PROGRESS");
            }
        }
    }

    public void setStatus(String id, String status) {
        ProgressInfo progress = progressMap.get(id);
        if (progress != null) {
            progress.setStatus(status);
        }
    }

    public ProgressInfo getProgress(String id) {
        return progressMap.get(id);
    }

    public void removeProgress(String id) {
        progressMap.remove(id);
        startTimeMap.remove(id);
    }

    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond < 1024) return String.format("%.2f B/s", bytesPerSecond);
        if (bytesPerSecond < 1024 * 1024) return String.format("%.2f KB/s", bytesPerSecond / 1024);
        if (bytesPerSecond < 1024 * 1024 * 1024) return String.format("%.2f MB/s", bytesPerSecond / (1024 * 1024));
        return String.format("%.2f GB/s", bytesPerSecond / (1024 * 1024 * 1024));
    }
}
