package com.xinyu.common.web;

import com.xinyu.common.api.ApiResponse;
import com.xinyu.common.api.ErrorCode;
import com.xinyu.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException exception) {
        ErrorCode code = exception.getErrorCode();
        return response(code, exception.getMessage(), exception.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBodyValidation(
            MethodArgumentNotValidException exception) {
        return validationResponse(fieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBinding(BindException exception) {
        return validationResponse(fieldErrors(exception.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodValidation(
            HandlerMethodValidationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(error ->
                        errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
                parameterErrors.getGlobalErrors().forEach(error ->
                        errors.putIfAbsent(parameterErrors.getObjectName(), error.getDefaultMessage()));
                continue;
            }
            String parameterName = result.getMethodParameter().getParameterName();
            errors.putIfAbsent(parameterName == null ? "request" : parameterName,
                    firstMessage(result.getResolvableErrors()));
        }
        exception.getCrossParameterValidationResults().forEach(error ->
                errors.putIfAbsent("request", error.getDefaultMessage()));
        return validationResponse(errors.isEmpty() ? Map.of("request", "request validation failed") : errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintValidation(
            ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return validationResponse(errors.isEmpty() ? Map.of("request", "request validation failed") : errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
        return response(ErrorCode.INVALID_ARGUMENT, "request body is invalid", null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestParameter() {
        return response(ErrorCode.INVALID_ARGUMENT, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation, traceId={}", TraceContext.currentTraceId());
        return response(ErrorCode.DUPLICATE_RESOURCE, null, null);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound() {
        return response(ErrorCode.NOT_FOUND, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled exception, traceId={}", TraceContext.currentTraceId(), exception);
        return response(ErrorCode.INTERNAL_ERROR, null, null);
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> validationResponse(Map<String, String> errors) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, errors));
    }

    private Map<String, String> fieldErrors(Iterable<FieldError> errors) {
        Map<String, String> result = new LinkedHashMap<>();
        errors.forEach(error -> result.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return result;
    }

    private String firstMessage(Iterable<? extends MessageSourceResolvable> errors) {
        for (MessageSourceResolvable error : errors) {
            return error.getDefaultMessage() == null ? "request validation failed" : error.getDefaultMessage();
        }
        return "request validation failed";
    }

    private <T> ResponseEntity<ApiResponse<T>> response(ErrorCode code, String message, T data) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.failure(code, message == null ? code.getMessage() : message, data));
    }
}
