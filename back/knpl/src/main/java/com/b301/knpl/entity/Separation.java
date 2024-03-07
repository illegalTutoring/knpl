package com.b301.knpl.entity;


import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document
public class Separation {

    private String vocal;
    private String drum;
    private String  bass;
    private String session;

}
