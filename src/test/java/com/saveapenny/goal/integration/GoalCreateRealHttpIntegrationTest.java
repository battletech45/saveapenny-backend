package com.saveapenny.goal.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots a real embedded servlet container and issues an actual HTTP request over the wire,
 * instead of going through MockMvc. MockMvc does not reproduce Spring's real HttpMessageConverter
 * selection, so it previously missed a classpath regression where a transitive Jackson 3
 * ("tools.jackson") starter shadowed the app's Jackson 2 request converter, breaking
 * deserialization of any JsonNode-typed request field (e.g.
 * {@link com.saveapenny.goal.dto.CreateGoalRequest#getInputs()}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goal-create-real-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class GoalCreateRealHttpIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RoleRepository roleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @Test
    void create_overRealHttp_deserializesJsonNodeInputsAndPersists() throws Exception {
        String token = registerAndGetToken("goal.real.http@example.com", "Goal Real Http");

        String createBody = """
                {
                  "type": "SAVINGS",
                  "title": "Emergency Fund",
                  "targetAmount": 10000.0000,
                  "currency": "USD",
                  "targetDate": "2030-06-01",
                  "inputs": {
                    "version": 1,
                    "type": "SAVINGS",
                    "values": {
                      "monthlyContribution": 350,
                      "expectedAnnualReturn": 0,
                      "startBalance": 1500
                    }
                  }
                }
                """;

        HttpResponse<String> response = post("/api/v1/goals", createBody, token);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("title").asText()).isEqualTo("Emergency Fund");
        assertThat(body.path("data").path("inputs").path("values").path("monthlyContribution").asInt())
                .isEqualTo(350);
    }

    private String registerAndGetToken(String email, String fullName) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Strong@123",
                  "fullName": "%s"
                }
                """.formatted(email, fullName);

        HttpResponse<String> response = post("/api/v1/auth/register", registerBody, null);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        return json.path("data").path("accessToken").asText();
    }

    private HttpResponse<String> post(String path, String body, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url(path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
