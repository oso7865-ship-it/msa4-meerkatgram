package com.msa4meerkatgram.global.errors;

import com.msa4meerkatgram.global.errors.constant.CustomErrorCode;
import com.msa4meerkatgram.global.errors.custom.*;
import com.msa4meerkatgram.global.responses.GlobalErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<GlobalErrorResponse> generateErrorResponse(CustomErrorCode customErrorCode) {
        return ResponseEntity.status(customErrorCode.getHttpStatus())
                   .body(GlobalErrorResponse.from(customErrorCode.getCode(),customErrorCode.name()));
    }

    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalErrorResponse> notRegisteredHandle(NotRegisteredException e) {
        log.debug(CustomErrorCode.NOT_REGISTERED_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.NOT_REGISTERED_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalErrorResponse> authenticationHandle(AuthenticationException e) {
        log.debug(CustomErrorCode.UNAUTHENTICATED_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.UNAUTHENTICATED_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalErrorResponse> accessDeniedHandle(AccessDeniedException e) {
        log.debug(CustomErrorCode.UNAUTHORIZED_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.UNAUTHORIZED_ERROR);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<GlobalErrorResponse> invalidTokenHandle(InvalidTokenException e) {
        log.debug(CustomErrorCode.INVALID_TOKEN_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.INVALID_TOKEN_ERROR);
    }

    @ExceptionHandler(InvalidPostCreateException.class)
    public ResponseEntity<GlobalErrorResponse> invalidPostCreateHandle(InvalidPostCreateException e) {
        log.debug(CustomErrorCode.INVALID_PARAMETER_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(PostDeleteException.class)
    public ResponseEntity<GlobalErrorResponse> postDeleteHandle(PostDeleteException e) {
        log.debug(CustomErrorCode.UNAUTHENTICATED_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.UNAUTHENTICATED_ERROR);
    }

    @ExceptionHandler(DeletedRecordException.class)
    public ResponseEntity<GlobalErrorResponse> deletedRecordHandle(DeletedRecordException e) {
        log.debug(CustomErrorCode.NOT_FOUND_DATA_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.NOT_FOUND_DATA_ERROR);
    }

    @ExceptionHandler(DuplicatedRecordException.class)
    public ResponseEntity<GlobalErrorResponse> DuplicatedRecordHandle(DuplicatedRecordException e) {
        log.debug(CustomErrorCode.DUPLICATED_DATA_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.DUPLICATED_DATA_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalErrorResponse> methodArgumentTypeMismatchExceptionHandle(MethodArgumentTypeMismatchException e) {
        log.debug(CustomErrorCode.INVALID_PARAMETER_ERROR.name(), String.format("%s : 필드를 확인해 주세요.",e.getName()));
        return generateErrorResponse(CustomErrorCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalErrorResponse> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.debug(CustomErrorCode.INVALID_PARAMETER_ERROR.name(), String.format("%s : 필드를 확인해 주세요.",
            e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .toList())
        );
        return generateErrorResponse(CustomErrorCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(FileManagedException.class)
    public ResponseEntity<GlobalErrorResponse> FileManagedHandle(FileManagedException e) {
        log.debug(CustomErrorCode.FILE_MANAGED_ERROR.name(), e);
        return generateErrorResponse(CustomErrorCode.FILE_MANAGED_ERROR);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<GlobalErrorResponse> sqlHandle(SQLException e) {
        log.error("DB 에러", e);
        return generateErrorResponse(CustomErrorCode.DB_ERROR);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalErrorResponse> othersHandle(Exception e) {
        log.error("시스템 에러", e);
        return generateErrorResponse(CustomErrorCode.SYSTEM_ERROR);
    }



}
