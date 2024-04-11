package com.b301.knpl.controller;


import com.b301.knpl.dto.FileResultDto;
import com.b301.knpl.dto.FileStateDto;
import com.b301.knpl.dto.MessageDto;
import com.b301.knpl.dto.SeparationDto;
import com.b301.knpl.entity.*;
import com.b301.knpl.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@RestController
@Tag(name = "separation", description = "음성분리 API")
@RequestMapping("/api/separation")
@RequiredArgsConstructor
public class SeparationController {

    private final FileService fileService;
    private final TaskService taskService;
    private final WebClientService webClientService;
    private final SeparationService separationService;
    private final SSEService sseService;

    private static final String MESSAGE = "message";

    /**
     * @param file            mp3, wav 파일 타입의 multiPartFile
     * @param outputExtension 결과물 확장자 타입
     * @return ResponseEntity
     * @Name fileUpload
     * @Method POST
     * @
     */
    @Operation(summary = "파일 업로드", description = "새롭게 작업할 음원을 올립니다.")
    @ApiResponse(responseCode = "200", description = "업로드 성공")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터 값")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> fileUpload(@RequestPart MultipartFile file, @RequestPart String outputExtension) {

        log.info("================start file upload==================");

        HashMap<String, String> responseBody = new HashMap<>();

