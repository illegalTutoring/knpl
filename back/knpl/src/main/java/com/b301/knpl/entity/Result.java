package com.b301.knpl.entity;

import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.Document;

public class Result {

    private Separation separation;
    private SVC svc;

    // 생성자, 게터, 세터 등 필요한 메서드들을 추가할 수 있습니다.
    @Builder
    public Result(Separation separation, SVC svc) {
        this.separation = separation;
        this.svc = svc;
    }
}