package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MessageDto {
    String message;

    @Builder
    public MessageDto(String message) {
        this.message = message;
    }
}