        if (file == null || file.isEmpty()) {
            log.info("파일이 null 이거나 file 내용이 비어있음");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            log.error(Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        log.info(String.format("들어온 파일: %s", file.getOriginalFilename()));

        if (Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "mp3") || Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "wav")) {

            String taskId = taskService.insertFileAndGetTaskId(file, outputExtension);
            File resultFile = fileService.getFile(taskService.getTask(taskId).getOriginalFile());
            webClientService.createSeparationRequest(resultFile,outputExtension, resultFile.getExtension(), taskId);

            responseBody.put(MESSAGE, Message.FILE_RECEIVED_SUCCESSFULLY.getStatusMessage());
            responseBody.put("taskId", taskId);

            log.info("파일을 무사히 받고 AI에 전송함");

            return ResponseEntity.status(Message.FILE_RECEIVED_SUCCESSFULLY.getHttpCode()).body(responseBody);
        } else {
            //지원하지 않는 파일형식
            responseBody.put(MESSAGE, responseBody.getOrDefault(MESSAGE, Message.UNSUPPORTED_FILE_FORMAT.getStatusMessage()));
            log.error("프론트에서 지원하지 않는 파일 형식 보냄");
            return ResponseEntity.status(Message.UNSUPPORTED_FILE_FORMAT.getHttpCode()).body(responseBody);
        }

    }


    /**
     * @param vocals MultipartFile
     * @param drums  MultipartFile
     * @param bass   MultipartFile
     * @param other  MultipartFile
     * @param taskId String
     * @return message
     */
    @Operation(summary = "음성 AI 분리 완료", description = "분리된 음성을 전달 받습니다.")
    @ApiResponse(responseCode = "200", description = "파일 전달 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터 값")
    @ApiResponse(responseCode = "404", description = "파일이 존재하지 않습니다.")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    @PostMapping(path = "/{taskId}/completion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> separationDone(@RequestPart MultipartFile vocals, @RequestPart MultipartFile drums, @RequestPart MultipartFile bass, @RequestPart MultipartFile other, @PathVariable("taskId") String taskId) {
        log.info("========AI 서버에서 분리 완료 데이터 받기===========");
        log.info(String.format("vocals: %s", vocals.getOriginalFilename()));
        log.info(String.format("drums: %s", drums.getOriginalFilename()));
        log.info(String.format("bass: %s", bass.getOriginalFilename()));
        log.info(String.format("other: %s", other.getOriginalFilename()));

        List<MultipartFile> files = fileService.createFileList(vocals, drums, bass, other);

        //CollectionUtils: list data empty 검사와 list null 인경우 동시 처리
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                MessageDto message = MessageDto.builder().message(Message.FILE_NOT_FOUND.getStatusMessage()).build();
                return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(message);
            }
        }
        if (taskId == null || taskId.length() != 24) {
            MessageDto message = MessageDto.builder().message(Message.BAD_REQUEST.getStatusMessage()).build();
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(message);
        }
        //list size != 4
        if (files.size() != 4) {
            MessageDto message = MessageDto.builder().message(Message.NOT_ENOUGH_FILES.getStatusMessage()).build();
            return ResponseEntity.status(Message.NOT_ENOUGH_FILES.getHttpCode()).body(message);
        }

        //리스트 데이터 확장자 검사
        for (MultipartFile file : files) {
            if (!Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "mp3") && !Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "wav")) {
                MessageDto message = MessageDto.builder().message(Message.UNSUPPORTED_FILE_FORMAT.getStatusMessage()).build();
                return ResponseEntity.status(Message.UNSUPPORTED_FILE_FORMAT.getHttpCode()).body(message);
            }
        }


        //separationCode 를 통해 task Collection 에서 document 를 찾으려 했는데 그게 없으면  return 404
        if (taskService.getTask(taskId) == null) {
            MessageDto message = MessageDto.builder().message(Message.TASK_NOT_FOUND.getStatusMessage()).build();
            return ResponseEntity.status(Message.TASK_NOT_FOUND.getHttpCode()).body(message);
        }

        taskService.completeSeparation(vocals, drums, bass, other, taskId);
        sseService.sendCompleteMessage(taskId);

        MessageDto message = MessageDto.builder().message(Message.RESPONSE_COMPLETED.getStatusMessage()).build();
        return ResponseEntity.status(Message.RESPONSE_COMPLETED.getHttpCode()).body(message);

    }

    @Operation(summary = "음성 분리 데이터 전달", description = "분리된 음성을 client에게 전달 합니다.")
    @ApiResponse(responseCode = "200", description = "파일 전달 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터 값")
    @ApiResponse(responseCode = "404", description = "파일이 존재하지 않습니다.")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    @GetMapping("/{taskId}")
    public ResponseEntity<Object> getSeparationSound(@PathVariable("taskId") String taskId) {
        log.info("============분리된 음성데이터를 Front에 전달==============");

        HashMap<String, Object> responseBody = new HashMap<>();

        //파라미터 검증 1: 길이가 24
        if (taskId == null || taskId.length() != 24) {
            log.error("taskId 길이 부족");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        //해당 task separation file 찾기
        List<SeparationDto> files = separationService.getSeparationDto(taskId);

        if (files.isEmpty()) {
            log.error("옳지 않은 taskId 혹은 separation 존재하지 않음");
            responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        log.info("------------------보낼 정보-------------------");
        log.info("vocals:            {}", files.get(0).getFileName());
        log.info("vocals_endpoint:   {}", files.get(0).getEndPoint());
        log.info("drums:             {}", files.get(1).getFileName());
        log.info("drums_endpoint:    {}", files.get(1).getEndPoint());
        log.info("bass:              {}", files.get(2).getFileName());
        log.info("bass_endpoint:     {}", files.get(2).getEndPoint());
        log.info("other:             {}", files.get(3).getFileName());
        log.info("other_endpoint:    {}", files.get(3).getEndPoint());

        responseBody.put(MESSAGE, Message.RESPONSE_COMPLETED.getStatusMessage());
        responseBody.put("files", files);


        return ResponseEntity.status(Message.RESPONSE_COMPLETED.getHttpCode()).body(responseBody);


    }


    @Operation(summary = "분리된 음원 선택 후 믹싱요청", description = "AI서버에 선택한 파일 믹싱 요청")
    @ApiResponse(responseCode = "200", description = "파일 믹싱 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "파일이 존재하지 않습니다.")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    @PostMapping(value = "/{taskId}/mixing")
    public ResponseEntity<Object> mixing(@RequestBody FileStateDto fileStateDto, @PathVariable("taskId") String taskId) {
        log.info("===== [method] mixing =====");
        log.info(String.format("vocals select: %s", fileStateDto.isVocals()));
        log.info(String.format("drums select: %s", fileStateDto.isDrums()));
        log.info(String.format("bass select: %s", fileStateDto.isBass()));
        log.info(String.format("other select: %s", fileStateDto.isOther()));
        log.info(fileStateDto.toString());

        Map<String, String> responseBody = new HashMap<>();

        // file state 검사 >> 전부 false면 오류
        if (!(fileStateDto.isVocals() || fileStateDto.isDrums() || fileStateDto.isBass() || fileStateDto.isOther())) {
            log.error("믹스할 파일 미선택");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        // taskId 검사
        if (taskId == null || taskId.isEmpty()) {
            log.error("입력된 taskId null 혹은 없음");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        if (taskId.length() != 24) {
            log.error("옳지않은 taskId 입력");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        Task task = taskService.getTask(taskId);

        if (task == null) {
            log.error("존재하지 않는 task");
            responseBody.put(MESSAGE, Message.TASK_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.TASK_NOT_FOUND.getHttpCode()).body(responseBody);
        }
        log.info("=======task 정보========");
        log.info("taskID: {} ", task.getId());
        log.info("originalFiles: {}", task.getOriginalFile());
        log.info("result: {}", task.getResult());
        log.info("outputExtension: {}", task.getOutputExtension());

        //해당 정보 AI 서버에 전송
        webClientService.createMixingRequest(fileStateDto, taskId);

        log.info("파일 믹싱 요청 완료");
        responseBody.put(MESSAGE, Message.FILE_RECEIVED_SUCCESSFULLY.getStatusMessage());
        return ResponseEntity.status(Message.FILE_RECEIVED_SUCCESSFULLY.getHttpCode()).body(responseBody);

    }

    @Operation(summary = "믹싱된 음원 받기", description = "믹싱 완료된 음성 AI서버에서 받아옴")
    @ApiResponse(responseCode = "200", description = "파일 믹싱 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "파일이 존재하지 않습니다.")
    @ApiResponse(responseCode = "500", description = "서버 에러")
    @PostMapping("/{taskId}/mixing/completion")
    public ResponseEntity<Object> mixingDone(@RequestPart MultipartFile file, @PathVariable("taskId") String taskId) {

        log.info("========AI 서버에서 믹싱 완료 데이터 받기===========");

        //파일 존재 검사
        if (file == null || file.isEmpty()) {
            log.info("file 이 입력되지 않음 혹은 파일이 비어있음");
            MessageDto message = MessageDto.builder().message(Message.FILE_NOT_FOUND.getStatusMessage()).build();
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(message);
        }

        log.info("file: {}", file.getOriginalFilename());

        if (taskId == null) {
            log.info("taskId 입력되지 않음");
            MessageDto message = MessageDto.builder().message(Message.BAD_REQUEST.getStatusMessage()).build();
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(message);
        }

        if (taskId.length() != 24) {
            log.info("taskId 길이 부족");
            MessageDto message = MessageDto.builder().message(Message.BAD_REQUEST.getStatusMessage()).build();
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(message);
        }

        if (!Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "mp3") && !Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "wav")) {
            log.info("지원하지 않는 파일 입력됨");
            MessageDto message = MessageDto.builder().message(Message.UNSUPPORTED_FILE_FORMAT.getStatusMessage()).build();
            return ResponseEntity.status(Message.UNSUPPORTED_FILE_FORMAT.getHttpCode()).body(message);
        }


        //taskId 통해 task Collection에서 document를 찾으려 했는데 그게 없으면  return 404
        int updateTask = taskService.updateMix(file, taskId);

        //프론트에 SSE 메시지 보내기
        sseService.sendCompleteMessage(taskId);

        if (updateTask == 1) {
            log.info("파일 성공적으로 전달받음");
            MessageDto message = MessageDto.builder().message(Message.FILE_RECEIVED_SUCCESSFULLY.getStatusMessage()).build();
            return ResponseEntity.status(Message.FILE_RECEIVED_SUCCESSFULLY.getHttpCode()).body(message);
        } else {
            log.info("해당 작업을 찾을 수 없음");
            MessageDto message = MessageDto.builder().message(Message.TASK_NOT_FOUND.getStatusMessage()).build();
            return ResponseEntity.status(Message.TASK_NOT_FOUND.getHttpCode()).body(message);
        }

    }

    @Operation(summary = "음성 분리 데이터 전달", description = "분리된 음성을 client에게 전달 합니다.")
    @ApiResponse(responseCode = "200", description = "파일 전달 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터 값")
    @ApiResponse(responseCode = "404", description = "파일이 존재하지 않습니다.")
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    @GetMapping("/{taskId}/mixing")
    public ResponseEntity<Object> getMixingSound(@PathVariable("taskId") String taskId) {
        log.info("============분리된 음성데이터를 Front에 전달==============");

        HashMap<String, Object> responseBody = new HashMap<>();

        //파라미터 검증 1: 길이가 24
        if (taskId.length() != 24) {
            log.error("taskId 길이 부족");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        //해당 task separation file 찾기
        FileResultDto dto = separationService.getMixDto(taskId);

        if (dto == null) {
            log.error("옳지 않은 taskId 혹은 separation 존재하지 않음");
            responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        log.info("------------------보낼 정보-------------------");
        log.info("fileName: {}", dto.getFileName());
        log.info("filePath: {}", dto.getEndPoint());

        responseBody.put("fileName", dto.getFileName());
        responseBody.put("filePath", dto.getEndPoint());
        responseBody.put(MESSAGE, Message.RESPONSE_COMPLETED.getStatusMessage());

        return ResponseEntity.status(Message.RESPONSE_COMPLETED.getHttpCode()).body(responseBody);

    }

}
