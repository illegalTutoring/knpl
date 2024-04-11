# KNPL Backend

## 환경 세팅

1. 버전정보

   - 환경

   ```
   JDK : openJDK 17.0.9" 2023-10-17 LTS
   Framework : SpringBoot (3.2.3), Gradle(8.5)
   ```

   - Database

   ```
   Database : MongoDB v7.0.5 (Node.js v21.6.1)
   ```

2. IntelliJ 설정
   - 프로젝트 열기
     File > Open > 프로젝트 선택 > OK
     <br>
    ![image](/uploads/631d5fa93d2e5e9ef2ba1bf940b52647/image.png){: width="300"}
     <br>
    ![image](/uploads/eaa757dabbc66754bdd60889552f5645/image.png){: width="300"}
     <br>

- Gradle 설정
  - build.gradle dependencies
  ```java
  dependencies {
      implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
      implementation 'org.springframework.boot:spring-boot-starter-web'
      implementation 'org.springframework.boot:spring-boot-starter-web-services'
      compileOnly 'org.projectlombok:lombok'
      developmentOnly 'org.springframework.boot:spring-boot-devtools'
      annotationProcessor 'org.projectlombok:lombok'
      testImplementation 'org.springframework.boot:spring-boot-starter-test'

      // swagger
      implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'

      //webClient
      implementation 'org.springframework.boot:spring-boot-starter-webflux'
  }
  ```
  ![image](/uploads/8b5265c2dbaf8e20e5d8930966944f9c/image.png){: width="300"}
   <br>
  ![image](/uploads/e98e99b59fb73c77cc71485b9f890071/image.png){: width="300"}
   <br>
  ![image](/uploads/b76056fbae2050e4482ee822025f9733/image.png){: width="350"}
  <br>
  실행 버튼이 생기면 완료

3. application-env.properties
    
    ```xml
    # mongoDB
    spring.data.mongodb.uri=mongodb://{몽고DB ID}:{몽고 DB PW}@{접속 ip}:{몽고DB 접속 port}/knpl
    
    # file upload
    app.upload.dir = 내부 저장소 위치    # 마지막에 / 붙이기
    
    # AI Server
    app.ai.api2url=AI API2 서버 주소
    app.ai.api3url=AI API3 서버 주소    
    app.ai.result.dir=파일 저장할 내부 저장소 위치     # 마지막에 / 붙이기
    
    # cors
    cors.allowedOrigins=http://프론트 접속 서버,https://프론트 접속 서버
    ```

## Backend 구조
```shell
.
├── build
├── build.gradle
├── gradle
│   └── wrapper
├── gradlew
├── gradlew.bat
├── settings.gradle
├── src/main
│		└── java/com/b301/knpl
│       ├── KnplApplication.java
│       ├── config
│       │   ├── MongoConfig.java
│       │   ├── RestTemplateConfig.java
│       │   └── WebConfig.java
│       ├── controller
│       │   ├── DIFFController.java
│       │   ├── FileDownloadController.java
│       │   ├── SSEController.java
│       │   ├── SVCController.java
│       │   └── SeparationController.java
│       ├── dto
│       │   ├── FileResultDto.java
│       │   ├── FileStateDto.java
│       │   ├── MessageDto.java
│       │   ├── SVCFileDto.java
│       │   ├── SVCFileListDto.java
│       │   ├── SVCInfoDto.java
│       │   ├── SVCMixDto.java
│       │   ├── SVCResponseDto.java
│       │   ├── SVCWithSepDto.java
│       │   └── SeparationDto.java
│       ├── entity
│       │   ├── Custom.java
│       │   ├── File.java
│       │   ├── Message.java
│       │   ├── Mix.java
│       │   ├── Result.java
│       │   ├── SVC.java
│       │   ├── SVCMix.java
│       │   ├── Separation.java
│       │   └── Task.java
│       ├── repository
│       │   ├── DIFFRepository.java
│       │   ├── DIFFRepositoryImpl.java
│       │   ├── FileRepository.java
│       │   ├── KnplRepository.java
│       │   ├── KnplRepositoryImpl.java
│       │   └── TaskRepository.java
│       └── service
│           ├── CustomService.java
│           ├── FileService.java
│           ├── SSEService.java
│           ├── SVCService.java
│           ├── SeparationService.java
│           ├── TaskService.java
│           └── WebClientService.java
└── resources
    ├── application-env.properties
    ├── application-ssl.properties
    ├── application.properties
    └── keystore.p12
```

## SSE(Server-Sent Event)

### Controller

```java
public ResponseEntity<SseEmitter> connect(@PathVariable("taskId") String taskId){
	log.info("============start sse connect==========");
	SseEmitter emitter = sseService.connect(taskId);
	
	log.info("emitter: {}",emitter.toString());
	
	return ResponseEntity.status(HttpStatus.OK).body(emitter);
}
```

→ taskId로 sse emitter 생성

### Service

```java
// sseEmitter들을 모아둘 컨테이너
private static Map<String, SseEmitter> container = new ConcurrentHashMap<>(); 

public SseEmitter connect(final String taskId) {
	// 1. sseEmitter 생성, timeout 시간까지 연결 끊기지 않음
	SseEmitter sseEmitter = new SseEmitter(600000L); // 10분
	
	log.info("sse connect taskId: {}", taskId);
	
	
	// 2. 연결 정보 보냄 ( 503Service Unavailable 에러를 방지 하기 위한 더미데이터임 )
	final SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event().name("connect").data("connected!");
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
```

→ sse 연결 한 번 연결 시 10분 유지하도록 설정

→ 연결 시 connect 이벤트 발생

```java
public void sendCompleteMessage(String taskId){
	log.info("===========start sendCompleteMessage==============");
	SseEmitter emitter = getSseEmitter(taskId);
	final SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event().name("completed message").data("complete");
	
	sendEvent(emitter, sseEventBuilder);
	log.info("===========end sendCompleteMessage==============");
}
```

→ taskId에 맞는 sse emitter를 가져오고 “complete message” 이벤트를 만들어 front에 작업 완료 메세지를 보냄

```java
private void sendEvent(SseEmitter sseEmitter, final SseEmitter.SseEventBuilder sseEventBuilder) {
	try {
		sseEmitter.send(sseEventBuilder);
	} catch (IOException e) {
		log.error("SSEService IOException", e);
		sseEmitter.complete();
	}
}
```

→ sendEvent 메소드 구현부

---

## **Front**

```jsx
const sseConnect = (param:string) => {
	const sse = new EventSource(`${import.meta.env.VITE_APP_API_URL}/api/sse/connect/${param}`);
	
	sse.addEventListener('completed message', event => {
		console.log(event.data);
		console.log("이벤트 발생");
		done.value = true;
	})
	
	sse.onopen = function (e) {
		console.log("connect!");
		console.log(e);
	}
	
	sse.onerror = function (error) {
		console.log("error!!");
		console.log(error);
		sse.close();
	}
}
```

