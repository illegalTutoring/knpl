package com.b301.knpl.entity;

import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.DBRef;

public class SVC {
    private String file;

    // 생성자, 게터, 세터 등 필요한 메서드들을 추가할 수 있습니다.
    @Builder
    public SVC(String file) {
        this.file = file;
    }
}
