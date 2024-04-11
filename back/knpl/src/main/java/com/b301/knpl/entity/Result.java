package com.b301.knpl.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Result {

    private Separation separation;
    private SVC svc;
    private Mix mix;
    private SVCMix svcMix;

    // 생성자, 게터, 세터 등 필요한 메서드들을 추가할 수 있습니다.
    @Builder
    public Result(Separation separation, SVC svc, Mix mix, SVCMix svcMix) {
        this.separation = separation;
        this.svc = svc;
        this.mix = mix;
        this.svcMix = svcMix;
    }
}