package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class FilePathDto {

    private List<String> filePathList;

    @Builder
    public FilePathDto(List<String> filePathList) {
        this.filePathList = filePathList;
    }
}
