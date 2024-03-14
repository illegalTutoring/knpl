package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SVCFileListDto {

    public List<SVCFileDto> svcFileInfo;

    @Builder
    public SVCFileListDto(List<SVCFileDto> svcFileDtoList) {
        this.svcFileInfo = svcFileDtoList;
    }


}
