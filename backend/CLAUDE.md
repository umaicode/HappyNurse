# HappyNurse Backend 개발 컨벤션

> Stack: Spring Boot 3.5.13 · Java 17 · PostgreSQL · JPA/Hibernate · Spring Security + JWT

---

## 목차

1. [기술 스택](#1-기술-스택)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [TDD & 단위 테스트](#3-tdd--단위-테스트)
4. [에러 코드 & 예외 처리](#4-에러-코드--예외-처리)
5. [API 응답 형식](#5-api-응답-형식)
6. [Swagger 문서화](#6-swagger-문서화)
7. [보안 규칙](#7-보안-규칙)
8. [Git 컨벤션](#8-git-컨벤션)

---

## 1. 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.5.13 |
| Language | Java 17 |
| DB | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| 인증 | Spring Security + JWT (JJWT 0.12.6) |
| API 문서 | SpringDoc OpenAPI (springdoc-openapi-starter-webmvc-ui) |
| 빌드 | Gradle |
| 테스트 | JUnit5 · Mockito · AssertJ · Spring Security Test |
| 설정 파일 | `application.yml` |

---

## 2. 프로젝트 구조

```
src/main/java/com/ssafy/happynurse/
├── domain/
│   ├── auth/          # 인증 (로그인, 토큰, 세션)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── common/        # 공통 엔티티 (Practitioner, PractitionerRole)
│   ├── patient/       # 환자, 입원, 기관, 병동
│   ├── nurse/         # 간호 기록, 투약 기록, 알림
│   ├── doctor/        # 투약 오더
│   ├── watch/         # NFC, 수액, 워치
│   ├── webapp/        # 환자 자가 보고
│   └── handover/      # 인계
└── global/
    ├── config/        # SecurityConfig, SwaggerConfig
    ├── exception/     # ErrorCode, CustomException, GlobalExceptionHandler
    ├── response/      # ApiResponse
    └── security/      # JWT, CookieUtil, Filter, EntryPoint
```

---

## 3. TDD & 단위 테스트

### 원칙

**Red → Green → Refactor**

1. 실패하는 테스트 먼저 작성
2. 테스트 통과하는 최소한의 코드 작성
3. 리팩터링 (테스트 통과 유지)

> 새 기능 추가 전 반드시 테스트 먼저 작성

### 테스트 메서드명

```
메서드명_한글_시나리오() + @DisplayName("한글 설명")
```

```java
@Test
@DisplayName("로그인 성공 시 AuthResult를 반환한다")
void login_성공() { ... }

@Test
@DisplayName("존재하지 않는 사원번호 → INVALID_CREDENTIALS")
void login_실패_존재하지_않는_사원번호() { ... }

@Test
@DisplayName("이미 확정된 간호 기록 → NURSING_ALREADY_CONFIRMED")
void confirm_실패_이미_확정됨() { ... }
```

### 서비스 단위 테스트

`@ExtendWith(MockitoExtension.class)` + BDDMockito + AssertJ

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock PractitionerRepository practitionerRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("로그인 성공 시 AuthResult를 반환한다")
    void login_성공() {
        // Given
        given(practitionerRepository.findByEmployeeNumber("EMP001"))
                .willReturn(Optional.of(practitioner));
        given(passwordEncoder.matches("password", "hashedPw"))
                .willReturn(true);

        // When
        AuthResult result = authService.login("EMP001", "password", "127.0.0.1", 1L, 3L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("mock-jwt-token");
        verify(sessionLogRepository).save(any(SessionLog.class));
    }
}
```

### 컨트롤러 테스트

`@WebMvcTest` + `@MockitoBean` + MockMvc

```java
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "jwt.cookie-name=ACCESS_TOKEN",
    "jwt.access-token-expiration-ms=1800000",
    // ...
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login - 로그인 성공")
    void login_성공() throws Exception {
        given(authService.login(anyString(), anyString(), anyString(), anyLong(), anyLong()))
                .willReturn(authResult);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

### 엔티티 테스트

순수 단위 테스트 (프레임워크 의존 없음)

```java
class RefreshTokenTest {

    @Test
    @DisplayName("create 시 usable 상태이다")
    void create_정상() {
        RefreshToken token = RefreshToken.create("session-1", practitioner, 604800000L, 1L, 3L, "nurse");
        assertThat(token.isUsable()).isTrue();
    }
}
```

### 커버리지 기준

```bash
./gradlew test
```

| 레이어 | 최소 커버리지 |
|--------|-------------|
| Service | **90% 이상** |
| 전체 | 70% 이상 |

---

## 4. 에러 코드 & 예외 처리

### ErrorCode enum

`global/exception/ErrorCode.java`

도메인별로 분리한다. HttpStatus와 메시지를 함께 정의.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ResponseCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "사원번호 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),

    // Business
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다.");

    private final HttpStatus status;
    private final String message;
}
```

### 에러 코드 네이밍 가이드

| 도메인 | 접두사 예시 |
|--------|-----------|
| 인증/인가 | `UNAUTHORIZED`, `FORBIDDEN`, `INVALID_CREDENTIALS`, `TOKEN_*` |
| 환자 | `PATIENT_NOT_FOUND`, `PATIENT_DUPLICATE` |
| 입원 | `ENCOUNTER_NOT_FOUND` |
| 간호 기록 | `NURSING_NOT_FOUND`, `NURSING_ALREADY_CONFIRMED` |
| 투약/오더 | `MEDICATION_NOT_FOUND` |
| 수액 | `IV_NOT_FOUND`, `IV_ALREADY_ENDED` |
| NFC | `NFC_TAG_NOT_FOUND`, `NFC_TAG_DISABLED` |
| 공통 | `INVALID_INPUT_VALUE`, `RESOURCE_NOT_FOUND`, `INTERNAL_SERVER_ERROR` |

### 커스텀 예외

```java
// global/exception/CustomException.java
@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String detail;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }
}
```

### 글로벌 예외 핸들러

```java
// global/exception/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e, HttpServletRequest request) {
        log.warn("CustomException: {} | detail: {} | URI: {}", e.getMessage(), e.getDetail(), request.getRequestURI());
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ApiResponse.fail(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(...) { ... }
}
```

---

## 5. API 응답 형식

모든 응답은 `ApiResponse<T>`를 따른다. (`global/response/ApiResponse.java`)

**성공**
```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": { ... }
}
```

**실패**
```json
{
  "success": false,
  "message": "사원번호 또는 비밀번호가 올바르지 않습니다.",
  "errorCode": "INVALID_CREDENTIALS"
}
```

> `null` 필드는 JSON에서 제외됨 (`@JsonInclude(NON_NULL)`)

### 사용법

```java
// 성공
ApiResponse.ok(data)
ApiResponse.ok("로그인에 성공했습니다.", loginResponse)

// 실패
ApiResponse.fail(ErrorCode.INVALID_CREDENTIALS)
```

### HTTP 상태 코드

| 상황 | 코드 |
|------|------|
| 조회 성공 | `200` |
| 생성 성공 | `201` |
| 리소스 없음 | `404` |
| 인증 실패 | `401` |
| 권한 없음 | `403` |
| 중복/충돌 | `409` |
| 유효성 실패 | `422` |
| 서버 오류 | `500` |

---

## 6. Swagger 문서화

### 기본 설정

`application.yml`에서 경로 설정:

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /api-docs
```

### 라우터 태그

모든 컨트롤러에 `@Tag` 지정.

```java
@Tag(name = "인증", description = "로그인, 로그아웃, 토큰 갱신 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController { ... }
```

| 태그명 | 도메인 |
|--------|--------|
| `인증` | 로그인, 토큰 |
| `환자` | patient |
| `입원` | encounter |
| `간호 기록` | nursing_record |
| `투약 오더` | medication_order |
| `투약 기록` | medication_administration |
| `수액 관리` | iv_infusion |
| `환자 요청` | patient_self_report |
| `NFC` | nfc_tag |
| `알림` | notification |
| `인계` | shift_handover |

### 엔드포인트 문서화

모든 엔드포인트에 `@Operation`, `@ApiResponses` 명시.

```java
@Operation(summary = "로그인", description = "사원번호와 비밀번호로 로그인합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그인 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 오류"),
    @ApiResponse(responseCode = "401", description = "자격증명 오류 — INVALID_CREDENTIALS")
})
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(...) { ... }
```

### DTO 스키마

프론트가 바로 테스트할 수 있도록 `@Schema` 필수.

```java
public record LoginRequest(
    @Schema(description = "사원번호", example = "EMP001")
    @NotBlank String employeeNumber,

    @Schema(description = "비밀번호", example = "password123")
    @NotBlank String password,

    @Schema(description = "기관 ID", example = "1")
    @NotNull Long organizationId,

    @Schema(description = "병동 ID", example = "3")
    @NotNull Long wardId
) {}
```

---

## 7. 보안 규칙

- URL에 `practitioner_id` 노출 금지 → JWT 토큰에서 추출
- 다른 병동 데이터 접근 시 `403` 반환 (IDOR 방지)
- 비밀번호는 bcrypt 해싱 필수 (`passwordHash`)
- 로그인/로그아웃은 `SessionLog`에 IP 포함 기록
- JWT는 HttpOnly 쿠키로 전달 (`ACCESS_TOKEN`, `REFRESH_TOKEN`)

```java
// 금지
@GetMapping("/nursing-records/{practitionerId}/list")

// 올바른 방식
@GetMapping("/nursing-records/my")
public ResponseEntity<?> getMyRecords(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long practitionerId = userDetails.getPractitionerId();
}
```

---

## 8. Git 컨벤션

### 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `master` | 배포 브랜치 |
| `develop` | 기본(default) 브랜치 |

### 브랜치 네이밍

```
직무-플랫폼/유형-설명-지라번호
```

**예시:** `be-web/feat-로그인-구현-S14P31E101-206`

#### 직무

| 카테고리 | 의미 |
|----------|------|
| `be` | 백엔드 |
| `fe` | 프론트엔드 |
| `infra` | 인프라 |
| `ai` | AI |

#### 플랫폼

| 플랫폼 | 의미 |
|--------|------|
| `app` | 앱 개발 |
| `web` | 웹 개발 |
| `watch` | 워치 개발 |

#### 유형

| 유형 | 의미 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 formatting, 세미콜론 누락 등 |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가 |
| `chore` | 패키지 매니저 수정, 기타 수정 |
| `design` | CSS 등 UI 디자인 변경 |
| `comment` | 주석 추가 및 변경 |
| `rename` | 파일/폴더명 수정/이동 |
| `remove` | 파일 삭제 |
| `!BREAKING CHANGE` | 커다란 API 변경 |
| `!HOTFIX` | 급하게 치명적인 버그 수정 |

### 커밋 메시지

```
[포지션] 유형 : 설명
```

**예시:**
- `[BE] feat : 로그인 API 구현`
- `[BE] fix : WebMvcConfig CORS 설정 허용`
- `[BE] test : AuthService 단위 테스트 작성`

**원칙:**
- 한 커밋에는 한 가지 문제만 작성
- 추적 가능하게 유지 (너무 많은 변경을 한 커밋에 담지 않기)

### PR 순서

```bash
# 1. develop 브랜치 최신화
git pull origin develop

# 2. feature 브랜치로 이동 후 병합
git checkout be-web/feat-xxx
git merge origin/develop

# 3. 충돌 해결 후 푸시
git push -u origin be-web/feat-xxx
```

### PR 템플릿

```markdown
## Part
- [ ] FE
- [ ] BE
- [ ] AI
- [ ] INFRA

## 이슈 번호
> Closes [S14P31E101-](https://ssafy.atlassian.net/browse/S14P31E101-)

## 작업 내용
-

## 참고 사항
-

## 체크리스트
- [ ] 브랜치 방향 확인 (feature/.. -> develop)
- [ ] 코드가 정상적으로 실행되나요?
- [ ] 불필요한 주석이나 더미 데이터는 없나요?
```
