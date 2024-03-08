package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SVCInfoDto {

    private String voice;
    private String svcCode;

    @Builder
    public SVCInfoDto(String voice, String svcCode) {
        this.voice = voice;
        this.svcCode = svcCode;
    }
}
