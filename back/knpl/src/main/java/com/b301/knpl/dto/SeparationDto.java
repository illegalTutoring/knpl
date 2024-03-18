package com.b301.knpl.dto;


import lombok.*;

@Getter
@ToString
public class SeparationDto {

    private final String type;
    private final String fileName;
    private final String endPoint;

    @Builder
    public SeparationDto(String type, String fileName, String endPoint) {
        this.type = type;
        this.fileName = fileName;
        this.endPoint = endPoint;
    }
}
