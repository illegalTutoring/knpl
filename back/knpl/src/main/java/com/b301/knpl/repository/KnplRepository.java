package com.b301.knpl.repository;

import com.b301.knpl.entity.File;
import com.b301.knpl.entity.Task;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Query;


public interface KnplRepository {

    File updateFile(Query q, Update u, Class<File> f, String collectionName);

    Task updateTask(Query q, Update u, Class<Task> t, String collectionName);

    File findByChangeFileName(Query q, Class<File> f, String collectionName);

    Task pushResult(Query q, Update u, Class<Task> t , String collectionName);

}
