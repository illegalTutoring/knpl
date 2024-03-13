package com.b301.knpl.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SVCFileListDto {

    public List<SVCFileDto> svcFileInfo;
    public String svcCode;

    @Builder
    public SVCFileListDto(List<SVCFileDto> svcFileDtoList, String svcCode) {
        this.svcFileInfo = svcFileDtoList;
        this.svcCode = svcCode;
    }


}
