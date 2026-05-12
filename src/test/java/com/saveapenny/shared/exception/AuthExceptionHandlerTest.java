package com.saveapenny.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.auth.exception.EmailAlreadyExistsException;
import com.saveapenny.auth.exception.InvalidCredentialsException;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RefreshTokenExpiredException;
import com.saveapenny.shared.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailAlreadyExists_returnsConflictWithExpectedCode() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleEmailAlreadyExists(new EmailAlreadyExistsException("john@example.com"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("EMAIL_ALREADY_EXISTS", response.getBody().getError().getCode());
    }

    @Test
    void handleInvalidCredentials_returnsUnauthorizedWithExpectedCode() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_CREDENTIALS", response.getBody().getError().getCode());
    }

    @Test
    void handleInvalidRefreshToken_returnsUnauthorizedWithExpectedCode() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleInvalidRefreshToken(new InvalidRefreshTokenException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("INVALID_REFRESH_TOKEN", response.getBody().getError().getCode());
    }

    @Test
    void handleRefreshTokenExpired_returnsUnauthorizedWithExpectedCode() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleRefreshTokenExpired(new RefreshTokenExpiredException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("REFRESH_TOKEN_EXPIRED", response.getBody().getError().getCode());
    }
}
