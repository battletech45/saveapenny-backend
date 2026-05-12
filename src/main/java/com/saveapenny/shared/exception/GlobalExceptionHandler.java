package com.saveapenny.shared.exception;

import com.saveapenny.account.exception.AccountNameAlreadyExistsException;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.exception.AccountInactiveException;
import com.saveapenny.category.exception.CategoryNameAlreadyExistsException;
import com.saveapenny.category.exception.CategoryNotFoundException;
import com.saveapenny.category.exception.SystemCategoryModificationNotAllowedException;
import com.saveapenny.auth.exception.EmailAlreadyExistsException;
import com.saveapenny.auth.exception.InvalidCredentialsException;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RefreshTokenExpiredException;
import com.saveapenny.shared.api.ApiError;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.user.exception.InvalidPasswordException;
import com.saveapenny.user.exception.PasswordReuseNotAllowedException;
import com.saveapenny.user.exception.UserNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryNotFound(CategoryNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("CATEGORY_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(CategoryNameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleCategoryNameAlreadyExists(CategoryNameAlreadyExistsException ex) {
        ApiError error = ApiError.builder()
                .code("CATEGORY_NAME_ALREADY_EXISTS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(SystemCategoryModificationNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSystemCategoryModificationNotAllowed(
            SystemCategoryModificationNotAllowedException ex) {
        ApiError error = ApiError.builder()
                .code("SYSTEM_CATEGORY_MODIFICATION_NOT_ALLOWED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountNameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNameAlreadyExists(AccountNameAlreadyExistsException ex) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_NAME_ALREADY_EXISTS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountInactive(AccountInactiveException ex) {
        ApiError error = ApiError.builder()
                .code("ACCOUNT_INACTIVE")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        ApiError error = ApiError.builder()
                .code("EMAIL_ALREADY_EXISTS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_CREDENTIALS")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_REFRESH_TOKEN")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshTokenExpired(RefreshTokenExpiredException ex) {
        ApiError error = ApiError.builder()
                .code("REFRESH_TOKEN_EXPIRED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        ApiError error = ApiError.builder()
                .code("USER_NOT_FOUND")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        ApiError error = ApiError.builder()
                .code("INVALID_PASSWORD")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(PasswordReuseNotAllowedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordReuseNotAllowed(PasswordReuseNotAllowedException ex) {
        ApiError error = ApiError.builder()
                .code("PASSWORD_REUSE_NOT_ALLOWED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toMessage)
                .collect(Collectors.toList());

        ApiError error = ApiError.builder()
                .code("VALIDATION_FAILED")
                .message("Request validation failed.")
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        ApiError error = ApiError.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred.")
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = ApiError.builder()
                .code("ACCESS_DENIED")
                .message(ex.getMessage())
                .details(List.of())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    private String toMessage(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
