package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SVCInfoDto {

    private String voice;
    private String outputExtension;

    @Builder
    public SVCInfoDto(String voice, String outputExtension) {
        this.voice = voice;
        this.outputExtension = outputExtension;
    }
}
