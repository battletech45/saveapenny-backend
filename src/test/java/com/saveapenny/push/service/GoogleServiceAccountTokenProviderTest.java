package com.saveapenny.push.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.saveapenny.config.TimeService;
import com.saveapenny.push.config.FirebaseServiceAccount;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleServiceAccountTokenProviderTest {

    private static String generatePrivateKeyPem() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }

    private static FirebaseServiceAccount serviceAccount(String privateKey) {
        return new FirebaseServiceAccount(
                "proj-1",
                "svc@proj-1.iam.gserviceaccount.com",
                privateKey,
                "https://oauth2.googleapis.com/token");
    }

    /** A {@link Clock} whose {@code instant()} can be advanced mid-test, to exercise the provider's expiry logic without sleeping. */
    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        MutableClock(Instant initial) {
            this.instant = new AtomicReference<>(initial);
        }

        void advanceTo(Instant next) {
            instant.set(next);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }

    @Test
    void fetchesAndCachesAccessToken() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        TimeService timeService = new TimeService(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        GoogleServiceAccountTokenProvider provider =
                new GoogleServiceAccountTokenProvider(restClient, serviceAccount(generatePrivateKeyPem()), timeService);

        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"access_token\":\"token-1\",\"expires_in\":3600}"));

        String first = provider.getAccessToken();
        String second = provider.getAccessToken();

        assertEquals("token-1", first);
        assertEquals("token-1", second);
        server.verify();
    }

    @Test
    void refreshesTokenAfterExpiry() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        TimeService timeService = new TimeService(clock);
        GoogleServiceAccountTokenProvider provider =
                new GoogleServiceAccountTokenProvider(restClient, serviceAccount(generatePrivateKeyPem()), timeService);

        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"access_token\":\"token-1\",\"expires_in\":60}"));
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"access_token\":\"token-2\",\"expires_in\":3600}"));

        assertEquals("token-1", provider.getAccessToken());

        clock.advanceTo(Instant.parse("2026-01-01T00:10:00Z"));

        assertEquals("token-2", provider.getAccessToken());
        server.verify();
    }

    @Test
    void throwsWhenPrivateKeyIsNotValidPkcs8() {
        RestClient restClient = RestClient.builder().build();
        TimeService timeService = new TimeService(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        GoogleServiceAccountTokenProvider provider =
                new GoogleServiceAccountTokenProvider(restClient, serviceAccount("not-a-valid-key"), timeService);

        assertThrows(IllegalStateException.class, provider::getAccessToken);
    }
}
