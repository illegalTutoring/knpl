package com.b301.knpl.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mix {
    private String file;

    @Builder
    public Mix(String file) {
        this.file = file;
    }
}
