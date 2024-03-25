package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SVCFileDto {

    private final String type;
    private final Boolean select;
    private final String voice;

    @Builder
    public SVCFileDto(String type, Boolean select, String voice) {
        this.type = type;
        this.select = select;
        this.voice = voice;
    }
}
