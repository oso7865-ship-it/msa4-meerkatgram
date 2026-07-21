# 3개월 차 개발자를 위한 Meerkatgram 서비스 코드 설명

> 설명 페르소나: 15년 차 풀스택 개발자 강사
>
> 목표: 코드를 외우는 것이 아니라, HTTP 요청 한 건이 어디를 지나고 누가 어떤 책임을 갖는지 이해하기

## 1. 제일 먼저 잡아야 하는 큰 그림

웹 백엔드는 식당으로 비유하면 이해가 쉽습니다.

- **Controller는 주문받는 직원**입니다. URL과 HTTP method를 보고 주문을 받습니다.
- **Service는 주방장**입니다. “누가 주문했나?”, “재료가 맞나?”, “어떤 순서로 처리하나?” 같은 업무 규칙을 실행합니다.
- **Repository는 창고 담당자**입니다. DB에서 데이터를 꺼내고 저장합니다.
- **Entity는 DB에 보관되는 원본 장부**입니다.
- **Request/Response DTO는 주문서와 영수증**입니다. 외부에 필요한 값만 담습니다.
- **Security Filter는 출입구 경비원**입니다. Controller에 도착하기 전에 토큰을 확인합니다.
- **GlobalExceptionHandler는 고객 응대 담당자**입니다. 내부 예외를 일정한 오류 응답으로 바꿉니다.

이 역할이 섞이면 한 파일을 고쳤는데 보안, DB, 응답 형식이 함께 깨집니다. 현재 프로젝트는 기본 폴더 구분은 잘 되어 있지만 URL 계약과 예외 계약이 서로 어긋난 부분이 있습니다.

## 2. 애플리케이션 시작점

### `Msa4MeerkatgramApplication`

이 클래스의 `main()`이 서버 전원을 켜는 스위치입니다.

- `@SpringBootApplication`: Component, Service, Repository, Configuration을 찾아 Spring Bean으로 등록합니다.
- `@ConfigurationPropertiesScan`: YAML의 `security.jwt`, `cors`, `file` 값을 record 설정 객체로 묶습니다.
- `@EnableJpaAuditing`: Entity가 저장·수정될 때 `createdAt`, `updatedAt`을 자동 입력할 준비를 합니다.

여기서 중요한 점은 서버가 켜질 때 DB 연결과 SQL 초기화도 일어난다는 것입니다. 현재 `mode: always` 때문에 “서버를 켠다”가 “더미 데이터로 DB를 다시 채운다”까지 포함할 수 있습니다.

## 3. 인증 서비스

### 3.1 로그인: `POST /api/login`

흐름은 다음과 같습니다.

1. `AuthController.login()`이 JSON을 `LoginRequest`로 받습니다.
2. `@Valid`가 email/password 형식을 검사합니다.
3. `AuthService.login()`이 email로 User를 조회합니다.
4. `PasswordEncoder.matches()`가 입력 비밀번호와 BCrypt hash를 비교합니다.
5. 성공하면 access token과 refresh token을 만듭니다.
6. refresh token은 User row와 HttpOnly cookie에 저장합니다.
7. access token과 사용자 정보를 JSON 응답으로 보냅니다.

비밀번호를 `equals()`로 비교하지 않는 이유는 DB에 실제 비밀번호가 아니라 BCrypt hash가 저장되기 때문입니다. BCrypt는 같은 비밀번호도 매번 다른 hash가 나올 수 있으므로 반드시 `matches(raw, encoded)`를 사용합니다.

#### 현재 주의점

- 로그인도 refresh token을 DB에 저장하므로 읽기 전용 메서드가 아닙니다.
- 응답 사용자 DTO에 email과 role이 들어갑니다. 로그인 응답에서는 필요할 수 있지만 공개 게시글 작성자 DTO와 재사용하면 개인정보가 노출됩니다.
- 같은 사용자가 여러 기기에서 로그인하면 DB에 refresh token 하나만 저장하므로 이전 기기의 재발급이 막힙니다. 단일 세션 정책인지 의도 확인이 필요합니다.

### 3.2 토큰 생성: `JwtProvider`

JWT는 서버가 서명한 출입증입니다.

- `subject`: 사용자 ID
- `issuer`: 발급자
- `expiration`: 만료 시각
- `role`: 사용자 역할
- `signature`: 서버 secret으로 만든 위조 방지 서명

Access token은 짧게 쓰는 일일 출입증, Refresh token은 새 일일 출입증을 발급받는 장기 증명서라고 생각하면 됩니다.

현재 두 토큰의 내용 구조가 같아서 경비원이 둘을 구분하지 못합니다. `token_use=access` 또는 `token_use=refresh` 도장을 넣고, 출입구에서는 access만, 재발급 창구에서는 refresh만 받게 해야 합니다.

### 3.3 인증 필터: `TokenAuthenticationFilter`

이 필터는 Controller보다 먼저 실행됩니다.

1. Authorization header를 찾습니다.
2. `Bearer` 뒤의 JWT를 꺼냅니다.
3. `JwtProvider.extractClaims()`로 서명과 만료를 검사합니다.
4. Claims를 `Authentication` principal로 SecurityContext에 넣습니다.
5. Controller는 `@AuthenticationPrincipal Claims`로 사용자 ID를 받습니다.

