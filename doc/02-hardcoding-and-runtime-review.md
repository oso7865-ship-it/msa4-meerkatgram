# 하드코딩·설정·물리적 실행 오류 검수

## 1. 하드코딩 및 설정 충돌

### HC-01 `[P0][확정]` 모든 환경에서 더미 SQL이 실행될 수 있음

**근거**

- `application.yaml`: `spring.sql.init.data-locations: classpath*:dummy/*.sql`
- `application.yaml`: `spring.sql.init.mode: always`
- `01_dummy_users.sql`: `DELETE FROM users`
- `02_dummy_posts.sql`: `DELETE FROM posts`

Spring Boot의 프로필 YAML은 기본 YAML을 교체하는 것이 아니라 **겹쳐서 적용**합니다. `application-prod.yaml`이 `spring.sql.init.mode`를 `never`로 덮어쓰지 않으므로 운영 프로필도 기본값 `always`를 상속할 수 있습니다.

**영향**

- 운영 서버 재시작 시 회원·게시글 데이터가 삭제되고 고정 더미 데이터로 교체될 수 있습니다.
- 고정 ID insert와 FK 검사 변경까지 포함돼 데이터 무결성에 큰 영향을 줍니다.

**권장 수정**

- 공통 설정은 `mode: never`로 둡니다.
- `application-local.yaml` 또는 테스트 전용 프로필에서만 `mode: always`를 사용합니다.
- 운영 스키마·기준 데이터는 Flyway/Liquibase migration으로 관리합니다.

### HC-02 `[P0][확정]` 게시글 Security URL과 실제 URL이 다름

| 동작 | Security registry | 실제 Controller |
|---|---|---|
| 작성 | `POST /api/posts` | `POST /api/postCreate` |
| 삭제 | `DELETE /api/posts/{id}` | `DELETE /api/postDelete/{postId}` |

registry에 적힌 경로에는 실제 핸들러가 없고 실제 핸들러는 `anyRequest().permitAll()`로 넘어갑니다. URL을 두 곳에서 문자열로 따로 관리한 것이 직접 원인입니다.

**권장 수정**

- Controller 경로를 REST 경로로 통일합니다.
- 공개 경로만 명시하고 나머지는 인증시키는 allowlist 정책으로 바꿉니다.
- MockMvc 보안 테스트로 인증 없는 작성·삭제가 반드시 401인지 고정합니다.

### HC-03 `[P1][확정]` 운영 DB 포트 변수 오타

`application-prod.yaml`의 URL은 `${DB_PROT}`를 사용합니다. 일반적으로 의도한 변수는 `${DB_PORT}`입니다. 환경에 `DB_PROT`가 없으면 placeholder 해석 또는 DB 연결 단계에서 서버가 시작되지 않습니다.

### HC-04 `[P1][조건부 위험]` Windows 개발 PC 절대 경로가 운영에도 고정됨

공통·운영 설정 모두 다음 경로를 사용합니다.

```text
E:/msa/worksapce/msa4-meerkatgram/storage
```

- 다른 Windows PC에는 E 드라이브가 없을 수 있습니다.
- Linux에서는 의도한 절대 경로가 아니라 이상한 상대 경로처럼 취급될 수 있습니다.
- 컨테이너 재배포 시 로컬 파일이 사라질 수 있습니다.
- 여러 서버 인스턴스는 파일을 공유하지 못합니다.

`FILE_STORAGE_PATH` 환경 변수로 분리하고, 운영에서는 영속 볼륨이나 오브젝트 스토리지를 사용해야 합니다.

### HC-05 `[P1][확정]` 개발 비밀번호와 JWT 기본 secret이 소스에 존재

- DB 기본 사용자: `root`
- DB 기본 비밀번호: `msa505`
- JWT secret의 긴 기본값 존재

환경 변수가 빠져도 서버가 조용히 알려진 credential로 동작합니다. 로컬 전용 값이라도 Git에 들어간 순간 공유된 비밀로 취급해야 합니다. 기본값을 제거하고 누락 시 시작 단계에서 실패시키는 편이 안전합니다.

### HC-06 `[P1][확정]` CORS와 파일 서버 URI 변수 의미가 뒤섞임

공통 설정은 다음과 같습니다.

- `cors.allowed-origins: ${SERVER_URI:http://localhost:8080}`
- `file.server-uri: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}`

일반적인 구성에서는 CORS origin은 프론트엔드 주소 `5173`, 파일 서버 URI는 백엔드 주소 `8080`이어야 합니다. 현재 기본값과 변수명이 서로 뒤바뀐 모양입니다.

**예상 증상**

- 기본 설정에서 Vue 개발 서버의 교차 출처 요청이 CORS로 차단됩니다.
- 업로드 응답의 파일 URL이 프론트엔드 주소로 만들어져 이미지가 404가 될 수 있습니다.

