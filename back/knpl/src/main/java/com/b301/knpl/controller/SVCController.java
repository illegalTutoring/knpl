package com.b301.knpl.controller;

import com.b301.knpl.dto.*;
import com.b301.knpl.entity.*;
import com.b301.knpl.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "svc", description = "SVC API")
@RequestMapping("/api/svc")
@Slf4j
public class SVCController {

    private final FileService fileService;
    private final TaskService taskService;
    private final SVCService svcService;
    private final WebClientService webClientService;
    private final SSEService sseService;

    private static final String MESSAGE = "message";

    @Operation(summary = "음성 변환(분리O)", description = "음성 분리 후 음성 변환 시작 요청")
    @ApiResponse(responseCode = "200", description = "음성 변환 시작")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "422", description = "지원하지 않는 확장자")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @PostMapping("/separation")
    public ResponseEntity<Object> startSVCWithSep(@RequestBody SVCFileListDto svcFile) {
        log.info("===== [SVCController] startSVCWithSep start =====");

        Map<String, String> responseBody = new HashMap<>();

        // svc 파일 없는 경우 (400)
        if(CollectionUtils.isEmpty(svcFile.getSvcFile())) {
            log.error("svc file info is null or empty");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        if(svcFile.getSvcFile().size() != 4) {
            log.error("svc file info count is not fit");
            responseBody.put(MESSAGE, Message.NOT_ENOUGH_FILES.getStatusMessage());
            return ResponseEntity.status(Message.NOT_ENOUGH_FILES.getHttpCode()).body(responseBody);
        }

        // request 요청 생성
        webClientService.createSVCWithSepRequest(svcFile.getSvcFile(), svcFile.getTaskId());

        log.info("file received successfully. Start voice conversion");
        responseBody.put(MESSAGE, Message.REQUEST_RECEIVED.getStatusMessage());
        responseBody.put("taskId", svcFile.getTaskId());
        return ResponseEntity.status(Message.REQUEST_RECEIVED.getHttpCode()).body(responseBody);
    }

    @Operation(summary = "AI 서버에서 변환 완료(분리O)", description = "AI 서버에서 변환 완료 request를 받음")
    @ApiResponse(responseCode = "200", description = "파일 성공적으로 받았습니다.")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @PostMapping(path="/separation/{taskId}/completion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> completeSVCWithSep(@RequestPart MultipartFile vocals, @RequestPart MultipartFile drums,
                                                     @RequestPart MultipartFile bass, @RequestPart MultipartFile other, @PathVariable("taskId") String taskId) {
        log.info("===== [SVCController] completeSVCWithSep start =====");

        log.info("===== receive multipart file name =====");
        log.info(String.format("vocals: %s", vocals.getOriginalFilename()));
        log.info(String.format("drums: %s", drums.getOriginalFilename()));
        log.info(String.format("bass: %s", bass.getOriginalFilename()));
        log.info(String.format("other: %s", other.getOriginalFilename()));
        log.info("=======================================");

        Map<String, String> responseBody = new HashMap<>();

        List<MultipartFile> files = fileService.createFileList(vocals, drums, bass, other);

        //CollectionUtils: list data empty 검사와 list가 null인경우 동시 처리
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                log.error("file이 없습니다.");
                responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
                return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
            }
        }

        // taskId 검사
        if(taskId == null || taskId.length() != 24) {
            log.error("taskId is null or invalid");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        //list size가 4가 아니면 오류
        if (files.size() != 4) {
            log.error("파일 갯수가 맞지 않습니다.");
            responseBody.put(MESSAGE, Message.NOT_ENOUGH_FILES.getStatusMessage());
            return ResponseEntity.status(Message.NOT_ENOUGH_FILES.getHttpCode()).body(responseBody);
        }

        //리스트 데이터 확장자 검사
        for (MultipartFile file : files) {
            if (!Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "mp3") &&
                    !Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "wav")) {
                log.error("지원하지 않는 파일 형식입니다.");
                responseBody.put(MESSAGE, Message.UNSUPPORTED_FILE_FORMAT.getStatusMessage());
                return ResponseEntity.status(Message.UNSUPPORTED_FILE_FORMAT.getHttpCode()).body(responseBody);
            }
        }

        // 분리 파일 DB 저장
        File v = fileService.fileUpload(vocals);
        File d = fileService.fileUpload(drums);
        File b = fileService.fileUpload(bass);
        File o = fileService.fileUpload(other);

        // Task 가져오기
        Task task = taskService.getTask(taskId);

        // task 검사
        if (task == null) {
            log.error("task is null");
            responseBody.put(MESSAGE, Message.TASK_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.TASK_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        // svc 엔터티 생성
        SVC svc = SVC.builder().vocals(v.getId()).drums(d.getId()).bass(b.getId()).other(o.getId()).build();
        Task updateTask = taskService.pushSVC(task, svc);
        log.info("updateTask : {}", updateTask.toString());

        //프론트에 SSE 보내기
        sseService.sendCompleteMessage(taskId);

        log.info("file received successfully");
        responseBody.put(MESSAGE, Message.FILE_RECEIVED_SUCCESSFULLY.getStatusMessage());
        return ResponseEntity.status(Message.FILE_RECEIVED_SUCCESSFULLY.getHttpCode()).body(responseBody);
    }

    @Operation(summary = "변환된 음성 전달하기(분리O)", description = "분리 완료 된 음성 AI서버에서 받아옴")
    @ApiResponse(responseCode = "200", description = "변환된 음성 전달하기 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @GetMapping("/separation/{taskId}")
    public ResponseEntity<Object> getSVCwithSep(@PathVariable("taskId") String taskId) {
        log.info("===== [SVCController] getSVCwithSep start =====");

        Map<String, Object> responseBody = new HashMap<>();

        if(taskId == null || taskId.length() != 24) {
            log.error("taskId가 비어있거나 올바르지 않은 값입니다.");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        //해당 task separation file 찾기
        List<SVCWithSepDto> files = svcService.getSVCWithSepDto(taskId);

        if(files.isEmpty()) {
            log.error("옳지 않은 taskId 혹은 svc 존재하지 않음");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
        }

        svcService.printfilesInfo(files);

        log.info("변환된 음성 전달 완료");
        responseBody.put(MESSAGE, Message.RESPONSE_COMPLETED.getStatusMessage());
        responseBody.put("svcWithSepFilesInfo", files);
        return ResponseEntity.status(Message.RESPONSE_COMPLETED.getHttpCode()).body(responseBody);
    }

    @Operation(summary = "변환된 음원 선택 후 믹싱(분리O)", description = "믹싱 작업 시작 요청")
    @ApiResponse(responseCode = "200", description = "파일 믹싱 작업 시작")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @PostMapping("/separation/{taskId}/mixing")
    public ResponseEntity<Object> startSVCWithSepMixing(@RequestBody FileStateDto fileStateDto, @PathVariable("taskId") String taskId) {
        log.info("===== [SVCController] startSVCWithSepMixing start =====");

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
        if(taskId == null || taskId.length() != 24) {
            log.error("taskId is null or invalid");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        Task task = taskService.getTask(taskId);
        // task 검사
        if (task == null) {
            log.error("task is null");
            responseBody.put(MESSAGE, Message.TASK_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.TASK_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        log.info("======= task 정보 ========");
        log.info("taskID: {} ", task.getId());
        log.info("originalFiles: {}", task.getOriginalFile());
        log.info("result: {}", task.getResult());
        log.info("outputExtension: {}", task.getOutputExtension());

        //해당 정보 AI 서버에 전송
        webClientService.createSVCMixingRequest(fileStateDto, taskId);

        log.info("파일 믹싱 작업 시작");
        responseBody.put(MESSAGE, Message.FILE_RECEIVED_SUCCESSFULLY.getStatusMessage());
        return ResponseEntity.status(Message.FILE_RECEIVED_SUCCESSFULLY.getHttpCode()).body(responseBody);
    }

    @Operation(summary = "변환된 음원 선택 후 믹싱 완료(분리O)", description = "믹싱 완료된 음성 AI서버에서 받아옴")
    @ApiResponse(responseCode = "200", description = "파일 믹싱 작업 시작")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @PostMapping("/separation/{taskId}/mixing/completion")
    public ResponseEntity<Object> completeSVCWithSepMixing(@RequestPart MultipartFile file, @PathVariable("taskId") String taskId) {
        log.info("===== [SVCController] completeSVCWithSepMixing start =====");

        Map<String, String> responseBody = new HashMap<>();

        //파일 존재 검사
        if (file == null || file.isEmpty()) {
            log.error("파일이 없거나 비어있습니다.");
            responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        log.info("file: {}", file.getOriginalFilename());

        // taskId 검사
        if(taskId == null || taskId.length() != 24) {
            log.error("taskId is null or invalid");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        if (!Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "mp3")
                && !Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "wav")) {
            log.error("지원하지 않는 파일 형식입니다.");
            responseBody.put(MESSAGE, Message.UNSUPPORTED_FILE_FORMAT.getStatusMessage());
            return ResponseEntity.status(Message.UNSUPPORTED_FILE_FORMAT.getHttpCode()).body(responseBody);
        }

        //taskId 통해 task Collection에서 document를 찾으려 했는데 그게 없으면  return 404
        int updateTask = taskService.updateSVCMix(file, taskId);

        //프론트에 SSE 보내기
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

    @Operation(summary = "변환된 합성 음성 전달하기(분리O)", description = "믹싱 완료된 음성 front에 전달")
    @ApiResponse(responseCode = "200", description = "파일 믹싱 작업 시작")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @GetMapping("/separation/{taskId}/mixing")
    public ResponseEntity<Object> getSVCWithSepMixing(@PathVariable("taskId") String taskId) {
        log.info("===== [SVCController] getSVCWithSepMixing start =====");

        HashMap<String, Object> responseBody = new HashMap<>();

        // taskId 검사
        if(taskId == null || taskId.length() != 24) {
            log.error("taskId is null or invalid");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        //해당 task separation file 찾기
        SVCMixDto dto = svcService.getMixDto(taskId);

        if(dto == null){
            log.error("옳지 않은 taskId 혹은 svc 존재하지 않음");
            responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        log.info("------------------보낼 정보-------------------");
        log.info("fileName: {}", dto.getFileName());
        log.info("endPoint: {}", dto.getEndPoint());

        log.info("파일 전달 완료");
        responseBody.put("fileName", dto.getFileName());
        responseBody.put("endPoint", dto.getEndPoint());
        responseBody.put(MESSAGE, Message.RESPONSE_COMPLETED.getStatusMessage());
        return ResponseEntity.status(Message.RESPONSE_COMPLETED.getHttpCode()).body(responseBody);
    }

    @Operation(summary = "음성 변환(분리X)", description = "음성 변환 요청 받음")
    @ApiResponse(responseCode = "200", description = "변환할 음성파일 업로드 완료. 변환 요쳥 시작")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> startSVC(@RequestPart MultipartFile file,
                                           @RequestPart SVCInfoDto svcInfo) throws IOException {
        log.info("===== [SVCController] startSVC start =====");

        Map<String, String> responseBody = new HashMap<>();

        // file이 없는 경우
        if(file == null || file.isEmpty()) {
            responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
            log.info("file is null or empty");
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        // 확장자 검사
        if (!Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "mp3")
                && !Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "wav")) {
            log.error("지원하지 않는 파일 형식입니다.");
            responseBody.put(MESSAGE, Message.UNSUPPORTED_FILE_FORMAT.getStatusMessage());
            return ResponseEntity.status(Message.UNSUPPORTED_FILE_FORMAT.getHttpCode()).body(responseBody);
        }

        if(svcInfo.getVoice() == null || svcInfo.getVoice().isEmpty()) {
            log.error("voice is null or empty");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        if(svcInfo.getOutputExtension() == null || svcInfo.getOutputExtension().isEmpty()) {
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            log.error("outputExtension is null or empty");
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        File newFile = fileService.fileUpload(file);
        fileService.printFileInfo(newFile, "새로운 파일 정보");

        // task 생성
        Task task = taskService.insertTask(newFile.getId(), svcInfo.getOutputExtension());

        String taskId = task.getId();
        webClientService.createSVCRequest(newFile, svcInfo, taskId);

        log.info("변환할 음성파일 업로드 완료. 변환 요청 시작");
        responseBody.put(MESSAGE, Message.REQUEST_RECEIVED.getStatusMessage());
        responseBody.put("taskId", taskId);
        return ResponseEntity.status(Message.REQUEST_RECEIVED.getHttpCode()).body(responseBody);
    }

    @Operation(summary = "AI 서버에서 변환 완료(분리X)", description = "AI 서버에서 변환 끝났다!")
    @ApiResponse(responseCode = "200", description = "파일 잘 받았다!")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @PostMapping("/{taskId}/completion")
    public ResponseEntity<Object> completeSVC(@RequestBody MultipartFile file, @PathVariable("taskId") String taskId) {
        log.info("===== [SVCController] completeSVC start =====");

        Map<String, String> responseBody = new HashMap<>();

        // file이 없는 경우
        if(file == null || file.isEmpty()) {
            log.error("file is null or empty");
            responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        // file 확장자 유효 검사
        if (!Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "mp3")
                && !Objects.equals(StringUtils.getFilenameExtension(file.getOriginalFilename()), "wav")) {
            log.error("지원하지 않는 파일 형식입니다.");
            responseBody.put(MESSAGE, Message.UNSUPPORTED_FILE_FORMAT.getStatusMessage());
            return ResponseEntity.status(Message.UNSUPPORTED_FILE_FORMAT.getHttpCode()).body(responseBody);
        }

        // taskId 검사
        if(taskId == null || taskId.length() != 24) {
            log.error("taskId is null or invalid");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        Task receiveTask = taskService.getTask(taskId);
        if(receiveTask == null) {
            log.error("receiveTask is null");
            responseBody.put(MESSAGE, Message.TASK_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.TASK_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        // 변환 완료된 파일 DB 저장
        File svcFile = fileService.fileUpload(file);
        SVCMix svcMix = SVCMix.builder().file(svcFile.getId()).build();
        Result result = Result.builder().svcMix(svcMix).build();

        taskService.updateTask(taskService.getTask(taskId), result);

        //프론트에 SSE 보내기
        sseService.sendCompleteMessage(taskId);

        log.info("file receive success!");
        responseBody.put(MESSAGE, Message.FILE_RECEIVED_SUCCESSFULLY.getStatusMessage());
        return ResponseEntity.status(Message.FILE_RECEIVED_SUCCESSFULLY.getHttpCode()).body(responseBody);
    }

    @Operation(summary = "변환된 음성 전달하기(분리X)", description = "변환 완료된 음성 파일 전달하기")
    @ApiResponse(responseCode = "200", description = "변환된 파일 보내기 완료")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 파라미터")
    @ApiResponse(responseCode = "404", description = "NOT FOUND")
    @ApiResponse(responseCode = "500", description = "서버 에러!")
    @GetMapping("/{taskId}")
    public ResponseEntity<Object> getSVC(@PathVariable("taskId") String taskId) {
        log.info("===== [SVCController] getSVC start =====");

        Map<String, String> responseBody = new HashMap<>();

        // taskId 검사
        if(taskId == null || taskId.length() != 24) {
            log.error("taskId is null or invalid");
            responseBody.put(MESSAGE, Message.BAD_REQUEST.getStatusMessage());
            return ResponseEntity.status(Message.BAD_REQUEST.getHttpCode()).body(responseBody);
        }

        // Task 가져오기
        Task task = taskService.getTask(taskId);

        // task 검사
        if (task == null) {
            log.error("task is null");
            responseBody.put(MESSAGE, Message.TASK_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.TASK_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        SVCResponseDto dto = svcService.getSVCResponseDto(taskId);

        if(dto == null){
            log.error("옳지 않은 taskId 혹은 svc 존재하지 않음");
            responseBody.put(MESSAGE, Message.FILE_NOT_FOUND.getStatusMessage());
            return ResponseEntity.status(Message.FILE_NOT_FOUND.getHttpCode()).body(responseBody);
        }

        log.info("------------------보낼 정보-------------------");
        log.info("fileName: {}", dto.getFileName());
        log.info("endPoint: {}", dto.getEndPoint());

        log.info("변환된 음성 전달 완료");
        responseBody.put("fileName", dto.getFileName());
        responseBody.put("endPoint", dto.getEndPoint());
        responseBody.put(MESSAGE, Message.RESPONSE_COMPLETED.getStatusMessage());
        return ResponseEntity.status(Message.RESPONSE_COMPLETED.getHttpCode()).body(responseBody);
    }
}
