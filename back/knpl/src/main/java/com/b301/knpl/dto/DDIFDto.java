package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@ToString
@Builder
public class DDIFDto {
    private final MultipartFile vocalFile;
    private final MultipartFile musicFile;
}
