package com.b301.knpl.repository;

import com.b301.knpl.entity.Custom;
import com.b301.knpl.entity.File;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DIFFRepository {

    boolean findByToken(String token);

    void saveCustom(Custom custom);

    void updateCustom(String token, String result);
}
