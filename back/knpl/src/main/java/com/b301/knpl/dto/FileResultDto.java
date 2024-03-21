package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class FileResultDto {

    private String fileName;
    private String filePath;
    private String message;

    @Builder
    public FileResultDto(String fileName, String filePath, String message) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.message = message;
    }
}
