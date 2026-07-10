# API Response 정리

> 분석 대상: `feature/v2/migration-jpa` 브랜치 (2026-07-09 기준)
> 공통 응답 래퍼: [`GlobalResponse<T>`](../src/main/java/com/msa4meerkatgram/global/responses/GlobalResponse.java) — `{ code, message, data }`
> 전역 예외 처리: [`GlobalExceptionHandler`](../src/main/java/com/msa4meerkatgram/global/errors/GlobalExceptionHandler.java) (`@RestControllerAdvice`)
> 성공 코드는 컨트롤러에서 개별적으로 `"00"` 을 하드코딩하고 있음 (공통 상수 없음).

---

## 0. 공통 에러코드 테이블 (GlobalExceptionHandler 기준, 전체 API 공통 적용)

| HttpStatus | 코드 | 발생 예외 | message | 발생 조건 |
|---|---|---|---|---|
| 400 | E01 | `NotRegisteredException` | 로그인 에러 | 로그인 시 이메일/비밀번호 불일치 |
| 401 | E02 | `AuthenticationException` (Spring Security) | UNAUTHENTICATED_ERROR | 인증 필요한 API에 미인증 접근 |
| 403 | E03 | `AccessDeniedException` (Spring Security) | UNAUTHORIZED_ERROR | 권한 없는 리소스 접근 |
| 401 | E04 | `InvalidTokenException` | 토큰 이상 | 토큰 없음/유효하지 않음/불일치 |
| 400 | E05 | `InvalidPostCreateException` | 게시글 안에 내용이 비어있음 | 게시글 내용/이미지 누락 |
| 403 | E06 | `PostDeleteException` | 게시글의 권한을 확인해 주세요 | 게시글 삭제 권한 없음 (**현재 미사용** — 아래 1.3 참고) |
| 404 | E10 | `DeletedRecordException` | DELETED_RECORD_ERROR | 이미 삭제되었거나 존재하지 않는 레코드 조회 |
| 409 | E11 | `DuplicatedRecordException` | DUPLICATED_RECORD_ERROR | 이미 존재하는 레코드 (이메일 중복 등) |
| 400 | E21 | `MethodArgumentTypeMismatchException` | 요청 파라미터에 이상이 있습니다. | PathVariable/RequestParam 타입 불일치 |
| 400 | E21 | `MethodArgumentNotValidException` | 요청 파라미터에 이상이 있습니다. | `@Valid` 바디 검증 실패 (data: 필드별 메시지 리스트) |
| 500 | E40 | `FileManagedException` | 파일 업로드 실패 | 파일 저장 중 오류 |
| 500 | E80 | `SQLException` | DB 에러 | DB 처리 중 오류 |
| 500 | E99 | `Exception` (그 외 전체) | 시스템 에러 | 위에 해당하지 않는 모든 예외 (아래에서 보듯 **서비스 로직에 미완성 `RuntimeException`이 다수 남아있어 실제로는 500/E99로 새는 케이스가 많음**) |

> 참고: `@Min` 위반(`PostController.show`의 `id`)은 `ConstraintViolationException`으로 발생하는데, 이를 처리하는 `@ExceptionHandler`가 없어 **E99(500)로 처리됨** — 400이 되어야 할 케이스가 500으로 새는 버그성 이슈로 보임.

---

## 1. 인증 API (`AuthController`, `/api/*`)

### 1.1 `POST /api/login` — 로그인
- 정상: `200` / `code: "00"` / `GlobalResponse<AuthRes>` (`AuthRes{ userWithPostCountRes, accessToken }`)
- 에러:
  - `400` `E21` — `LoginRequest` 검증 실패 (이메일/비밀번호 형식, 필수값)
  - `400` `E01` — 이메일 미존재 또는 비밀번호 불일치 (`AuthService.login`)
  - `500` `E99` — 기타 (예: 예상 못한 런타임 오류)

### 1.2 `POST /api/reissue-token` — 토큰 재발급
- 정상: `200` / `code: "00"` / `GlobalResponse<AuthRes>`
- 에러:
  - `401` `E04` — 리프레시 토큰 없음 / 유저 미존재 / 저장된 토큰과 불일치 (`AuthService.reissue`, 3곳에서 동일 코드로 던짐)
  - `500` `E99` — 기타

### 1.3 `POST /api/logout` — 로그아웃 (인증 필요)
- 정상: `200` / `code: "00"` / `GlobalResponse<String>` (data 없음)
- 에러:
  - `401` `E02` — 미인증 (Security 필터에서 위임)
  - `401` `E04` — `claims.getSubject()`로 조회한 유저가 없음 (`AuthService.logout`)
  - `500` `E99` — 기타

### 1.4 `POST /api/registration` — 회원가입
- 정상: `200` / `code: "00"` / `GlobalResponse<String>` (data 없음)
- 에러:
  - `400` `E21` — `RegistrationReq` 검증 실패 (이메일/비밀번호/닉네임 형식, 비밀번호 확인 불일치 등)
  - `409` `E11` — 이미 가입된 이메일 (`AuthService.registration`)
  - `500` `E99` — 기타

