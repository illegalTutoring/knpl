package com.b301.knpl.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class SeparationDto {

    private final String type;
    private final String fileName;

    public SeparationDto(String type, String fileName) {
        this.type = type;
        this.fileName = fileName;
    }
}
