package com.b301.knpl.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SVC {
    private String vocals;
    private String drums;
    private String bass;
    private String other;

    // 생성자, 게터, 세터 등 필요한 메서드들을 추가할 수 있습니다.
    @Builder
    public SVC(String vocals, String drums, String bass, String other) {
        this.vocals = vocals;
        this.drums = drums;
        this.bass = bass;
        this.other = other;
    }
}
