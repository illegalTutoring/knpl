package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SVCWithSepDto {

    private final String type;
    private final String fileName;
    private final String endPoint;

    @Builder
    public SVCWithSepDto(String type, String fileName, String endPoint) {
        this.type = type;
        this.fileName = fileName;
        this.endPoint = endPoint;
    }
}
