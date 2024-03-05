package com.b301.knpl.dto;

import com.b301.knpl.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class TeamDto {

    private String id;
    private String name;
    private Member member;


    public TeamDto(String name, Member member) {
        this.name = name;
        this.member = member;
    }
}
