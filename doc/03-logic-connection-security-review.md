# 논리 구조·연결 관계·보안 검수

## 1. 논리 구조와 연결 관계

### LG-01 `[P0][확정]` “인증 필요 API 목록”과 Controller 계약이 분리됨

Security registry와 Controller가 서로의 상수를 공유하지 않고 문자열을 각각 보유합니다. Controller 경로를 바꾸어도 컴파일 오류가 없기 때문에 이번처럼 보안 규칙만 오래된 상태로 남을 수 있습니다.

가장 안전한 방향은 다음과 같습니다.

```text
permitAll: login, registration, reissue, 공개 게시글 목록, 정적 파일
authenticated: 그 밖의 모든 요청
```

즉, 보호할 URL을 빠짐없이 나열하는 blacklist보다 공개할 URL만 좁게 나열하는 allowlist가 안전합니다.

### LG-02 `[P1][확정]` API 이름이 REST 계약과 일치하지 않음

- 목록: `GET /api/posts`
- 상세: `GET /api/posts/{id}`
- 작성: `POST /api/postCreate`
- 삭제: `DELETE /api/postDelete/{postId}`

같은 자원인데 일부는 명사 `posts`, 일부는 동사 `postCreate/postDelete`를 씁니다. 이 불일치가 Security URL 오류의 원인이기도 합니다. 다음으로 통일하는 것이 좋습니다.

| 기능 | 권장 API |
|---|---|
| 목록 | `GET /api/posts` |
| 상세 | `GET /api/posts/{id}` |
| 작성 | `POST /api/posts` |
| 삭제 | `DELETE /api/posts/{id}` |

### LG-03 `[P1][확정]` 예외 클래스, 응답 코드, 서비스 사용처가 서로 다름

| 상황 | 준비된 요소 | 실제 동작 |
|---|---|---|
| 게시글 내용/이미지 누락 | `InvalidPostCreateException` | 현재 `E21` 일반 파라미터 오류로 합쳐짐 |
| 삭제 권한 없음 | `PostDeleteException` | 서비스는 일반 RuntimeException 사용 |
| 삭제 예외 handler | 403 성격 | `UNAUTHENTICATED_ERROR`, 401로 매핑 |
| 중복 데이터 | `DUPLICATED_DATA_ERROR` | 코드는 `E11`이 아닌 `11` |
| DB 오류 | SQLException handler | Spring Data 래핑 예외는 E99 가능 |

기존 `api-response-spec.md`에는 E05/E06 등이 적혀 있지만 현재 `CustomResponseCode`에는 존재하지 않습니다. 문서, enum, handler, service 중 어느 것이 진짜 계약인지 결정해야 합니다.

### LG-04 `[P1][확정]` 성공 응답 타입과 실제 데이터가 다름

게시글 작성 컨트롤러는 `GlobalResponse<PostCreateReq>`를 선언하지만 실제로는 항상 `null`을 반환합니다. 클라이언트와 OpenAPI 문서는 생성된 게시글 데이터가 올 것으로 오해할 수 있습니다.

선택지는 둘 중 하나입니다.

- 생성된 ID/게시글 DTO를 반환하면서 `201 Created` 사용
- body가 필요 없다면 `GlobalResponse<Void>` 또는 `204 No Content` 사용

### LG-05 `[P2][확정]` User 도메인은 외형만 있고 기능 연결이 없음

- `UserController`: endpoint 없음
- `UserService`: JwtProvider만 주입받고 메서드 없음
- User용 repository는 `AuthRepository`, `UserRepository` 두 개로 나뉨

도메인 경계를 나눈 것은 좋지만 현재는 빈 계층과 중복 repository가 탐색 비용만 늘립니다. 곧 구현할 기능이면 TODO와 설계 목적을 남기고, 그렇지 않다면 실제 기능이 생길 때 추가하는 편이 낫습니다.

### LG-06 `[P2][확정]` Entity가 모든 setter를 열어 불변 조건을 보장하지 못함

`User`, `Post`에 `@Setter`가 클래스 전체로 적용되어 어느 서비스에서든 email, role, user 관계 등을 임의 변경할 수 있습니다. 특히 role 변경이나 게시글 작성자 변경 같은 민감한 상태는 의미 있는 메서드로 제한해야 합니다.

예:

```java
user.rotateRefreshToken(token);
post.changeContent(validatedContent);
post.belongsTo(userId);
```

### LG-07 `[P2][확정]` 파일과 게시글의 생명주기가 분리됨

파일 API로 먼저 업로드한 파일과 게시글 작성 안에서 업로드한 파일이 모두 같은 저장소에 쌓이지만, 어떤 파일이 어느 게시글에 연결되었는지 별도 metadata가 없습니다. 임시 파일 만료, 참조 여부, 삭제 보상 정책을 구현하기 어렵습니다.

