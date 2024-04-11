package com.b301.knpl.service;

import com.b301.knpl.entity.File;
import com.b301.knpl.entity.Task;
import com.b301.knpl.repository.FileRepository;
import com.b301.knpl.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    // 파일 저장될 경로
    @Value("${app.upload.dir}")
    private String filePath;

    private final TaskRepository taskRepository;
    private final FileRepository fileRepository;


    @Transactional
    public File fileUpload(MultipartFile multipartFile)  {

        if(multipartFile.isEmpty()) return null;

        String original = multipartFile.getOriginalFilename().replaceAll(" ","_");
        original = original.substring(0, original.lastIndexOf("."));


        String extension = StringUtils.getFilenameExtension(multipartFile.getOriginalFilename());
        UUID uuid = UUID.randomUUID();
        String changeFile = uuid +"."+extension;
        String path = filePath;


        // 파일에 대한 정보를 db 저장
        File file = File.builder().original(original).changeFilename(changeFile).filePath(path).extension(extension).build();
        fileSave(file);

        String fullPath = filePath+changeFile;

        // 실제 파일을 해당 경로에 저장
        try {
            multipartFile.transferTo(new java.io.File(fullPath));
        } catch (IOException e){
            log.error("multipartFile.transferTo 오류", e);
        }
        return file;
    }

    @Transactional
    public File fileSave(File file){
        return fileRepository.save(file);
    }

    public File getFile(String id){
        return fileRepository.findById(id).orElseThrow();
    }

    public File getFileByFileName(String changeFileName) {
        Query q = new Query(where("change_filename").is(changeFileName));
        return fileRepository.findByChangeFileName(q, File.class, "file");
    }

    @Transactional
    public File fileUpdate(File file, String extension){
        Query q = new Query(where("_id").is(file.getId()));
        Update u = new Update();
        UUID uuid = UUID.randomUUID();
        u.set("changeFilename", uuid.toString()+"."+extension);
        u.set("filePath", filePath);
        return fileRepository.updateFile(q,u,File.class,"file");
    }

    public List<MultipartFile> createFileList(MultipartFile vocals, MultipartFile drums, MultipartFile bass, MultipartFile other ){
        ArrayList<MultipartFile> result = new ArrayList<>();
        result.add(vocals);
        result.add(drums);
        result.add(bass);
        result.add(other);

        return result;
    }

    public List<String> createFileList(String a, String b, String c, String d){
        ArrayList<String> result = new ArrayList<>();
        result.add(a);
        result.add(b);
        result.add(c);
        result.add(d);

        return result;
    }

    public Map<String, String> getFilePathSVC(Task task) {
        Map<String, String> response = new HashMap<>();
        String svcFile = task.getResult().getSvcMix().getFile();
        String fileName = getFile(svcFile).getOriginal() + "_svc." + getFile(svcFile).getExtension();
        String endPoint = "/api/file/download/" + getFile(svcFile).getChangeFilename();

        response.put("fileName", fileName);
        response.put("endPoint", endPoint);
        return response;
    }

    public void printFileInfo(File file, String fileInfo) {
        log.info("---------- {} ----------", fileInfo);
        log.info(String.format("objectId: %s", file.getId()));
        log.info(String.format("changeFileName id: %s", file.getChangeFilename()));
        log.info(String.format("extension: %s", file.getExtension()));
        log.info(String.format("original: %s", file.getOriginal()));
        log.info(String.format("filePath: %s", file.getFilePath()));
        log.info("----------------------------------");
    }


}
