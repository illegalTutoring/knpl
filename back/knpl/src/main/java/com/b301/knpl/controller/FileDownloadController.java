package com.b301.knpl.controller;

import com.b301.knpl.entity.File;
import com.b301.knpl.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Tag(name = "file download", description = "FILE DOWNLOAD API")
@RequestMapping("/api/file/download")
@Slf4j
public class FileDownloadController {

    private final FileService fileService;

    @Value("${app.upload.dir}")
    private String baseUrl;

    @Operation(summary = "음성 변환(분리X)", description = "음성 변환 요청 받음")
    @ApiResponse(responseCode = "200", description = "변환할 음성파일 업로드 완료. 변환 요쳥 시작")
    @GetMapping(path="/{fileName}", produces=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Resource downloadFile(@PathVariable("fileName") String fileName) throws IOException {
        log.info("===== [FileDownloadController] downloadFile start =====");

        File findFile = fileService.getFileByFileName(fileName);

        if(findFile == null) {
            log.error("cannot find file");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        // 파일 정보 로그 출력
        fileService.printFileInfo(findFile, "download file 정보");

        log.info("download path: {}", findFile.getFilePath() + fileName);

        try {
            return new ByteArrayResource((FileCopyUtils.copyToByteArray(new FileInputStream(
                    findFile.getFilePath() + fileName
            ))));
        } catch (Exception e) {
            log.error("exception 발생", e);
            return new ByteArrayResource((FileCopyUtils.copyToByteArray(new FileInputStream(
                    ""
            ))));
        }

    }
}