### LG-08 `[P2][조건부 위험]` offset pagination은 새 글 유입 시 중복·누락 가능

정렬 자체는 `createdAt DESC, id ASC`로 결정적이지만 사용자가 다음 페이지를 요청하기 전에 새 게시글이 추가되면 offset이 밀려 같은 글이 다시 보이거나 일부가 건너뛰어질 수 있습니다. 작은 서비스에서는 허용 가능하지만 무한 스크롤이 중요하면 cursor pagination을 고려합니다.

### LG-09 `[P3][확정]` 미사용 코드가 실제 기능처럼 남음

- `PostShowResponse`
- `PostCreateReq`의 builder와 응답 타입
- `GlobalErrorResponse`
- `MeerkatChatController`
- `UserService`
- `PostRepository.user()`

미사용 코드는 JPA 전환 완료 범위를 흐리므로 제거하거나 migration TODO로 명시해야 합니다.

## 2. 보안 검수

### SEC-01 `[P0][확정]` 게시글 작성·삭제 인증 규칙 누락

가장 직접적인 권한 문제입니다. 실제 API가 `permitAll`이고 메서드 수준 보안도 없습니다. 현재는 null Claims 때문에 500이 날 가능성이 크지만, 향후 principal 처리 방식이 바뀌면 인증 우회가 실제 데이터 변경으로 이어질 수 있습니다.

### SEC-02 `[P1][확정]` 파일 업로드 API가 인증 없이 공개됨

`POST /api/files/profiles`, `POST /api/files/posts`는 registry에 없습니다. 공격자는 계정 없이 반복 업로드해 저장 공간과 네트워크를 소비할 수 있습니다. 사용자별 quota, rate limit, 인증, 임시 파일 정리 정책이 필요합니다.

### SEC-03 `[P1][확정]` 파일 이름 확장자만 검사하고 SVG를 허용함

검증 코드는 원본 파일명의 마지막 확장자를 꺼내 `image/` 문자열을 붙여 allowlist와 비교합니다. 실제 byte signature, MIME, 이미지 decode 여부는 확인하지 않습니다.

특히 SVG는 XML/스크립트·외부 참조 같은 active content를 포함할 수 있고 동일 백엔드 origin의 `/files/**`로 제공됩니다. 브라우저 처리 방식과 삽입 방식에 따라 저장형 XSS 또는 추적 문제가 생길 수 있습니다.

**권장 방어**

- SVG 업로드 금지 또는 전문 sanitizer 사용
- magic number와 실제 이미지 decode 검증
- 허용 포맷을 JPEG/PNG/WebP처럼 좁힘
- 업로드 파일을 별도 도메인/object storage에서 `Content-Disposition`, CSP와 함께 제공
- 이미지 재인코딩으로 metadata와 비정상 payload 제거

### SEC-04 `[P1][확정]` Access token과 Refresh token을 구분하지 않음

두 토큰은 같은 secret, 같은 claim 구조를 사용하고 만료 시간만 다릅니다. 인증 필터는 서명과 만료만 확인하므로 유출된 refresh token을 Authorization Bearer 값으로 넣어도 access token처럼 인증될 수 있습니다.

`token_use: access|refresh` 또는 별도 audience를 넣고 각 처리 지점에서 예상 종류를 강제해야 합니다. 가능하면 key 또는 검증 정책도 분리합니다.

### SEC-05 `[P1][확정]` JWT issuer를 발급하지만 검증하지 않음

토큰 생성 시 issuer를 넣지만 parser는 `verifyWith(secretKey)`만 수행합니다. 같은 key로 생성된 다른 용도의 JWT도 받아들일 수 있습니다. issuer, audience, token_use를 모두 검증해야 합니다.

### SEC-06 `[P1][확정]` 운영 refresh cookie가 HTTPS 전용이 아님

운영 YAML도 `secure: false`입니다. HTTPS 서비스에서 Secure가 없으면 구성 실수나 HTTP 접근 시 cookie가 평문 채널로 전달될 위험이 있습니다. `SameSite`도 명시하지 않아 프론트·백엔드 배치 방식에 따라 재발급 요청에서 쿠키가 누락되거나 CSRF 정책이 불명확해집니다.

권장 속성:

- `HttpOnly=true`
- `Secure=true` in production
- `SameSite=Lax/Strict` 또는 cross-site가 필요하면 `None; Secure`
- 필요한 최소 Path
- 프론트엔드 배치에 맞춘 명시적 Domain 정책

`jakarta.servlet.http.Cookie`만으로 SameSite 설정이 부족하면 `ResponseCookie`를 사용하는 방법이 있습니다.

### SEC-07 `[P1][확정]` refresh token을 DB와 더미 SQL에 원문 저장

