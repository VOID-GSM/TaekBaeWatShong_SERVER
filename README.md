# 택배왔숑 (TaekBaeWatShong)

택배왔숑은 광주소프트웨어마이스터고등학교로 배송되는 택배가 방치되거나 분실되지 않도록 관리하는 서비스입니다. 학생과 선생님이 학교로 주문한 택배마다 주인, 도착 시각, 보관 구역, 수령 여부를 기록합니다. 오래 찾아가지 않은 택배는 알림을 보내 수령하도록 안내합니다.

이 저장소는 택배왔숑의 백엔드 API 서버입니다. 프론트엔드는 학생과 선생님이 쓰는 **사용자 앱**, 관리자와 택배 기사님이 쓰는 **관리자 앱** 두 가지이며, 둘 다 이 서버 하나를 사용합니다.

## 사용자

| 역할 | 로그인 | 설명 |
|---|---|---|
| 학생 (`STUDENT`) | Google, DataGSM | 택배를 등록하고, 내 택배를 조회·수령하고, 분실 신고를 합니다. |
| 선생님 (`TEACHER`) | Google, DataGSM | 학생과 같은 기능을 사용합니다. |
| 관리자 (`ADMIN`) | Google, DataGSM, 이메일 (초대 코드로 등록) | 학생생활안전부 선생님, 학생회 학생생활안전부 학생, VOID 팀, 택배 기사님이 사용합니다. 전체 택배 관리, 도착 스캔, 구역 배정, 분실 신고 처리, 택배기사 QR을 담당합니다. |

이메일·비밀번호 로그인(`/auth/email/**`)도 함께 제공합니다.

## 주요 기능

- **인증**: Google·DataGSM OAuth 로그인을 제공합니다. 로그인 후 일회용 코드를 JWT로 교환합니다. 사용자 앱과 관리자 앱은 리다이렉트 주소가 따로 있고, 관리자는 DB의 역할(`ADMIN`)로 판별합니다. 처음 관리자로 등록할 때는 기존 관리자가 발급한 초대 코드가 필요합니다. 코드 하나는 발급 시 정한 인원(기본 1명, 최대 20명)까지 사용할 수 있고, 24시간 뒤 만료됩니다.
- **택배 등록**: 운송장 번호, 택배사, 상품명(선택)을 입력해 등록합니다. 운송장은 Apick 배송 조회 API로 검증합니다.
- **택배 조회**: 내 택배 목록을 볼 수 있고, 관리자는 전체 택배를 상태별로 조회합니다. 수령한 택배는 수령 후 3일 동안만 목록에 나타납니다.
- **도착 처리**: 관리자 앱에서 바코드 스캐너로 운송장을 찍으면 택배가 도착 처리되고 보관 구역이 지정됩니다.
- **보관 구역**: 선반 2개 × 3단으로 이루어진 A~F 구역별로 택배 수와 목록을 보여 주며, 구역을 다시 배정할 수 있습니다.
- **택배 수령**: 본인 택배의 바코드를 스캔해 수령 처리합니다. 도착 후 지난 일수(`unclaimedDays`)도 함께 내려 줍니다.
- **분실 신고**: 분실 예상 시각과 내용을 적어 신고합니다. 관리자는 신고 목록을 확인하고 처리 상태를 접수 → 처리 중 → 완료로 바꿉니다.
- **1:1 채팅**: 택배 기사님(관리자)과 학생·선생님이 대화할 수 있고, DataGSM으로 로그인한 학생과 선생님끼리도 대화할 수 있습니다. 메시지는 REST로 보내고, 실시간 수신은 STOMP(WebSocket)로 받습니다. 자세한 내용은 [실시간 채팅 연동 가이드](docs/CHAT-REALTIME.md)를 참고하세요.
- **알림**: 택배 등록·도착·구역 배정·수령 시 앱 내 알림을 남깁니다. 찾아가지 않은 택배는 스케줄러가 정해진 경과일(기본 1·3·5일)마다 다시 알립니다. 푸시 알림에 쓸 디바이스 토큰도 등록할 수 있습니다.
- **택배기사 QR**: 관리자가 택배기사 QR 코드(300×300 PNG)를 발급하고, QR을 스캔해 방문 기록을 남깁니다.

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language & Framework | Kotlin 1.9.23, Spring Boot 3.2.5 (Java 21) |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL |
| Auth | Spring Security, OAuth2 Client (Google, DataGSM), JWT (JJWT 0.12.6) |
| 실시간 | Spring WebSocket + STOMP (simple broker) |
| 외부 연동 | Apick 배송 조회 API |
| QR 코드 | ZXing |
| API 문서 | Springdoc OpenAPI (Swagger UI) |
| Lint | ktlint |
| Test | JUnit 5, Spring Security Test |
| Build & Deploy | Gradle (Kotlin DSL), GitHub Actions, systemd |

## 프로젝트 구조

