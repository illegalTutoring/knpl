package com.b301.knpl.service;

import com.b301.knpl.entity.Custom;
import com.b301.knpl.repository.DIFFRepositoryImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomService {

    private final DIFFRepositoryImpl diffRepository;

    // 파일 저장될 경로
    @Value("${app.ai.result.dir}")
    private String filePath;

    @Transactional
    public void fileUpload(MultipartFile multipartFile, String token)  {
        // 변경될 파일 이름
        String changeFile = multipartFile.getOriginalFilename();
        // token 이름으로 폴더 생성
        String directoryPath = filePath + token;
        // 폴더 경로 확인 및 생성
        java.io.File directory = new java.io.File(directoryPath);

        if (!directory.exists()) {
            directory.mkdirs(); // 폴더가 없다면 생성
        }

        // 파일에 대한 정보를 DB에 저장
        diffRepository.updateCustom(token,directoryPath + "/" + changeFile);

        // 실제 파일 저장 경로
        String fullPath = directoryPath + "/" + changeFile;

        // 실제 파일을 해당 경로에 저장
        try {
            multipartFile.transferTo(new java.io.File(fullPath));
        } catch (IOException e) {
            log.error("multipartFile.transferTo 오류", e);
        }
    }

    public void saveCustom(Custom file){
        diffRepository.saveCustom(file);
    }

    public boolean findByToken(String token){
        return diffRepository.findByToken(token);
    }

    public void updateCustom(String token, String result){
        diffRepository.updateCustom(token, result);
    }

    public Custom getCustomByTaskId(String token){
        return diffRepository.getCustomByTaskId(token);
    }

    public File convertMultipartFileToFile(MultipartFile file) {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        try {
            file.transferTo(convFile);
        } catch (IOException e) {
            log.error("File conversion error", e);
        }
        return convFile;
    }

    public void parseJsonAndSaveFile(ObjectMapper mapper, String json){
        try {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode messageIdNode = rootNode.path("message_queue_id");
            if (!messageIdNode.isMissingNode()) { // 항상 이 체크를 수행하는 것이 좋음
                log.info("message_queue_id: " + messageIdNode.asText());
                Custom file = new Custom(messageIdNode.asText(), null);
                saveCustom(file);
            } else {
                log.info("message_queue_id not found");
            }
        } catch (Exception e) {
            log.error("Parsing error", e);
        }
    }

}
