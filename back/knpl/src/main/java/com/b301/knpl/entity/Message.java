package com.b301.knpl.entity;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum Message {

    REQUEST_RECEIVED(HttpStatus.ACCEPTED, "Request received"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "File not found"),
    RESPONSE_COMPLETED(HttpStatus.OK, "Successfully response completed"),
    FILE_RECEIVED_SUCCESSFULLY(HttpStatus.ACCEPTED, "File received successfully"),
    UNSUPPORTED_FILE_FORMAT(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Unsupported file format"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    NOT_ENOUGH_FILES(HttpStatus.BAD_REQUEST,"Not enough files"),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found"),
    TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "Token not found"),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Server error"),
    ONE_REQUEST_POSSIBLE_PER_DAY(HttpStatus.TOO_MANY_REQUESTS,"You can make one request per day");


    private final HttpStatus httpCode;
    private final String statusMessage;


    Message(HttpStatus httpCode, String message) {
        this.httpCode = httpCode;
        this.statusMessage = message;
    }
}
