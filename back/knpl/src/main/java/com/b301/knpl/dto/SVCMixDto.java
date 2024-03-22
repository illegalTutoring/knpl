package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SVCMixDto {

    private final String fileName;
    private final String endPoint;

    @Builder
    public SVCMixDto(String fileName, String endPoint) {
        this.fileName = fileName;
        this.endPoint = endPoint;
    }

}
