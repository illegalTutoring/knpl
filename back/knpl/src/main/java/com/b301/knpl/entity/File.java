package com.b301.knpl.entity;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;


@Getter
@Document(collection = "file")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File {

    @Id
    @Field(value="id",  targetType = FieldType.OBJECT_ID)
    private String id;


    private String original;

    @Field(value="change_filename")
    private String changeFilename;
    @Field(value="file_path")
    private String filePath;
    private String extension;

    @Builder
    public File(String original, String changeFilename, String filePath, String extension) {
        this.original = original;
        this.changeFilename = changeFilename;
        this.filePath = filePath;
        this.extension = extension;
    }

    @Builder
    public File(String original, String extension) {
        this.original = original;
        this.extension = extension;
    }

}
