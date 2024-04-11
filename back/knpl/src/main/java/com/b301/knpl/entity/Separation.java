package com.b301.knpl.entity;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Separation {

        private String vocals;
        private String drums;
        private String bass;
        private String other;

        // 생성자, 게터, 세터 등 필요한 메서드들을 추가할 수 있습니다.

        @Builder
        public Separation(String vocals, String drums, String bass, String other) {
                this.vocals = vocals;
                this.drums = drums;
                this.bass = bass;
                this.other = other;
        }
}
