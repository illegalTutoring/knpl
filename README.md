# AI 음성 분리, 변환, 합성 서비스 `KNPL`

## ✔️ 한 줄 소개

### Make it, Play it. KNPL

**KNPL은 음원 분리 및 목소리 변환, 목소리 커스텀 서비스입니다.**

**DDSP model을 활용하여 음원 분리 및 목소리 변환, DIFF 모델을 활용하여 목소리를 커스텀합니다.**

**또한 해당 결과물 중 원하는 결과만 MIX, Download까지 지원합니다.**

## ✔️ 기획 배경

하나의 음원을 여러명의 가수들의 아카펠라로 바꿀 수 없을까? 🎤

혹은 어떤 음원에 내 목소리를 입힌다면 어떨까? 🎶

KNPL은 단순한 궁금증에서 시작한 서비스 입니다.

## ✔️ 타겟

- 원하는 음원을 [음성, 베이스, 드럼, 이외] 로 분리하고 싶은 유저
- 원하는 음원을 아카펠라로 만들고 싶은 유저
- 음원을 내 목소리로 변경하고 싶은 유저

## ✔️ 주요기능

### 1. 음원 분리

- 오디오 파일 전송 시 vocals, drums, bass, other 4가지의 세션으로 음원을 분리
- 각 파일 미리 듣기 및 개별 파일 다운로드 가능
- 이퀄라이저 제공

### 2. 음원 분리 후 믹싱

- 각 세션 중 원하는 부분만 선택 후 믹싱
- 파일 다운로드 가능

### 3. 음원 분리 후 변환

- 각 세션 중 원하는 부분만 선택 후 원하는 목소리로 변환
- 각 파일 미리 듣기 및 개별 파일 다운로드 가능
- 이퀄라이저 제공

### 4. 음원 변환

- 음원 파일과 원하는 목소리 선택 시 해당 목소리로 음원을 변환하여 제공
  - 주요기능 1 + 주요기능 3 기능을 한번에 작업
  - 각 파일 미리듣기 및 개별 파일 다운로드 가능
  - 이퀄라이저 제공
- 파일 다운로드 가능
- 믹싱 기능 제공

### 5. AI 커스텀 음원 합성

- 음원과 학습을 위한 목소리 파일을 선택 제출 후 AI 커스텀 합성 시작
- 고유한 토큰 키를 발급 받고 완료 된 후 토큰 값을 통해 해당 작업물 접근
- 파일 다운로드 가능

## ✔️ 기대 효과

- 음원의 원하는 세션만 추출이 가능하다.
- 기존 음원 파일을 다른 목소리로 변경해 들을 수 있다.
- 사용자가 원하는 목소리를 이용해 음원 파일을 만들 수 있다.

## ✔️ 프로젝트 사용 도구

- 이슈 관리: Jira
- 형상 관리: Gitlab
- 커뮤니케이션: Notion, Mattermost, Discord
- 디자인: Figma, element-plus
- UCC: Movavi video editor plus 2022
- Infra: Jenkins, sonarqube
- Architecture diagram: Cloudcraft

## ✔️ 개발 도구

- Visual Studio Code: 1.85.1
- Intellij Ultimate: 2023.3.2
- Jira Webhook
- Terminus , MobaXterm

## ✔️ 개발 환경

### Frontend

| 이름                | 버전                         |
| ------------------- | ---------------------------- |
| Node.js             | 20.11.1(includes npm 10.2.4) |
| Vue-cli             | 5.0.4                        |
| Vue                 | 3.4.21                       |
| Pinia               | 2.1.7                        |
| wavesurfer.js       | 7.7.3                        |
| Volar(Vue-official) |                              |

### Backend

| 이름       | 버전                           |
| ---------- | ------------------------------ |
| Java       | openJDK 17.0.9" 2023-10-17 LTS |
| SpringBoot | 3.2.3                          |
| Gradle     | 8.5                            |

