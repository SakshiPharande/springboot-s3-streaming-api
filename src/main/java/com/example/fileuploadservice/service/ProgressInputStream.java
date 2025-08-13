package com.example.fileuploadservice.service;


import com.example.fileuploadservice.dto.Progress;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ProgressInputStream extends InputStream {
    private final InputStream inputStream;
    private final String progressId;
    private final Map<String, Progress> progressMap;
    private long bytesRead = 0;

    public ProgressInputStream(InputStream inputStream, String progressId, Map<String, Progress> progressMap) {
        this.inputStream = inputStream;
        this.progressId = progressId;
        this.progressMap = progressMap;
    }

    @Override
    public int read() throws IOException {
        int data = inputStream.read();
        if (data != -1) {
            bytesRead++;
            updateProgress();
        }
        return data;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesReadNow = inputStream.read(b, off, len);
        if (bytesReadNow > 0) {
            bytesRead += bytesReadNow;
            updateProgress();
        }
        return bytesReadNow;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        Progress progress = progressMap.get(progressId);
        if (progress != null) {
            progress.updateProgress(bytesRead);
        }
        inputStream.close();
    }

    private void updateProgress() {
        Progress progress = progressMap.get(progressId);
        if (progress != null) {
            progress.updateProgress(bytesRead);
//            System.out.println("Read so far: " + bytesRead + " bytes");
        }
    }

}