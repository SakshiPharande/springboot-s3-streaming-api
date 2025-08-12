package com.example.fileuploadservice.service;

import java.io.IOException;
import java.io.InputStream;

public class ProgressTrackingInputStream extends InputStream {
    private final InputStream delegate;
    private final String progressId;
    private final ProgressTrackingService progressService;
    private final int updateInterval;
    private long bytesRead = 0;
    private long lastUpdateBytes = 0;

    public ProgressTrackingInputStream(InputStream delegate, String progressId,
                                       ProgressTrackingService progressService, int updateInterval) {
        this.delegate = delegate;
        this.progressId = progressId;
        this.progressService = progressService;
        this.updateInterval = updateInterval;
    }

    @Override
    public int read() throws IOException {
        int result = delegate.read();
        if (result != -1) {
            updateProgress(1);
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = delegate.read(b, off, len);
        if (result > 0) {
            updateProgress(result);
        }
        return result;
    }

    private void updateProgress(int bytesReadNow) {
        bytesRead += bytesReadNow;

        // Update progress every updateInterval bytes to avoid too frequent updates
        if (bytesRead - lastUpdateBytes >= updateInterval || bytesReadNow == 0) {
            progressService.updateProgress(progressId, bytesRead);
            lastUpdateBytes = bytesRead;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } finally {
            // Final progress update
            progressService.updateProgress(progressId, bytesRead);
        }
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        long result = delegate.skip(n);
        if (result > 0) {
            updateProgress((int) result);
        }
        return result;
    }
}
