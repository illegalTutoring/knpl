package com.b301.knpl.service;

import com.b301.knpl.entity.*;
import com.b301.knpl.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.data.mongodb.core.query.Criteria.where;

//@Service
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final FileService fileService;


    public Task insertTask(String original, String outputExtension){
        Task task = Task.builder().originalFile(original).outputExtension(outputExtension).build();
        return taskRepository.save(task);

    }

    public Task getTask(String taskId){
        return taskRepository.findById(taskId).orElse(null);

    }

    public void updateTask(Task task, Result result){
        Query q = new Query(where("_id").is(task.getId()));
        Update u = new Update();
        u.set("result", result);
        taskRepository.updateTask(q, u, Task.class, "task");
    }

    public Task pushSep(Task task, Separation separation){
        Query q = new Query(where("_id").is(task.getId()));
        Update u = new Update();
        u.set("result.separation", separation);
        return taskRepository.pushResult(q,u, Task.class,"task");
    }

    public void pushMix(Task task, Mix mix){
        Query q = new Query(where("_id").is(task.getId()));
        Update u = new Update();
        u.set("result.mix", mix);
        taskRepository.pushResult(q, u, Task.class, "task");
    }

    public Task pushSVC(Task task, SVC svc){
        Query q = new Query(where("_id").is(task.getId()));
        Update u = new Update();
        u.set("result.svc", svc);
        return taskRepository.pushResult(q,u, Task.class,"task");
    }

    public void pushSVCMix(Task task, SVCMix svcMix){
        Query q = new Query(where("_id").is(task.getId()));
        Update u = new Update();
        u.set("result.svcMix", svcMix);
        taskRepository.pushResult(q, u, Task.class, "task");
    }

    @Transactional
    public int updateMix(MultipartFile file, String taskId){
        File f = fileService.fileUpload(file);

        // mix 엔터티 생성
        Mix mix = Mix.builder().file(f.getId()).build();
        //Task result 업데이트
        Task task = getTask(taskId);
        if(task == null) return -1;

        pushMix(task, mix);

        return 1;
    }

    @Transactional
    public int updateSVCMix(MultipartFile file, String taskId){
        File f = fileService.fileUpload(file);

        // mix 엔터티 생성
        SVCMix svcMix = SVCMix.builder().file(f.getId()).build();
        //Task result 업데이트
        Task task = getTask(taskId);
        if(task == null) return -1;

        pushSVCMix(task, svcMix);

        return 1;
    }


    public String insertFileAndGetTaskId(MultipartFile file, String outputExtension){
        File resultFile = fileService.fileUpload(file);

        log.info(String.format("파일 경로: %s", resultFile.getFilePath()));
        log.info(String.format("변경 파일명: %s", resultFile.getChangeFilename()));
        log.info(String.format("파일 확장자: %s", resultFile.getExtension()));
        log.info(String.format("파일 엔터티 id: %s", resultFile.getId()));
        log.info(String.format("원본 파일명: %s", resultFile.getOriginal()));

        Task task = insertTask(resultFile.getId(), outputExtension);

        String taskId = task.getId();

        log.info("taskId: {}", taskId);

        return task.getId();
    }

    public void completeSeparation(MultipartFile vocals, MultipartFile drums, MultipartFile bass, MultipartFile other, String taskId){
        // 분리 파일 DB 저장
        File v = fileService.fileUpload(vocals);
        File d = fileService.fileUpload(drums);
        File b = fileService.fileUpload(bass);
        File o = fileService.fileUpload(other);

        // Separation 엔터티 생성
        Separation sep = Separation.builder().vocals(v.getId()).drums(d.getId()).bass(b.getId()).other(o.getId()).build();
        // Result 생성
        Result result = Result.builder().separation(sep).build();
        //Task result 업데이트
        Task task = getTask(taskId);

        updateTask(task, result);

    }

}
