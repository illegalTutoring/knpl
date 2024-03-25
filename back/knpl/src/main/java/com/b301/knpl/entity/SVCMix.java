package com.b301.knpl.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SVCMix {
    private String file;

    @Builder
    public SVCMix(String file) {
        this.file = file;
    }
}
