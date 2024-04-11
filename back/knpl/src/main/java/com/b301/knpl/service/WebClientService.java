package com.b301.knpl.service;

import com.b301.knpl.dto.FileStateDto;
import com.b301.knpl.dto.SVCFileDto;
import com.b301.knpl.dto.SVCInfoDto;
import com.b301.knpl.entity.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WebClientService {

    @Value("${app.ai.api2url}")
    private String aiBaseUrl;

    public void createSeparationRequest(File file, String outputExtension, String inputExtension, String taskId) {
        WebClient webClient = WebClient.builder().baseUrl(aiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        //파일 찾기
        String internalFilePath = file.getFilePath() + file.getChangeFilename();
        log.info(String.format("internalFilePath: %s", internalFilePath));
        try {
            Path path = Paths.get(internalFilePath);
            byte[] fileBytes = Files.readAllBytes(path);
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return file.getChangeFilename(); // 파일 이름 반환
                }
            };

            String separationcode = "/api2/separation/" + taskId;
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            body.add("file", resource);
            body.add("outputExtension", outputExtension);
            body.add("inputExtension", inputExtension);
            log.info(body.toString());
            Mono<String> responseMono = webClient.post().uri(separationcode).body(BodyInserters.fromMultipartData(body)).retrieve().bodyToMono(String.class);

            //응답 받기
            // 비동기적으로 받은 응답(response)을 처리
            responseMono.subscribe(response -> log.info(response));

        } catch (IOException e) {
            log.error("IOException 발생", e);
        }
    }

    public void createSVCWithSepRequest(List<SVCFileDto> svcFile, String taskId) {
        log.info("===== [WebClientService] createSVCWithSepRequest start =====");

        Map<String, Object> reqBody = new HashMap<>();
        for (SVCFileDto svc : svcFile) {
            String type = svc.getType();
            Boolean select = svc.getSelect();
            String voice = type + "_model";
            String voiceModel = svc.getVoice();

            reqBody.put(type, select);
            reqBody.put(voice, voiceModel);
        }

        log.info("{}", reqBody.toString());

        WebClient webClient = WebClient.builder().baseUrl(aiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Mono<String> responseMono = webClient.post()
                .uri("/api2/svc/separation/" + taskId)
                .bodyValue(reqBody)
                .retrieve()
                .bodyToMono(String.class);

        //응답 받기
        responseMono.subscribe(response ->
                // 비동기적으로 받은 응답(response)을 처리
                log.info(response)
        );

    }

    public void createSVCRequest(File file, SVCInfoDto svcInfo, String taskId) {
        log.info(String.format("send aiBaseUrl: %s", aiBaseUrl));
        WebClient webClient = WebClient.builder().baseUrl(aiBaseUrl).build();

        // 파일 읽기
        String internalFilePath = file.getFilePath() + file.getChangeFilename();
        log.info(String.format("internalFilePath: %s", internalFilePath));

        try {
            Path filePath = Paths.get(internalFilePath); // 실제 파일 경로로 변경해야 합니다.
            byte[] fileBytes = Files.readAllBytes(filePath);
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return file.getChangeFilename(); // 파일 이름 반환
                }
            };

            // ByteArrayResource를 사용하여 파일 파트 생성
            MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            bodyMap.add("file", resource);
            bodyMap.add("voice", svcInfo.getVoice());
            bodyMap.add("inputExtension", file.getExtension());
            bodyMap.add("outputExtension", svcInfo.getOutputExtension());

            Mono<String> responseMono = webClient.post()
                    .uri("/api2/svc/" + taskId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyMap))
                    .retrieve()
                    .bodyToMono(String.class);

            // 응답 받기
            responseMono.subscribe(response ->
                    // 비동기적으로 받은 응답(response)을 처리
                    log.info(response)
            );

        } catch (IOException e) {
            log.error("IOException 발생", e);
        }
    }

    public void createMixingRequest(FileStateDto fileStateDto, String taskId) {
        log.info("======Start createMixingRequest method in WebClientService======");
        WebClient webClient = WebClient.builder().baseUrl(aiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String separationcode = "/api2/separation/" + taskId + "/mixing";


        Mono<String> responseMono = webClient.post().uri(separationcode).bodyValue(fileStateDto).retrieve().bodyToMono(String.class);
        //응답 받기
        responseMono.subscribe(response ->
                // 비동기적으로 받은 응답(response)을 처리
                log.info(response)
        );

    }

    public void createSVCMixingRequest(FileStateDto fileStateDto, String taskId) {
        log.info("====== Start createMixingRequest method in WebClientService ======");
        WebClient webClient = WebClient.builder().baseUrl(aiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String aiUri = "/api2/svc/separation/" + taskId + "/mixing";

        Mono<String> responseMono = webClient.post().uri(aiUri).bodyValue(fileStateDto).retrieve().bodyToMono(String.class);
        //응답 받기
        responseMono.subscribe(response ->
                // 비동기적으로 받은 응답(response)을 처리
                log.info(response)
        );

    }

}
