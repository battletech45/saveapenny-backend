package com.saveapenny.auth.service;

import com.saveapenny.auth.dto.AuthTokenResponse;
import com.saveapenny.auth.dto.LoginRequest;
import com.saveapenny.auth.dto.LogoutRequest;
import com.saveapenny.auth.dto.RefreshTokenRequest;
import com.saveapenny.auth.dto.RefreshTokenResponse;
import com.saveapenny.auth.dto.RegisterRequest;

public interface AuthService {

    AuthTokenResponse register(RegisterRequest request);

    AuthTokenResponse login(LoginRequest request);

    RefreshTokenResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}
