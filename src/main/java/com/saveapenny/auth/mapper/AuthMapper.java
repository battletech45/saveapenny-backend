package com.saveapenny.auth.mapper;

import com.saveapenny.auth.dto.AuthTokenResponse;
import com.saveapenny.auth.dto.RefreshTokenResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    default AuthTokenResponse toAuthTokenResponse(String accessToken, String refreshToken, long expiresIn) {
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }

    default RefreshTokenResponse toRefreshTokenResponse(String accessToken, long expiresIn) {
        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
