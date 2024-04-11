package com.b301.knpl.repository;

import com.b301.knpl.entity.Custom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DIFFRepositoryImpl implements DIFFRepository{
    private final MongoTemplate mongoTemplate;

    private static final String TOKEN = "token";

    @Override
    public boolean findByToken(String token) {
        Query query = new Query(Criteria.where(TOKEN).is(token));
        return mongoTemplate.exists(query, Custom.class);
    }

    @Override
    public void saveCustom(Custom custom) {
        mongoTemplate.save(custom);
    }

    @Override
    public void updateCustom(String token, String result) {
        // 쿼리 객체 생성: token 필드가 주어진 token 값과 일치하는 문서 찾기
        Query query = new Query(Criteria.where(TOKEN).is(token));

        // 갱신 정의: result 필드를 입력값으로 갱신
        Update update = new Update();
        update.set("result", result);

        // 갱신 실행: 주어진 쿼리에 맞는 첫 번째 문서의 "result" 필드를 갱신
        mongoTemplate.updateFirst(query, update, Custom.class);
    }

    @Override
    public Custom getCustomByTaskId(String token) {
        Query query = new Query(Criteria.where(TOKEN).is(token));
        return mongoTemplate.findOne(query, Custom.class);
    }
}
