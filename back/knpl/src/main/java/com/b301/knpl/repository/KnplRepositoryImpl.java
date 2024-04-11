package com.b301.knpl.repository;

import com.b301.knpl.entity.Custom;
import com.b301.knpl.entity.File;
import com.b301.knpl.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class KnplRepositoryImpl implements KnplRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public File updateFile(Query q, Update u, Class<File> f, String collectionName) {
        return mongoTemplate.findAndModify(q, u ,f, collectionName);
    }

    @Override
    public Task updateTask(Query q, Update u, Class<Task> t, String collectionName) {
        mongoTemplate.findAndModify(q, u ,t, collectionName);
        return mongoTemplate.findOne(q,t,collectionName);
    }

    @Override
    public File findByChangeFileName(Query q, Class<File> f, String collectionName) {
        return mongoTemplate.findOne(q, f, collectionName);
    }

    public Task pushResult(Query q, Update u, Class<Task> t, String collectionName) {
        mongoTemplate.updateFirst(q, u, t, collectionName);
        return mongoTemplate.findOne(q,t,collectionName);
    }

    public void saveCustom(Custom custom) {
        mongoTemplate.save(custom);
    }
}
