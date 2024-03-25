package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SVCFileListDto {

    private List<SVCFileDto> svcFile;
    private String taskId;

    @Builder
    public SVCFileListDto(List<SVCFileDto> svcFileDtoList, String taskId) {
        this.svcFile = svcFileDtoList;
        this.taskId = taskId;
    }


}