---

## 2. 게시글 API (`PostController`, `/api/*`)

### 2.1 `GET /api/posts` — 게시글 목록 조회 (페이지네이션)
- 정상: `200` / `code: "00"` / `GlobalResponse<PostIndexResponse>` (`{ total, lastPage, posts: PostWithUserRes[] }`)
- 에러:
  - `500` `E99` — 현재 별도 예외처리 없음. 서비스(`PostService.index`) 내 명시적 예외 throw 없음

### 2.2 `GET /api/posts/{id}` — 게시글 상세 조회 (인증 필요)
- 정상: `200` / `code: "00"` / `GlobalResponse<PostWithUserRes>`
- 에러:
  - `401` `E02` — 미인증
  - `400` `E21` — `id` 타입 불일치 (`MethodArgumentTypeMismatchException`)
  - **`500` `E99`** — `@Min(1)` 위반 시 `ConstraintViolationException`이 미처리되어 500으로 처리됨 (버그 가능성, 원래 의도는 400으로 보임)
  - `404` `E10` — 게시글 미존재/삭제됨 (`PostService.show`, `DeletedRecordException`)

### 2.3 `POST /api/postCreate` — 게시글 작성 (인증 필요)
- 정상: `200` / `code: "00"` / `GlobalResponse<PostCreateReq>` (data는 항상 `null`로 반환 — Response 타입 설계 미스로 보임, 실제로는 body 없음)
- 에러:
  - `401` `E02` — 미인증
  - `400` `E05` — 내용(content) 또는 이미지(file) 누락 (`InvalidPostCreateException`)
  - **`500` `E99`** — 작성자 유저 미존재 시 순수 `RuntimeException("접근방법이 올바르지 않습니다.")`을 던짐 → 전용 예외 없이 일반 `Exception` 핸들러로 처리됨 (미완성 코드, 의도는 401/404로 보임)
  - `500` `E40` — 파일 저장 실패 (`FileService` → `FileManagedException`)

> 주의: `SecurityUrlRegistry`의 인증 필요 URL 목록(`AUTH_REQUIRED_POST_URLS = ["/api/logout", "/api/posts"]`)은 실제 매핑 경로(`/api/postCreate`)와 불일치함 — MyBatis→JPA 전환 중 경로가 바뀌었으나 시큐리티 화이트/블랙리스트가 갱신되지 않은 것으로 보임.

### 2.4 `DELETE /api/postDelete/{postId}` — 게시글 삭제 (인증 필요)
- 정상: `200` / `code: "00"` / `GlobalResponse<Void>` (data 없음)
- 에러:
  - `401` `E02` — 미인증
  - **`500` `E99`** — 작성자 유저 미존재, 게시글 미존재, 삭제 권한 없음 3가지 케이스 모두 순수 `RuntimeException`으로 처리되어 일반 예외 핸들러로 감 (미완성 코드). 이미 정의된 전용 예외 `DeletedRecordException`(404/E10), `PostDeleteException`(403/E06)이 있음에도 사용되지 않고 있음 — JPA 전환 작업 중 임시 상태로 보임.

---

## 3. 파일 API (`FileController`, `/api/*`)

### 3.1 `POST /api/files/profiles` — 프로필 이미지 업로드
- 정상: `200` / `code: "00"` / `GlobalResponse<FileRes>` (`{ fileUri }`)
- 에러:
  - `500` `E40` — 파일 저장 실패 (`FileManagedException`, `LocalFileManager`에서 발생 가능)
  - `500` `E99` — 기타

### 3.2 `POST /api/files/posts` — 게시글 이미지 업로드
- 정상: `200` / `code: "00"` / `GlobalResponse<FileRes>`
- 에러:
  - `500` `E40` — 파일 저장 실패
  - `500` `E99` — 기타

---

## 4. 유저 API (`UserController`, `/api`)

- 컨트롤러 클래스만 존재하고 **엔드포인트 없음** (`@RequestMapping("/api")` 만 선언된 빈 클래스). JPA 전환 작업 중 아직 이관되지 않은 것으로 보임.
- 응답 DTO는 존재: `UserRes`, `UserWithPostCountRes` (다른 도메인 응답 조합에 재사용 중, 단독 API는 없음).

---

## 5. 기타 확인 사항

- **`PostShowResponse`** (`domain/post/responses/PostShowResponse.java`) — 어떤 컨트롤러/서비스에서도 참조되지 않는 미사용 클래스 (MyBatis 시절 좋아요 기능 관련 잔재로 추정, `likeCount`/`liked` 필드 보유).
- 성공 응답의 `code`는 전 API 공통으로 `"00"` 하드코딩 (enum/상수 없음 — 오탈자 위험 존재).
- 에러코드 네이밍 규칙: `E` + 2자리 숫자. 그룹 없이 구현 순서대로 부여된 것으로 보이며(E01~E06, E10~E11, E21, E40, E80, E99), 별도의 `ErrorCode` enum 없이 `GlobalExceptionHandler` 각 메소드에 문자열 리터럴로 하드코딩되어 있음.
