# 공통 응답 형식 & 예외처리 가이드

## 패키지 위치

```
global/
├── response/
│   └── ApiResponse.java
└── exception/
    ├── ResponseCode.java       (인터페이스)
    ├── ErrorCode.java          (공통 에러 코드)
    ├── CustomException.java
    └── GlobalExceptionHandler.java
```

---

## 1. 공통 응답 형식 `ApiResponse<T>`

모든 API 응답은 `ApiResponse`로 감싸서 반환합니다.

### 응답 JSON 구조

**성공**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... }
}
```

**실패**
```json
{
  "success": false,
  "message": "요청한 리소스를 찾을 수 없습니다.",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

> `data`, `errorCode`는 null이면 응답 JSON에서 제외됩니다.

### 사용법

```java
// 데이터 반환
return ResponseEntity.ok(ApiResponse.ok(data));

// 메시지 + 데이터 반환
return ResponseEntity.ok(ApiResponse.ok("간호사 등록 완료", data));

// 데이터 없이 성공 메시지만
return ResponseEntity.ok(ApiResponse.ok("삭제 완료"));
```

---

## 2. 예외 발생 `CustomException`

비즈니스 로직에서 예외가 발생할 때는 반드시 `CustomException`을 사용합니다.  
`GlobalExceptionHandler`가 자동으로 잡아서 `ApiResponse` 형태로 응답합니다.

### 사용법

```java
// 기본 (ErrorCode 메시지 그대로 사용)
throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);

// detail 추가 (로그에만 기록, 응답에는 미포함)
throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "간호사 ID 42번 없음");

// 원인 예외 래핑
throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
```

> `detail`은 서버 로그에만 남습니다. 민감한 내부 정보를 적어도 클라이언트에 노출되지 않습니다.

---

## 3. 에러 코드 `ErrorCode`

### 현재 정의된 공통 에러 코드

| ErrorCode | HTTP Status | 메시지 |
|---|---|---|
| `INVALID_INPUT_VALUE` | 400 | 입력값이 올바르지 않습니다. |
| `RESOURCE_NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. |
| `DUPLICATE_RESOURCE` | 409 | 이미 존재하는 리소스입니다. |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드입니다. |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | 지원하지 않는 미디어 타입입니다. |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류가 발생했습니다. |

### 도메인별 에러 코드 추가 방법

공통 `ErrorCode`에 없는 에러는 도메인별 enum을 따로 만들어 `ResponseCode`를 구현합니다.

```java
@Getter
@RequiredArgsConstructor
public enum NurseErrorCode implements ResponseCode {

    NURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "간호사를 찾을 수 없습니다."),
    NURSE_ALREADY_ON_DUTY(HttpStatus.CONFLICT, "이미 근무 중인 간호사입니다.");

    private final HttpStatus status;
    private final String message;
}
```

```java
throw new CustomException(NurseErrorCode.NURSE_NOT_FOUND);
```

---

## 4. 전체 예시

```java
@RestController
@RequestMapping("/api/nurses")
@RequiredArgsConstructor
public class NurseController {

    private final NurseService nurseService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NurseResponse>> getNurse(@PathVariable Long id) {
        NurseResponse nurse = nurseService.findById(id);
        return ResponseEntity.ok(ApiResponse.ok(nurse));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NurseResponse>> createNurse(@Valid @RequestBody NurseRequest request) {
        NurseResponse nurse = nurseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("간호사 등록 완료", nurse));
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class NurseService {

    private final NurseRepository nurseRepository;

    public NurseResponse findById(Long id) {
        Nurse nurse = nurseRepository.findById(id)
                .orElseThrow(() -> new CustomException(NurseErrorCode.NURSE_NOT_FOUND, "id=" + id));
        return NurseResponse.from(nurse);
    }
}
```