```
com.example.taekbaewatshongserver
├── domain        # 도메인별 controller, dto(request/response), entity, repository, service
│   ├── parcel        # 택배 등록·도착·구역·수령, Apick 운송장 검증
│   ├── report        # 분실 신고
│   ├── chat          # 1:1 채팅, 실시간 이벤트 발행
│   ├── notification  # 알림, 디바이스 토큰, 미수령 재알림 스케줄러
│   ├── driver        # 택배기사 QR 발급·스캔
│   └── user          # 사용자
└── global        # config, exception, security(jwt, oauth, email, websocket)
```

## 시작하기

### 요구 사항

- JDK 21
- MySQL 8

### 환경 변수

설정은 `src/main/resources/application.yml`에서 환경 변수로 주입됩니다. 프로젝트 루트에 `.env` 파일을 두면 `bootRun`과 IDE 실행 시 자동으로 읽습니다. `.env`는 git에 올라가지 않습니다.

**필수** (없으면 서버가 뜨지 않습니다)

| 분류 | 변수 |
|---|---|
| DB | `DB_URL`(JDBC URL), `DB_USERNAME`, `DB_PASSWORD` |
| 인증 | `JWT_SECRET`(32바이트 이상), `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `DATAGSM_CLIENT_ID`, `DATAGSM_CLIENT_SECRET` |

**선택** (기본값이 있습니다)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `OAUTH2_REDIRECT_URI` | `http://localhost:3000/oauth2/redirect` | 사용자 앱 로그인 후 리다이렉트 주소 |
| `ADMIN_OAUTH2_REDIRECT_URI` | `http://localhost:3001/oauth2/redirect` | 관리자 앱 로그인 후 리다이렉트 주소 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:3001` | CORS·WebSocket 허용 Origin (쉼표로 구분) |
| `APICK_API_KEY` | (비어 있음) | Apick 배송 조회 API 키. **비어 있으면 운송장 검증을 건너뛰고 모든 운송장을 통과시킵니다.** |
| `UNCLAIMED_REMINDER_CRON` | `0 0 8 * * *` | 미수령 재알림 실행 시각 (매일 오전 8시) |
| `UNCLAIMED_REMINDER_DAYS` | `1,3,5` | 도착 후 며칠째에 재알림을 보낼지 |

관리자 허용 목록 환경 변수(`ADMIN_EMAILS`)는 없어졌습니다. 첫 관리자는 `application.yml`의 `app.admin.bootstrap-email` 고정값(`teamvoid0107@gmail.com`)으로 Google·DataGSM 관리자 로그인을 해서 등록하고, 이후 관리자는 `POST /admin/invite-codes`로 발급한 초대 코드로 등록합니다. 부트스트랩 이메일은 이메일·비밀번호 가입에는 적용되지 않습니다.

### 설치 및 실행

```bash
git clone https://github.com/VOID-GSM/TaekBaeWatShong_SERVER.git
cd TaekBaeWatShong_SERVER
./gradlew bootRun
```

DB 스키마는 `ddl-auto: update`로 서버가 시작될 때 자동으로 반영됩니다.

기본 포트는 `8080`이며, 서버를 실행한 뒤 아래 주소에서 API 문서를 확인할 수 있습니다.

http://localhost:8080/swagger-ui.html

### 테스트

```bash
./gradlew ktlintCheck   # 코드 스타일 검사
./gradlew test          # 테스트 실행
```

`TaekBaeWatShongServerApplicationTests`는 Spring 컨텍스트 전체를 띄우므로 MySQL과 필수 환경 변수가 있어야 합니다. 나머지 단위 테스트(채팅, WebSocket 인증, CORS, OAuth 등)는 DB 없이도 실행됩니다.

## CI/CD

- **CI**: `main`, `develop`에 푸시하거나 이 두 브랜치로 PR을 올리면 MySQL 서비스 컨테이너를 띄운 뒤 `ktlintCheck` → `test` → `bootJar` 순서로 실행합니다.
- **CD**: `develop`이나 `main`에 푸시되고 CI를 통과하면 jar를 GSMSV 서버에 SSH로 올리고 systemd 사용자 서비스로 재시작합니다. `develop`은 개발 서버, `main`은 운영 서버에 배포됩니다. 새 버전이 기동에 실패하면 직전 jar로 자동 롤백합니다.
- 각 단계의 결과는 디스코드로 알림이 갑니다.

## 문서

| 문서 | 내용 |
|---|---|
| [docs/PRODUCT.md](docs/PRODUCT.md) | 제품 명세 (사용자 앱·관리자 앱 기능, 비즈니스 규칙) |
| [docs/DOMAIN-MODEL.md](docs/DOMAIN-MODEL.md) | 도메인 모델, 택배 상태 전이 |
| [docs/IMPLEMENTATION-STATUS.md](docs/IMPLEMENTATION-STATUS.md) | 명세 대비 구현 현황 |
| [docs/CHAT-REALTIME.md](docs/CHAT-REALTIME.md) | 실시간 채팅(STOMP) 연동 가이드 |
| [AGENTS.md](AGENTS.md) | 코딩 컨벤션, 커밋·PR 규칙 |

## 팀

[VOID](https://github.com/VOID-GSM) — 광주소프트웨어마이스터고등학교