DB 유출 시 활성 refresh token을 바로 재사용할 수 있습니다. 비밀번호처럼 token 자체 대신 SHA-256 같은 hash를 저장하고 받은 token을 hash해 비교하는 방법이 안전합니다. 더미 SQL에 과거 JWT가 들어간 것도 제거하고, 사용한 key가 실제 환경과 겹쳤다면 회전해야 합니다.

### SEC-08 `[P1][확정]` JWT role claim을 권한으로 사용하지 않음

토큰에는 role을 넣지만 `SecurityAuthenticationProvider`는 authorities에 빈 리스트를 넣습니다. 따라서 `NORMAL`, `SUPER` 구분이 인증 객체에 반영되지 않습니다. 현재 role 보호 API가 없어 즉시 권한 상승은 아니지만, 관리자 API를 추가하면서 `authenticated()`만 사용하면 모든 로그인 사용자가 접근할 수 있습니다.

### SEC-09 `[P1][확정]` 파일·프로필 URL과 사용자 email을 공개 게시글 응답에 포함

공개 게시글 목록의 `UserRes`에는 email과 role까지 포함됩니다. UI에 필요하지 않다면 개인정보와 내부 권한 정보의 불필요한 노출입니다. 공개 작성자 DTO는 `id`, `nick`, `profile` 정도로 최소화해야 합니다.

### SEC-10 `[P1][조건부 위험]` 탈퇴·삭제 사용자 토큰이 만료 전까지 인증됨

JWT 필터는 DB에서 사용자 상태를 확인하지 않고 Claims만 principal로 사용합니다. 소프트 삭제된 사용자라도 기존 access token은 만료까지 protected GET에 인증될 수 있습니다. 민감 API는 DB 상태 확인, token version, 계정 상태 claim/캐시 등을 고려해야 합니다.

### SEC-11 `[P2][확정]` 로그아웃 후 access token은 계속 유효

로그아웃은 refresh token만 null로 만들고 access token은 폐기하지 않습니다. 짧은 access TTL을 사용하는 일반적인 설계일 수 있지만 “로그아웃 즉시 모든 접근 차단”이 제품 요구라면 token denylist 또는 user token version이 필요합니다. 현재 TTL은 약 16분 40초입니다.

### SEC-12 `[P2][확정]` 인증 실패 stack trace를 debug 로그에 반복 기록

정상적으로 자주 발생할 수 있는 401도 전체 stack trace를 기록합니다. 공격자가 반복 요청하면 로그가 빠르게 증가하고 중요한 오류가 묻힐 수 있습니다. 예상 가능한 4xx는 요청 ID, 경로, 오류 코드 정도만 구조화해 남기고 stack trace는 예상 밖 5xx 중심으로 기록해야 합니다.

### SEC-13 `[P2][확정]` 상세 오류·Swagger가 운영에서 노출될 수 있음

운영 stack trace/message와 Swagger/API docs를 그대로 열어 두면 패키지명, 내부 API, DTO 구조, DB 관련 메시지가 노출될 수 있습니다. 운영에서 비활성화하거나 관리자 네트워크·인증 뒤에 둡니다.

### SEC-14 `[P2][조건부 위험]` CSRF 정책과 refresh cookie 사용 방식의 설명이 부족함

CSRF는 완전히 꺼져 있습니다. access token만 Authorization header로 보내는 API는 CSRF 위험이 낮지만 refresh endpoint는 cookie 인증을 사용합니다. SameSite와 origin 검증을 명확히 하지 않으면 cross-site 요청 정책이 모호합니다. 최소한 reissue/logout에 Origin 검증 또는 CSRF 보호 여부를 설계 문서에 명시해야 합니다.

## 3. 응답 코드 일관성 표

| 코드 경로 | 현재 상태 | 권장 상태 |
|---|---|---|
| 성공 | `00 / SUCCESS` | 유지 가능 |
| 로그인 실패 | 401 E01 | 제품 계약에 맞춰 401 유지 가능 |
| 미인증 | 401 E02 | 유지 |
| 권한 없음 | 403 E03 | 삭제 권한 오류도 여기에 연결 |
| 잘못된 토큰 | 401 E04 | 유지 |
| 존재하지 않는 데이터 | 404 E10 | 유지 |
| 중복 데이터 | 409 `11` | `E11`로 수정 |
| 파라미터 오류 | 400 E21 | field별 오류 data 추가 권장 |
| 파일 형식/크기 오류 | 현재 500 E40 | 사용자 입력이면 400/415 권장 |
| 파일 저장 I/O 실패 | 500 E40 | 유지 가능 |
| 알 수 없는 URL | 현재 500 E99 | 404로 수정 |
| 예상 못한 오류 | 500 E99 | 내부 상세 숨기고 requestId 제공 |
