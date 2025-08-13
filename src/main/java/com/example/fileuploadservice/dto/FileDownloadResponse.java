package com.example.fileuploadservice.dto;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
public class FileDownloadResponse {
    private String fileName;
    private String fileSizeFormatted;
    private String filePath;
    private String message;
}