### HC-07 `[P1][확정]` 운영에서도 개발용 보안·오류 공개 설정 사용

- `security.jwt.secure: false`
- `server.error.include-stacktrace: always`
- `server.error.include-message: always`
- Swagger와 `/api-docs` 비활성화 설정 없음

운영 HTTPS에서는 refresh cookie가 반드시 `Secure=true`여야 합니다. stack trace, 내부 메시지, API 탐색 문서는 공격자에게 내부 구조를 제공하므로 운영 프로필에서 끄거나 접근을 제한해야 합니다.

### HC-08 `[P2][확정]` 만료 시간이 단위 없이 숫자로만 표현됨

- access: `1000000`ms, 약 16분 40초
- refresh: `1296000000`ms, 15일
- refresh cookie: `1296000`초, 15일

JWT는 밀리초이고 Cookie는 초라서 같은 15일이 서로 다른 숫자로 보입니다. 신규 개발자가 단위를 착각하기 쉽습니다. `Duration` 타입과 `15d`, `20m` 같은 표현을 사용하거나 프로퍼티 이름에 `-millis`, `-seconds`를 명시해야 합니다.

### HC-09 `[P2][확정]` 전환 전 설정과 사용하지 않는 의존성이 남음

- 운영 YAML의 MyBatis mapper 설정은 현재 JPA 코드와 연결되지 않습니다.
- Spring AI BOM을 import하지만 실제 Spring AI 의존성은 없습니다.
- `MeerkatChatController`는 전체 구현이 주석 처리된 빈 Bean입니다.
- README 기술 스택은 아직 MyBatis로 설명합니다.

이런 잔재는 “현재 무엇이 실제로 사용되는가?”를 흐리게 만듭니다.

### HC-10 `[P2][확정]` 로그 설정 패키지가 실제 패키지와 다름

설정은 `com.msa4meerkatgram.controllers`를 가리키지만 실제 컨트롤러는 `domain.auth.controllers`, `domain.post.controllers`, `domain.file.controller` 아래 있습니다. 기대한 debug/error 레벨이 적용되지 않습니다. 또한 모든 INFO 로그를 기록하면서 파일 이름은 `error.log`라 운영자에게 오해를 줍니다.

## 2. 물리적·실행 시 오류

### RT-01 `[P1][재현 확인]` 존재하지 않는 URL이 404가 아니라 500이 됨

`GlobalExceptionHandler`의 `@ExceptionHandler(Exception.class)`가 `NoResourceFoundException`까지 잡아 `SYSTEM_ERROR/E99`로 변환합니다. 실행 중 서버에서 `GET /actuator/health`가 500으로 반환되는 것을 확인했습니다.

**영향**

- 잘못된 클라이언트 URL이 서버 장애처럼 보입니다.
- 로드밸런서 health check를 잘못 설정하면 인스턴스가 비정상으로 판정됩니다.
- 실제 500 비율과 알람이 오염됩니다.

`NoResourceFoundException`을 404로 처리하고, 실제 health endpoint가 필요하면 Actuator를 명시적으로 구성해야 합니다.

### RT-02 `[P1][확정]` 인증 없이 실제 작성·삭제 URL 호출 시 NPE 가능

현재 URL이 인증에서 빠져 있기 때문에 `@AuthenticationPrincipal Claims claims`가 null일 수 있습니다. 컨트롤러는 즉시 `claims.getSubject()`를 호출하므로 `NullPointerException`이 발생하고 E99/500으로 응답합니다.

### RT-03 `[P1][확정]` DB 롤백이 저장된 파일을 되돌리지 못함

`PostService.postCreates()`는 파일을 디스크에 먼저 저장한 뒤 Post를 DB에 저장합니다. 이후 DB constraint 오류가 나면 JPA 트랜잭션은 rollback되지만 파일 시스템은 트랜잭션 대상이 아니므로 파일이 남습니다.

반대로 게시글 soft delete 시에도 연결된 이미지 파일을 삭제하거나 참조 상태로 관리하지 않습니다. 시간이 지날수록 고아 파일과 디스크 사용량이 증가합니다.

### RT-04 `[P1][확정]` 입력 길이와 DB 길이가 맞지 않아 500 가능

| 값 | DB 제약 | API 검증 |
|---|---:|---|
| Post content | 200자 | 최대 길이 검증 없음 |
| Post image | 100자 | 생성 URL 길이 검증 없음 |
| User profile | 100자 | 최대 길이·URL 형식 검증 없음 |
| User nick | 20자 | 정규식으로 최대 20자 검증 |

긴 content/profile은 Controller에서 400으로 막히지 않고 DB 예외로 진행될 수 있습니다. Spring은 보통 SQLException 자체가 아니라 `DataIntegrityViolationException`으로 감싸므로 현재 SQLException handler가 아닌 E99/500으로 빠질 가능성이 큽니다.

