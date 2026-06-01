package com.msa4meerkatgram.global.errors;

import com.msa4meerkatgram.global.errors.custom.InvalidTokenException;
import com.msa4meerkatgram.global.errors.custom.NotRegisteredException;
import com.msa4meerkatgram.global.responses.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalResponse<String>> notRegisteredHandle(NotRegisteredException e) {
        return ResponseEntity.status(400).body(
                GlobalResponse.<String>builder()
                        .code("E01")
                        .message("로그인 에러")
                        .data(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<GlobalResponse<String>> invalidTokenHandle(InvalidTokenException e) {
        return ResponseEntity.status(400).body(
                GlobalResponse.<String>builder()
                        .code("E04")
                        .message("토큰 이상")
                        .data(e.getMessage())
                        .build()
        );
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalResponse<String>> methodArgumentTypeMismatchExceptionHandle(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(400).body(
            GlobalResponse.<String>builder()
                .code("E21")
                .message("요청 파라미터에 이상이 있습니다.")
                .data(String.format("%s : 필드를 확인해 주세요.",e.getName()))
                .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<List<String>>> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ResponseEntity.status(400).body(
            GlobalResponse.<List<String>>builder()
                .code("E21")
                .message("요청 파라미터에 이상이 있습니다.")
                .data(
                        e.getBindingResult()
                        .getAllErrors()
                        .stream()
                        .map(ObjectError::getDefaultMessage)
                        .toList()
                )
                .build()
        );
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<String>> othersHandle(Exception e) {
        log.error(String.format("시스템 에러: %s\n%s",e.getMessage(), Arrays.toString(e.getStackTrace())));
        return ResponseEntity.status(500).body(
            GlobalResponse.<String>builder()
                           .code("E99")
                           .message("시스템 에러")
                           .data("현재 서비스 이용이 불가합니다. 잠시후 다시 시도해 주십시오.")
                           .build()
        );
    }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<GlobalResponse<String>> authenticationHandle(AuthenticationException e) {
            return ResponseEntity.status(401).body(
                GlobalResponse.<String>builder()
                    .code("E02")
                    .message("UNAUTHENTICATED_ERROR")
                    .data("로그인이 필요한 서비스입니다.")
                    .build()
            );
        }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponse<String>> accessDeniedHandle(AccessDeniedException e) {
        return ResponseEntity.status(403).body(
            GlobalResponse.<String>builder()
                .code("E03")
                .message("UNAUTHORIZED_ERROR")
                .data("로그인이 필요한 서비스입니다.")
                .build()
        );
    }

}
