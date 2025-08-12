package com.example.fileuploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

//response
public class FileUploadResponse {

    private String fileName;
    private String fileUrl;
    private long fileSize;
    private String uploadId;
    private String status;
    private String message;
}
