package com.b301.knpl.entity;


import com.b301.knpl.dto.SeparationDto;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.List;

@Getter
@Document(collection = "task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @Field(value = "_id", targetType = FieldType.OBJECT_ID)
    private String id;
    @Field(value="original_file", targetType = FieldType.OBJECT_ID)
    private String originalFile;
    private Result result;
    private String outputExtension;

    @Builder
    public Task(String originalFile, Result result, String outputExtension) {
        this.originalFile = originalFile;
        this.result = result;
        this.outputExtension = outputExtension;
    }
}
