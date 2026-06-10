package com.saveapenny.auth.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.auth.dto.AuthTokenResponse;
import com.saveapenny.auth.dto.RefreshTokenResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AuthMapperTest {

    private final AuthMapper authMapper = Mappers.getMapper(AuthMapper.class);

    @Test
    void toAuthTokenResponse_mapsFields() {
        AuthTokenResponse response = authMapper.toAuthTokenResponse("access-token", "refresh-token", 900L);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
    }

    @Test
    void toRefreshTokenResponse_mapsFields() {
        RefreshTokenResponse response = authMapper.toRefreshTokenResponse("new-access-token", 1800L);

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
    }
}
