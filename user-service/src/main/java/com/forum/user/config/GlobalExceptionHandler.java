package com.forum.user.config;

import com.forum.common.dto.Result;
import com.forum.common.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "phone", "手机号",
            "nickname", "昵称",
            "password", "密码",
            "email", "邮箱",
            "username", "用户名"
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = e.getCode() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidation(Exception e) {
        FieldError fieldError = null;
        if (e instanceof MethodArgumentNotValidException ex) {
            fieldError = ex.getBindingResult().getFieldError();
        } else if (e instanceof BindException ex) {
            fieldError = ex.getBindingResult().getFieldError();
        }
        String message = "参数校验失败";
        if (fieldError != null) {
            String label = FIELD_LABELS.getOrDefault(fieldError.getField(), fieldError.getField());
            String detail = fieldError.getDefaultMessage();
            message = label + "：" + (detail != null && !detail.isBlank() ? detail : "格式不正确");
        }
        return ResponseEntity.badRequest().body(Result.fail(400, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDuplicate(DataIntegrityViolationException e) {
        String detail = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        String message = "数据冲突，请检查输入";
        if (detail != null) {
            String lower = detail.toLowerCase();
            if (lower.contains("email") || lower.contains("'@")) {
                message = "邮箱已注册";
            } else if (lower.contains("phone")) {
                message = "该手机号已注册";
            } else if (lower.contains("nickname")) {
                message = "昵称已被使用";
            }
        }
        return ResponseEntity.badRequest().body(Result.fail(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        return ResponseEntity.internalServerError().body(Result.fail(500, e.getMessage()));
    }
}
