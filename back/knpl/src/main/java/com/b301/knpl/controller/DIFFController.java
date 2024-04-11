package com.b301.knpl.controller;


import com.b301.knpl.dto.MessageDto;
import com.b301.knpl.entity.Custom;
import com.b301.knpl.entity.Message;
import com.b301.knpl.service.CustomService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diff")
@Tag(name = "diff", description = "내 음성 변환")
public class DIFFController {

    private final RestTemplate restTemplate;


    @Value("${app.ai.result.dir}")
    private String baseUrl;

    private final CustomService customService;

    // Python API URL
    @Value("${app.ai.api3url}")
    private String pythonUrl;


    @Operation(summary = "내 음성으로 노래 만들기", description = "내 음성, 음악파일을 전달받아 학습 시킨 이후 노래 파일을 반환합니다")
    @ApiResponse(responseCode = "200", description = "파일 전달 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터 값")
    @ApiResponse(responseCode = "404", description = "파일이 없습니다")
    @ApiResponse(responseCode = "422", description = "지원하지 않는 파일")
    @ApiResponse(responseCode = "429", description = "하루에 한번만 요청 가능")
    @ApiResponse(responseCode = "500", description = "AI 서버와 연결 불가")
    @PostMapping("")
    public ResponseEntity<Object> requestAiServer(@RequestPart MultipartFile vocalFile,
                                                  @RequestPart MultipartFile musicFile) {

        if (vocalFile == null || vocalFile.isEmpty() || musicFile == null || musicFile.isEmpty()) {
            MessageDto message = MessageDto.builder().message(Message.FILE_NOT_FOUND.getStatusMessage()).build();
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(message);
        }


        if (!Objects.equals(StringUtils.getFilenameExtension(musicFile.getOriginalFilename()), "mp3") && !Objects.equals(StringUtils.getFilenameExtension(musicFile.getOriginalFilename()), "wav")) {
            MessageDto message = MessageDto.builder().message("지원하지 않는 파일 형식입니다.").build();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(message);
        }

        if (!Objects.equals(StringUtils.getFilenameExtension(vocalFile.getOriginalFilename()), "mp3") && !Objects.equals(StringUtils.getFilenameExtension(vocalFile.getOriginalFilename()), "wav")) {
            MessageDto message = MessageDto.builder().message("지원하지 않는 파일 형식입니다.").build();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(message);
        }


        log.info("========AI 서버로 보낼 요청===========");
        log.info("vocal: {}", vocalFile.getOriginalFilename());
        log.info("music: {}", musicFile.getOriginalFilename());

        // 파일과 데이터를 포함한 요청을 구성
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("vocalFile", new FileSystemResource(customService.convertMultipartFileToFile(vocalFile)));
        body.add("musicFile", new FileSystemResource(customService.convertMultipartFileToFile(musicFile)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);


        String url = pythonUrl + "api3/svc";
        // Python API에 요청 보내기
        ResponseEntity<String> response;
        try {
            // Python API에 요청 보내기 및 응답 받기
            response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            String json = response.getBody();

            ObjectMapper mapper = new ObjectMapper();

            customService.parseJsonAndSaveFile(mapper,json);

            return ResponseEntity.status(HttpStatus.OK).body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.info("하루 한번 요청 가능");
            MessageDto message = MessageDto.builder().message(Message.ONE_REQUEST_POSSIBLE_PER_DAY.getStatusMessage()).build();
            return ResponseEntity.status(Message.ONE_REQUEST_POSSIBLE_PER_DAY.getHttpCode()).body(message);
        } catch (Exception ex) {
            log.info("AI 서버와 연결 불가능");
            MessageDto message = MessageDto.builder().message(Message.SERVER_ERROR.getStatusMessage()).build();
            return ResponseEntity.status(Message.SERVER_ERROR.getHttpCode()).body(message);
        }
    }

    @Operation(summary = "AI서버로 부터 변환 완료된 파일 받기", description = "AI서버에서 결과가 나오면 api서버로 해당 파일을 보냅니다.")
    @ApiResponse(responseCode = "200", description = "파일과 taskId가 성공적으로 저장")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터 값")
    @ApiResponse(responseCode = "422", description = "지원하지 않는 파일")
    @ApiResponse(responseCode = "500", description = "서버와 연결 불가")
    @PostMapping("/completion")
    public ResponseEntity<Object> requestAiServer(@RequestPart MultipartFile resultFile,
                                                  @RequestPart String taskId) {

        log.info("========입력 받은 결과값, 태스크 아이디===========");


        // 400 ERROR 처리
        if (resultFile == null || resultFile.isEmpty()) {
            log.info("파일이 없습니다");
            MessageDto message = MessageDto.builder().message(Message.BAD_REQUEST.getStatusMessage()).build();
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(message);
        }
        if (taskId == null) {
            log.info("토큰이 입력되지 않았습니다");
            MessageDto message = MessageDto.builder().message(Message.TOKEN_NOT_FOUND.getStatusMessage()).build();
            return ResponseEntity.status(Message.TOKEN_NOT_FOUND.getHttpCode()).body(message);
        }
        if (!customService.findByToken(taskId)) {
            log.info("유효하지 않는 토큰값입니다.");
            MessageDto message = MessageDto.builder().message(Message.BAD_REQUEST.getStatusMessage()).build();
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(message);
        }

        log.info("result: {}", resultFile.getOriginalFilename());
        log.info("music: {}" , taskId);

        // 422 ERROR 처리
        if (!Objects.equals(StringUtils.getFilenameExtension(resultFile.getOriginalFilename()), "wav")) {
            MessageDto message = MessageDto.builder().message("지원하지 않는 파일 형식입니다.").build();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(message);
        }

        try {
            customService.fileUpload(resultFile, taskId);
            log.info("성공적으로 저장했습니다.");
            MessageDto message = MessageDto.builder().message(Message.RESPONSE_COMPLETED.getStatusMessage()).build();
            return ResponseEntity.status(Message.RESPONSE_COMPLETED.getHttpCode()).body(message);

        } catch (Exception e) { // 500 ERROR 처리
            log.error("서버 에러", e);
            MessageDto message = MessageDto.builder().message(Message.SERVER_ERROR.getStatusMessage()).build();
            return ResponseEntity.status(Message.SERVER_ERROR.getHttpCode()).body(message);
        }

    }

    @Operation(summary = "클라이언트에 결과 출력", description = "클라이언트에게 결과 파일을 보냅니다.")
    @ApiResponse(responseCode = "200", description = "정상적으로 파일 다운로드")
    @ApiResponse(responseCode = "204", description = "작업중..")
    @ApiResponse(responseCode = "400", description = "비정상적인 토큰 입력")
    @ApiResponse(responseCode = "404", description = "모든 작업이 완료되었으나, 파일이 비정상적으로 삭제됨")
    @ApiResponse(responseCode = "500", description = "서버와 연결 불가")
    @GetMapping(path = "status/{taskId}", produces = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> downloadFile(@PathVariable("taskId") String taskId) throws IOException {
        log.info("===== downloadFile start =====");


        // 400 ERROR
        if (taskId == null) {
            log.error("CustomService : downloadFile : bad request : taskId is null");
            return ResponseEntity.badRequest().body(null);
        }

        Custom findFile = customService.getCustomByTaskId(taskId);

        if (findFile == null) {
            log.error("CustomService : downloadFile : bad request : can not find file");
            return ResponseEntity.badRequest().body(null);
        }


        String filePath = findFile.getResult();
        log.info("filePath : {} ", filePath);

        File file;
        if (filePath == null) {
            log.error("CustomService : downloadFile : no content");
            return ResponseEntity.noContent().build();
        }else{
            file = new File(filePath);
        }

        // 404 ERROR
        if (!file.exists()) {
            log.error("CustomService : downloadFile : not found");
            return ResponseEntity.notFound().build();
        }

        ByteArrayResource resource = new ByteArrayResource(FileCopyUtils.copyToByteArray(new FileInputStream(file)));

        return ResponseEntity.ok()
                .contentLength(file.length())
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
