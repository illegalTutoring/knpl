package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class FileStateDto {
    private boolean vocals;
    private boolean drums;
    private boolean bass;
    private boolean other;


    @Builder
    public FileStateDto(boolean vocals, boolean drums, boolean bass, boolean other) {
        this.vocals = vocals;
        this.drums = drums;
        this.bass = bass;
        this.other = other;
    }
}
