package com.b301.knpl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class SSEService {

    private static Map<String, SseEmitter> container = new ConcurrentHashMap<>(); // sseEmitter들을 모아둘 컨테이너

    public SseEmitter connect(final String taskId) {
        // 1. sseEmitter 생성, timeout 시간까지 연결 끊기지 않음
        SseEmitter sseEmitter = new SseEmitter(600000L); // 10분

        log.info("sse connect taskId: {}", taskId);


        // 2. 연결 정보 보냄 ( 503Service Unavailable 에러를 방지 하기 위한 더미데이터임 )
        final SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event()
                .name("connect")
                .data("connected!");
        sendEvent(sseEmitter, sseEventBuilder);

        container.put(taskId,sseEmitter);

        //(5). 완료시
        sseEmitter.onCompletion(() -> {
            if (container.remove(taskId) != null) {
                log.info("server sent event removed in emitter cache: id={}", taskId);
            }

            log.info("disconnected by completed server sent event: id={}", taskId);
        });

        return sseEmitter;
    }

    public SseEmitter getSseEmitter(String taskId){
        return container.get(taskId);
    }

    public void sendCompleteMessage(String taskId){
        log.info("===========start sendCompleteMessage==============");
        SseEmitter emitter = getSseEmitter(taskId);
        final SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event()
                .name("completed message")
                .data("complete");

        sendEvent(emitter, sseEventBuilder);
        log.info("===========end sendCompleteMessage==============");
    }

    private void sendEvent(SseEmitter sseEmitter ,final SseEmitter.SseEventBuilder sseEventBuilder) {
        try {
            sseEmitter.send(sseEventBuilder);
        } catch (IOException e) {
            log.error("SSEService IOException", e);
            sseEmitter.complete();
        }
    }
}
