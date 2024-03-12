package com.b301.knpl.service;

import com.b301.knpl.entity.Task;
import com.b301.knpl.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final FileRepository fileRepository;
    private final TaskService taskService;

    public Task insertTask(String orignal, String outputEx){


        return null;
    }
}
