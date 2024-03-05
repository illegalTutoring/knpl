package com.b301.knpl.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public class Member {

    private String name;
    private String age;
    private String state;


    public Member(String name, String age, String state) {
        this.name = name;
        this.age = age;
        this.state = state;
    }
}
