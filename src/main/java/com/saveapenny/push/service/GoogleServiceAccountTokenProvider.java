package com.saveapenny.push.service;

import com.saveapenny.config.TimeService;
import com.saveapenny.push.config.FirebaseServiceAccount;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Exchanges a Firebase service account for a short-lived OAuth2 access token via the
 * JWT bearer flow, avoiding a hard dependency on the Firebase Admin SDK (same rationale
 * as the Measurement Protocol client in the analytics module: one signed HTTP call is
 * enough, no need for the SDK's gRPC/guava dependency tree).
 */
@Component
public class GoogleServiceAccountTokenProvider {

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final Duration EXPIRY_SAFETY_MARGIN = Duration.ofSeconds(60);

    private final RestClient pushRestClient;
    private final FirebaseServiceAccount serviceAccount;
    private final TimeService timeService;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile PrivateKey cachedPrivateKey;
    private volatile String cachedAccessToken;
    private volatile Instant cachedExpiry = Instant.EPOCH;

    public GoogleServiceAccountTokenProvider(
            RestClient pushRestClient, FirebaseServiceAccount serviceAccount, TimeService timeService) {
        this.pushRestClient = pushRestClient;
        this.serviceAccount = serviceAccount;
        this.timeService = timeService;
    }

    public String getAccessToken() {
        if (cachedAccessToken != null && timeService.now().isBefore(cachedExpiry.minus(EXPIRY_SAFETY_MARGIN))) {
            return cachedAccessToken;
        }
        lock.lock();
        try {
            if (cachedAccessToken != null && timeService.now().isBefore(cachedExpiry.minus(EXPIRY_SAFETY_MARGIN))) {
                return cachedAccessToken;
            }
            return refreshAccessToken();
        } finally {
            lock.unlock();
        }
    }

    private String refreshAccessToken() {
        Instant now = timeService.now();
        Instant expiry = now.plus(Duration.ofHours(1));
        String assertion = Jwts.builder()
                .issuer(serviceAccount.clientEmail())
                .subject(serviceAccount.clientEmail())
                .audience().add(serviceAccount.tokenUri()).and()
                .claim("scope", SCOPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey(), Jwts.SIG.RS256)
                .compact();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("assertion", assertion);

        Map<String, Object> response = pushRestClient.post()
                .uri(serviceAccount.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON)))
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });

        String accessToken = (String) response.get("access_token");
        Number expiresIn = (Number) response.getOrDefault("expires_in", 3600);
        cachedAccessToken = accessToken;
        cachedExpiry = now.plusSeconds(expiresIn.longValue());
        return accessToken;
    }

    private PrivateKey privateKey() {
        if (cachedPrivateKey != null) {
            return cachedPrivateKey;
        }
        try {
            String normalized = serviceAccount.privateKey().replace("\\n", "\n");
            String pem = normalized
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            cachedPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            return cachedPrivateKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse push.fcm.private-key as a PKCS8 RSA private key.", ex);
        }
    }
}
