package com.b301.knpl.service;

import com.b301.knpl.dto.SVCMixDto;
import com.b301.knpl.dto.SVCResponseDto;
import com.b301.knpl.dto.SVCWithSepDto;
import com.b301.knpl.entity.File;
import com.b301.knpl.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SVCService {

    private final FileService fileService;
    private final TaskService taskService;
    private static final String END_POINT = "/api/file/download/";

    public List<SVCWithSepDto> getSVCWithSepDto(String taskId){
        List<SVCWithSepDto> list = new ArrayList<>();

        Task task = taskService.getTask(taskId);

        //task 자체가 없을 때,
        if(task == null) { return list; }

        //구해야 할 것: 타입, 파일명(원본+타입+확장자), 파일 다운경로 (api2/file/download/change_filename)

        String originalName = fileService.getFile(task.getOriginalFile()).getOriginal();
        String outputExtension = task.getOutputExtension();

        //separation result 존재 여부 확인
        if(task.getResult().getSvc() == null) {
            return list;
        }

        File vocals = fileService.getFile(task.getResult().getSvc().getVocals());
        File drums = fileService.getFile(task.getResult().getSvc().getDrums());
        File bass = fileService.getFile(task.getResult().getSvc().getBass());
        File other = fileService.getFile(task.getResult().getSvc().getOther());

        list.add(new SVCWithSepDto("vocals", originalName+"_svc_vocals."+outputExtension,END_POINT+vocals.getChangeFilename()));
        list.add(new SVCWithSepDto("drums", originalName+"_svc_drums."+outputExtension,END_POINT+drums.getChangeFilename()));
        list.add(new SVCWithSepDto("bass", originalName+"_svc_bass."+outputExtension,END_POINT+bass.getChangeFilename()));
        list.add(new SVCWithSepDto("other", originalName+"_svc_other."+outputExtension,END_POINT+other.getChangeFilename()));

        return list;
    }

    public SVCMixDto getMixDto(String taskId){
        log.info("===== [SVCService] getMixDto start =====");
        Task task = taskService.getTask(taskId);

        //task 자체가 없을 때
        if(task == null) { return null; }

        //구해야 할 것: 타입, 파일명(원본+타입+확장자), 파일 다운경로 (api2/file/download/change_filename)
        String originalName = fileService.getFile(task.getOriginalFile()).getOriginal();
        String outputExtension = task.getOutputExtension();

        //mix result 존재 여부 확인
        if(task.getResult().getSvcMix() == null){
            return null;
        }

        File mixing = fileService.getFile(task.getResult().getSvcMix().getFile());

        return SVCMixDto.builder().fileName(originalName+"_mix_svc."+outputExtension)
                .endPoint(END_POINT+mixing.getChangeFilename()).build();
    }

    public SVCResponseDto getSVCResponseDto(String taskId){
        log.info("===== [SVCService] getSVCResponseDto start =====");
        Task task = taskService.getTask(taskId);

        //task 자체가 없을 때
        if(task == null) { return null; }

        //구해야 할 것: 타입, 파일명(원본+타입+확장자), 파일 다운경로 (api2/file/download/change_filename)
        String originalName = fileService.getFile(task.getOriginalFile()).getOriginal();
        String outputExtension = task.getOutputExtension();

        //mix result 존재 여부 확인
        if(task.getResult().getSvcMix() == null){
            return null;
        }

        File mixing = fileService.getFile(task.getResult().getSvcMix().getFile());

        return SVCResponseDto.builder().fileName(originalName+"_svc."+outputExtension)
                .endPoint(END_POINT+mixing.getChangeFilename()).build();
    }

    public void printfilesInfo(List<SVCWithSepDto> files) {
        log.info("===== [SVCService] printfilesInfo start =====");

        log.info("------------------ 보낼 정보 -------------------");
        log.info("vocals:       {}", files.get(0).getFileName());
        log.info("vocalsPath:   {}", files.get(0).getEndPoint());
        log.info("drums:        {}", files.get(1).getFileName());
        log.info("drumsPath:    {}", files.get(1).getEndPoint());
        log.info("bass:         {}", files.get(2).getFileName());
        log.info("bassPath:     {}", files.get(2).getEndPoint());
        log.info("other:        {}", files.get(3).getFileName());
        log.info("otherPath:    {}", files.get(3).getEndPoint());
    }
}