### DB

| 이름       | 버전                    |
| ---------- | ----------------------- |
| MongoDB    | 7.0.5 (Node.js v21.6.1) |
| Redis      | 7.2.4                   |
| PostgreSQL | 12.18                   |

### AI1 - Diff-svc

| 이름   | 버전 |
| ------ | ---- |
| python | 3.9  |
| cuda   | 11.6 |

### AI2 - DDSP-svc

| 이름   | 버전 |
| ------ | ---- |
| python | 3.9  |
| cuda   | 11.8 |

### Service

| 이름                        | 버전    |
| --------------------------- | ------- |
| docker                      | 25.0.3  |
| docker-compose              | 2.24.6  |
| Portainer Community Edition | 2.19.4  |
| Jenkins                     | 2.440.1 |
| Sonarqube                   | 10.4.1  |

더 자세한 정보는 포팅 매뉴얼 참고

## ✔️ 시스템 구조도

![knpl_architecture](/uploads/390f4ba1f5f7b646b30d50f740aab291/undefined__1_.png)

## ✔️ 프로젝트 파일 구조

### back

```
.
├── build
├── build.gradle
├── gradle
│   └── wrapper
├── gradlew
├── gradlew.bat
├── settings.gradle
├── src/main
│		└── java/com/b301/knpl
│       ├── KnplApplication.java
│       ├── config
│       │   ├── MongoConfig.java
│       │   ├── RestTemplateConfig.java
│       │   └── WebConfig.java
│       ├── controller
│       │   ├── DIFFController.java
│       │   ├── FileDownloadController.java
│       │   ├── SSEController.java
│       │   ├── SVCController.java
│       │   └── SeparationController.java
│       ├── dto
│       │   ├── FileResultDto.java
│       │   ├── FileStateDto.java
│       │   ├── MessageDto.java
│       │   ├── SVCFileDto.java
│       │   ├── SVCFileListDto.java
│       │   ├── SVCInfoDto.java
│       │   ├── SVCMixDto.java
│       │   ├── SVCResponseDto.java
│       │   ├── SVCWithSepDto.java
│       │   └── SeparationDto.java
│       ├── entity
│       │   ├── Custom.java
│       │   ├── File.java
│       │   ├── Message.java
│       │   ├── Mix.java
│       │   ├── Result.java
│       │   ├── SVC.java
│       │   ├── SVCMix.java
│       │   ├── Separation.java
│       │   └── Task.java
│       ├── repository
│       │   ├── DIFFRepository.java
│       │   ├── DIFFRepositoryImpl.java
│       │   ├── FileRepository.java
│       │   ├── KnplRepository.java
│       │   ├── KnplRepositoryImpl.java
│       │   └── TaskRepository.java
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

### front

```
├─public
│  └─assets
│      ├─background-image
│      ├─navbar-image
│      └─title-loading-image
└─src
    ├─components
    │  ├─check-result
    │  ├─exit-alarm
    │  ├─image-slide
    │  ├─loading
    │  │  └─scss
    │  ├─music-conversion
    │  ├─music-input
    │  ├─music-separation
    │  └─navbar
    ├─css
    ├─fonts
    ├─router
    ├─stores
    ├─styles
    └─views
```

## ✔️ 프로젝트 산출물

- <a href ="#"> 기획서 </a>
- <a href ="#"> 기능 명세서 </a>
- <a href ="#"> 화면 설계 </a>
- <a href ="#"> API 명세서 </a>
- <a href ="#"> ERD </a>

## ✔️ 프로젝트 결과물

- <a href ="#"> 포팅매뉴얼 </a>
- <a href ="#"> Back README.md </a>
- <a href ="#"> Front README.md </a>
- <a href ="#"> Infra README.md </a>
- <a href ="#"> AI - DIFF README.md </a>
- <a href ="#"> AI - DDSP README.md </a>

## ✔️ KNPL 서비스 화면
