package com.b301.knpl.service;

import com.b301.knpl.repository.FileRepository;
import com.b301.knpl.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SeparationService {

    TaskRepository taskRepository;
    FileRepository fileRepository;



}