토큰이 없다고 필터 자체가 항상 401을 만들지는 않습니다. Security 설정에서 해당 URL이 `authenticated()`일 때만 뒤 단계가 401을 만듭니다. 그래서 Security URL이 실제 Controller URL과 다르면 인증이 빠지는 것입니다.

### 3.4 재발급: `POST /api/reissue-token`

1. cookie에서 refresh token을 꺼냅니다.
2. JWT subject에서 User ID를 얻습니다.
3. DB User의 refresh token과 cookie token이 같은지 확인합니다.
4. access/refresh token을 모두 새로 만들고 DB/cookie를 교체합니다.

이것을 refresh token rotation이라고 볼 수 있습니다. 다만 현재 DB 값이 null이면 NPE가 나고, refresh token을 원문 저장하며, token 종류 검증이 없는 문제가 있습니다.

### 3.5 로그아웃: `POST /api/logout`

- DB의 refresh token을 null로 바꿉니다.
- 같은 cookie path에 max-age 0 cookie를 보내 브라우저 cookie를 삭제합니다.

이미 발급된 access token은 만료까지 살아 있습니다. 이것은 stateless JWT의 흔한 특성이지만 제품에서 “즉시 로그아웃”을 요구하면 별도 차단 정책이 필요합니다.

### 3.6 회원가입: `POST /api/registration`

1. Request DTO가 email, password, passwordChk, nick, profile을 검증합니다.
2. email 존재 여부를 확인합니다.
3. 비밀번호를 BCrypt hash로 변환합니다.
4. provider와 role 기본값을 넣고 User를 저장합니다.

`exists` 확인은 친절한 에러를 위한 사전 검사일 뿐입니다. 동시에 같은 email 요청이 들어올 수 있으므로 DB unique constraint가 최종 방어선이고, 그 예외도 409로 변환해야 합니다.

## 4. 게시글 서비스

### 4.1 목록: `GET /api/posts`

`PostService.index()`는 page와 limit으로 offset을 계산한 뒤 QueryDSL repository를 호출합니다.

```text
page=1, limit=6 → offset=0
page=2, limit=6 → offset=6
```

`PostQueryRepository.pagination()`은 Post와 User를 fetch join합니다. Post의 user 관계는 LAZY이므로 그냥 게시글 6개를 가져온 뒤 사용자 정보를 하나씩 읽으면 추가 query가 최대 6번 발생할 수 있습니다. fetch join은 처음 query에서 사용자까지 같이 가져와 이 N+1 문제를 줄입니다.

그다음 전체 게시글 수를 세고 `lastPage`를 계산해 `PostIndexResponse`를 만듭니다.

#### 현재 주의점

- limit 최대값이 없어서 큰 요청이 가능합니다.
- page × limit이 int 범위를 넘을 수 있습니다.
- offset pagination은 중간에 새 글이 들어오면 다음 페이지에서 중복/누락이 생길 수 있습니다.
- 공개 응답의 UserRes에 email과 role까지 포함됩니다.

### 4.2 상세: `GET /api/posts/{id}`

1. Repository `findById()`로 게시글을 찾습니다.
2. 없거나 soft delete된 글이면 `DeletedRecordException`을 던집니다.
3. Entity를 `PostWithUserRes` DTO로 바꿉니다.

Post의 user는 LAZY 관계입니다. 현재는 OSIV가 켜져 있어 DTO 변환 때 user 조회가 가능하지만 이 설정을 끄면 서비스의 transaction 밖에서 LAZY 로딩 오류가 날 수 있습니다. 조회 메서드에 `@Transactional(readOnly=true)`를 붙이고 상세 query에서도 user를 fetch하는 편이 안정적입니다.

### 4.3 작성: 현재 `POST /api/postCreate`

1. Claims에서 User ID를 꺼냅니다.
2. User가 존재하는지 확인합니다.
3. content가 비어 있지 않은지 확인합니다.
4. file이 비어 있지 않은지 확인합니다.
5. 파일을 디스크에 저장합니다.
6. 파일 URL을 Post image에 넣고 DB에 저장합니다.

여기서 꼭 기억할 점은 `@Transactional`이 **DB 작업만** 되돌린다는 것입니다. 5번 파일 저장 뒤 6번 DB 저장이 실패하면 파일은 자동 삭제되지 않습니다. 보상 삭제를 직접 구현하거나 임시 파일 → DB 성공 → 확정 파일의 단계를 설계해야 합니다.

또한 실제 작성 URL은 Security registry와 달라 인증 보호가 빠져 있습니다. 먼저 URL을 `POST /api/posts`로 통일해야 합니다.

### 4.4 삭제: 현재 `DELETE /api/postDelete/{postId}`

1. Claims의 User ID로 사용자 확인
2. 게시글 확인
3. 게시글 작성자 ID와 로그인 User ID 비교
4. `postRepository.delete(post)` 호출

