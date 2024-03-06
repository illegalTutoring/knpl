package com.b301.knpl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
//@EnableMongoRepositories
public class KnplApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnplApplication.class, args);
    }

}
