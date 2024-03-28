package com.b301.knpl.repository;

import com.b301.knpl.entity.Custom;
import org.springframework.stereotype.Repository;

@Repository
public interface DIFFRepository {

    boolean findByToken(String token);

    void saveCustom(Custom custom);

    void updateCustom(String token, String result);

    Custom getCustomByTaskId(String token);
}