### RT-05 `[P1][조건부 위험]` 파일 컨트롤러의 `@ModelAttribute MultipartFile` 바인딩이 불명확함

파일 API는 `@ModelAttribute MultipartFile file`을 사용하지만 게시글 작성은 `@RequestParam MultipartFile file`을 사용합니다. `MultipartFile` 하나를 받는 계약은 `@RequestPart("file")` 또는 `@RequestParam("file")`로 명시하는 것이 안전합니다. 현재 형태는 resolver 선택과 파라미터 이름 보존 여부에 의존해 바인딩 오류가 날 수 있습니다.

### RT-06 `[P1][확정]` refresh token이 null이면 재발급에서 NPE

```java
if (!user.getRefreshToken().equals(refreshTokenOptional))
```

DB 값이 null인 사용자는 `equals()` 호출에서 NPE가 발생합니다. `Objects.equals(stored, incoming)`을 사용하더라도 null 저장 토큰은 반드시 InvalidTokenException으로 명시 처리해야 합니다.

### RT-07 `[P2][확정]` 게시글 삭제의 정상적인 실패가 모두 500으로 변환됨

사용자 없음, 게시글 없음, 작성자 불일치에 일반 `RuntimeException`을 던집니다. 결과적으로 다음 의미가 사라집니다.

- 사용자/토큰 문제: 401
- 게시글 없음: 404
- 소유자 아님: 403

이미 `DeletedRecordException`, `PostDeleteException` 등이 있는데 실제 서비스가 사용하지 않습니다.

### RT-08 `[P2][확정]` 페이지 값이 매우 크면 정수 overflow 가능

```java
int offset = (page - 1) * limit;
```

`page`와 `limit`에 상한이 없어서 곱셈이 음수로 overflow될 수 있고 QueryDSL offset에서 예외가 발생할 수 있습니다. `Pageable`을 사용하거나 long 계산과 최대 크기 제한을 적용해야 합니다.

### RT-09 `[P2][확정]` 조회 로직이 OSIV에 숨게 의존함

실행 로그에서 `spring.jpa.open-in-view is enabled` 경고가 확인됩니다. `show()`는 명시적 read-only transaction이나 fetch join 없이 LAZY `post.user`를 DTO로 변환합니다. 현재는 OSIV 덕분에 동작할 수 있지만 운영에서 OSIV를 끄면 `LazyInitializationException`이 날 수 있습니다.

조회 서비스에 `@Transactional(readOnly = true)`를 붙이고 필요한 관계를 query에서 fetch하는 구조가 더 명확합니다.

### RT-10 `[P2][확정]` 스키마 migration이 저장소에 없음

`ddl-auto: none`인데 Flyway/Liquibase 또는 완전한 schema SQL이 없습니다. 새 환경은 테이블을 어떤 버전으로 만들어야 하는지 알 수 없고, 더미 SQL은 테이블이 이미 존재한다고 가정합니다. 애플리케이션 버전과 DB 스키마 버전을 함께 배포할 방법이 필요합니다.

### RT-11 `[P2][확정]` 의미가 불분명한 Repository 메서드

`PostRepository`의 `Long user(User user)`는 이름도 Spring Data 파생 쿼리 규칙도 의도가 불명확하고 반환형도 이상합니다. 현재 미사용이라 드러나지 않지만 호출되면 query/변환 오류가 날 가능성이 있습니다. 의도가 count라면 이미 존재하는 `countByUser`만 남겨야 합니다.

### RT-12 `[P2][확정]` 회원가입 중복 검사가 동시성에 안전하지 않음

`existsByEmail()` 확인과 `save()` 사이에 다른 요청이 같은 email을 저장할 수 있습니다. DB unique constraint는 최종 방어선으로 필요하지만, 발생한 `DataIntegrityViolationException`을 409로 변환해야 합니다.

## 3. 검증 규칙 불일치

- 이메일 TLD를 2~3자로 제한해 `.info`, `.museum` 등 정상 주소를 거부합니다.
- 닉네임 API는 영문·숫자·underscore만 허용하지만 더미 데이터에는 한글 닉네임이 있습니다.
- `PostIndexRequest` 생성자는 0/음수를 기본값으로 바꿔 `@Min` 오류를 숨깁니다.
- Controller에 `@Validated`가 없어 PathVariable의 `@Min`이 기대대로 동작하지 않을 수 있습니다.
- `PostIndexRequest`에 `@Valid`가 없고 limit 최대값도 없습니다.

검증은 “잘못된 값을 조용히 바꾸는 것”과 “400으로 거부하는 것” 중 하나를 API 계약으로 명확히 선택해야 합니다.
