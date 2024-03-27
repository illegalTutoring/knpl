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
    private String endPoint;
    private String message;

    @Builder
    public FileResultDto(String fileName, String endPoint, String message) {
        this.fileName = fileName;
        this.endPoint = endPoint;
        this.message = message;
    }
}
