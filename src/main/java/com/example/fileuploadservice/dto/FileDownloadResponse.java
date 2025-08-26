package com.example.fileuploadservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDownloadResponse {
    private String fileName;
    private byte[] content;
}
