package com.b301.knpl.service;

import com.b301.knpl.dto.FileResultDto;
import com.b301.knpl.dto.SeparationDto;
import com.b301.knpl.entity.File;
import com.b301.knpl.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeparationService {

    private final FileService fileService;
    private final TaskService taskService;
    private static final String END_POINT = "/api/file/download/";

    /**
     *
     * @param taskId 작업 id
     * @return List SeparationDto
     *
     */
    public List<SeparationDto> getSeparationDto(String taskId){
        List<SeparationDto> list = new ArrayList<>();

        Task task = taskService.getTask(taskId);

        //task 자체가 없을 때,
        if(task == null){return list;}

        //구해야 할 것: 타입, 파일명(원본+타입+확장자), 파일 다운경로 (api2/file/download/change_filename)

        String originalName = fileService.getFile(task.getOriginalFile()).getOriginal();
        String outputExtension = task.getOutputExtension();

        //separation result 존재 여부 확인
        if(task.getResult().getSeparation() == null){
            return list;
        }

        File vocals = fileService.getFile(task.getResult().getSeparation().getVocals());
        File drums = fileService.getFile(task.getResult().getSeparation().getDrums());
        File bass = fileService.getFile(task.getResult().getSeparation().getBass());
        File other = fileService.getFile(task.getResult().getSeparation().getOther());

        list.add(new SeparationDto("vocals", originalName+"_vocals."+outputExtension,END_POINT+vocals.getChangeFilename()));
        list.add(new SeparationDto("drums", originalName+"_drums."+outputExtension,END_POINT+drums.getChangeFilename()));
        list.add(new SeparationDto("bass", originalName+"_bass."+outputExtension,END_POINT+bass.getChangeFilename()));
        list.add(new SeparationDto("other", originalName+"_other."+outputExtension,END_POINT+other.getChangeFilename()));

        return list;
    }

    public FileResultDto getMixDto(String taskId){

        Task task = taskService.getTask(taskId);

        //task 자체가 없을 때
        if(task == null){return null;}

        //구해야 할 것: 타입, 파일명(원본+타입+확장자), 파일 다운경로 (api2/file/download/change_filename)

        String originalName = fileService.getFile(task.getOriginalFile()).getOriginal();
        String outputExtension = task.getOutputExtension();

        //mix result 존재 여부 확인
        if(task.getResult().getMix() == null){
            return null;
        }

        File mixing = fileService.getFile(task.getResult().getMix().getFile());

        return FileResultDto.builder().fileName(originalName+"_mix."+outputExtension).endPoint(END_POINT+mixing.getChangeFilename()).build();
    }



}
