package com.b301.knpl.controller;

import com.b301.knpl.service.SSEService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/sse")
public class SSEController {

    private final SSEService sseService;

    @Operation(summary = "sse 생성 및 연결", description = "taskId에 따른 sse를 생성하고 클라이언트와 연결합니다.")
    @ApiResponse(responseCode = "200", description = "파일 전달 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터 값")
    @ApiResponse(responseCode = "404", description = "파일이 존재하지 않습니다.")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    @GetMapping(path="/connect/{taskId}" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(@PathVariable("taskId") String taskId){

        log.info("============start sse connect==========");
        SseEmitter emitter = sseService.connect(taskId);

        log.info("emitter: {}",emitter.toString());

        return ResponseEntity.status(HttpStatus.OK).body(emitter);
    }
}
