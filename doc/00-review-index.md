# Meerkatgram JPA 전환 재검수 문서

> 검수 기준: `feature/v2/migration-jpa` 브랜치, 2026-07-10 소스
>
> 범위: Java 소스, Gradle 설정, YAML 설정, 더미 SQL, 기존 문서, 실행 로그, 로컬 실행 서버의 읽기 전용 API 확인

## 먼저 읽어야 할 결론

현재 프로젝트는 회원가입·로그인·게시글 조회 같은 기본 흐름을 학습하고 확장하기에는 구조가 비교적 잘 나뉘어 있습니다. 하지만 **운영 배포 가능 상태는 아닙니다.** 특히 아래 네 가지는 다른 개선보다 먼저 처리해야 합니다.

1. 기본 설정의 `spring.sql.init.mode: always`와 더미 SQL의 `DELETE`가 운영 프로필에도 합성될 수 있어, 서버 재시작 시 운영 데이터가 초기화될 위험이 있습니다.
2. Spring Security에 등록된 게시글 작성·삭제 URL과 실제 컨트롤러 URL이 달라 실제 API가 인증 보호에서 빠져 있습니다.
3. 파일 업로드 API가 공개되어 있고 파일 내용 검증 없이 SVG를 포함한 확장자만 검사하므로 저장형 XSS와 디스크 고갈 위험이 있습니다.
4. 운영 설정의 `${DB_PROT}` 오타, Windows 절대 저장 경로, `Secure=false`, stack trace 공개 등 운영 장애·정보 노출 요소가 함께 남아 있습니다.

## 문서 구성

| 문서 | 내용 | 추천 독자 |
|---|---|---|
| [01-review-method-and-architecture.md](./01-review-method-and-architecture.md) | 검수 페르소나, 동조편향 제거 방식, 현재 구조와 요청 흐름 | 전체 팀 |
| [02-hardcoding-and-runtime-review.md](./02-hardcoding-and-runtime-review.md) | 하드코딩, 설정 충돌, 물리적·실행 시 오류 가능성 | 백엔드·인프라 담당 |
| [03-logic-connection-security-review.md](./03-logic-connection-security-review.md) | 논리 구조, 계층 연결, 응답/예외 불일치, 보안 이슈 | 백엔드·보안 담당 |
| [04-service-guide-for-junior.md](./04-service-guide-for-junior.md) | 각 서비스와 코드 흐름을 3개월 차 개발자 눈높이로 설명 | 주니어 개발자 |
| [05-remediation-and-test-roadmap.md](./05-remediation-and-test-roadmap.md) | 수정 순서, 완료 조건, 필요한 자동화 테스트 | 구현 담당 |

기존 [api-response-spec.md](./api-response-spec.md)는 현재 코드와 불일치하는 부분이 있으므로, 수정 전까지는 참고 자료로만 사용해야 합니다.

## 심각도 기준

| 등급 | 의미 |
|---|---|
| `P0` | 데이터 손실, 인증 우회처럼 즉시 배포를 막아야 하는 문제 |
| `P1` | 높은 확률의 운영 장애, 중요한 보안 문제, 핵심 API 계약 오류 |
| `P2` | 유지보수·확장·성능·일관성을 훼손하는 문제 |
| `P3` | 정리하면 좋은 코드 품질 및 문서 문제 |

## 검수 한계

- 소스와 현재 실행 중인 로컬 서버는 확인했지만, 이번 작업에서 DB를 변경하는 POST/DELETE 호출은 하지 않았습니다.
- 기존 테스트는 `contextLoads()` 한 건뿐이고 테스트 결과 산출물이 없어 회귀 안정성을 증명할 수 없습니다.
- 운영 인프라, 프론트엔드 소스, 실제 배포 환경 변수는 제공되지 않았으므로 해당 부분은 코드 기반 위험 분석입니다.
- 저장소의 추적 파일은 변경되어 있지 않았고, 기존 `.claude/`, `logs/`는 사용자 소유의 미추적 파일로 보존했습니다.