Post Entity에 `@SQLDelete`가 있으므로 실제 DELETE가 아니라 `deleted_at=NOW()` UPDATE가 실행됩니다. `@SQLRestriction` 때문에 이후 일반 조회에서는 숨겨집니다. 이것이 soft delete입니다.

현재 문제는 실패 이유를 모두 RuntimeException으로 던진다는 것입니다. 서버가 예상한 “없는 글”, “내 글이 아님”은 시스템 장애가 아니므로 각각 404와 403으로 구분해야 합니다.

## 5. 파일 서비스

### `FileService`와 `LocalFileManager`

`FileService`는 profile용인지 post용인지 경로 종류를 정하고, 실제 파일명 생성과 디스크 쓰기는 `LocalFileManager`가 담당합니다.

현재 파일명은 다음처럼 만들어집니다.

```text
20260710_UUID.png
```

원본 파일명을 그대로 쓰지 않기 때문에 같은 이름 충돌과 단순 path traversal 위험을 줄인 점은 좋습니다.

하지만 검사하는 것은 원본 이름의 확장자뿐입니다. `attack.png`라는 이름의 파일 내용이 실제 PNG인지 확인하지 않습니다. 또 SVG는 단순 그림 파일이 아니라 active content가 될 수 있습니다. 운영 업로드는 다음 순서가 안전합니다.

1. 인증·사용량 제한 확인
2. 크기 제한 확인
3. 확장자와 실제 MIME/magic number 확인
4. 이미지 decoder로 열 수 있는지 확인
5. 안전한 포맷으로 재인코딩
6. 별도 저장 도메인에 저장
7. DB 저장 실패 시 파일 삭제

### `WebConfig`

디스크의 `storage/files`를 브라우저 URL `/files/**`로 연결합니다. 쉽게 말해 “이 URL로 요청하면 이 폴더의 파일을 읽어 줘”라는 매핑입니다.

운영 서버가 여러 대면 각 서버 폴더 내용이 다르기 때문에 어떤 요청은 이미지가 보이고 어떤 요청은 404가 될 수 있습니다. 이때 공유 object storage가 필요합니다.

## 6. User 도메인

현재 `UserController`와 `UserService`는 실질적인 기능이 없습니다. User Entity와 Response DTO는 Auth/Post가 사용하고 있습니다.

초급 개발자가 흔히 “레이어를 미리 다 만들어야 좋은 구조”라고 생각하지만, 빈 Controller와 빈 Service는 기능을 설명하지 못하고 탐색만 어렵게 합니다. 곧 구현할 프로필 조회/수정 기능이 없다면 제거하고 필요할 때 추가해도 됩니다.

## 7. Entity와 Repository

### User Entity

- email, password, nick, provider, role, profile, refreshToken을 가집니다.
- createdAt/updatedAt은 JPA Auditing으로 채웁니다.
- delete는 실제 row 삭제 대신 deletedAt을 기록합니다.

### Post Entity

- content, image, 작성 시각, 수정 시각을 가집니다.
- `ManyToOne`으로 User 한 명에 연결됩니다.
- 여러 Post가 한 User를 가리키는 구조입니다.

### Repository가 두 종류인 이유

- `JpaRepository`: findById, save, delete, count 같은 기본 CRUD를 자동 제공합니다.
- `PostQueryRepository`: fetch join, 정렬, pagination처럼 복잡한 query를 QueryDSL로 직접 만듭니다.

현재 User Entity를 다루는 `AuthRepository`와 `UserRepository`가 따로 있는데 둘 다 JpaRepository입니다. 도메인별 이름을 분리하려는 의도는 이해되지만 같은 aggregate 저장소가 중복돼 혼란을 줍니다. 하나의 UserRepository로 합치고 서비스가 필요한 method를 공유하는 쪽이 단순합니다.

## 8. 공통 응답과 예외 처리

성공 응답은 다음 형태입니다.

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {}
}
```

Service가 예외를 던지면 `GlobalExceptionHandler`가 HTTP status와 공통 응답으로 바꿉니다. 좋은 예외 처리는 단순히 모든 오류를 500으로 만드는 것이 아닙니다.

- 사용자가 잘못 입력함 → 400
- 로그인하지 않음 → 401
- 로그인했지만 권한 없음 → 403
- 데이터 없음 → 404
- 중복 → 409
- 서버가 예상하지 못한 실패 → 500

현재 일반 RuntimeException과 catch-all handler 때문에 예상 가능한 실패도 500이 됩니다. 전용 예외를 실제 서비스에서 사용하고 Spring Data 예외도 의미에 맞게 변환해야 합니다.

## 9. 코드를 수정할 때 따라갈 체크 순서

예를 들어 게시글 작성 URL을 바꾼다면 한 파일만 보고 끝내지 마세요.

1. Controller mapping
2. Security permit/authenticated 규칙
3. 프론트엔드 호출 URL
4. Swagger/OpenAPI 문서
5. 테스트 URL
6. 기존 Markdown API 문서

이 프로젝트의 큰 오류들은 대부분 같은 계약을 여러 곳에서 따로 관리하다가 한쪽만 바뀌어서 생겼습니다. “연결된 곳을 같이 찾는 습관”이 가장 중요한 개선입니다.
