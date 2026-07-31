package com.saveapenny.feedback.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.test.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedback-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class FeedbackFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void feedbackCrudFlow_isUserScoped() throws Exception {
        String tokenA = register("feedback.user.a@example.com", "Feedback User A");
        String tokenB = register("feedback.user.b@example.com", "Feedback User B");

        String createBody = """
                {
                  "type": "FEATURE_REQUEST",
                  "rating": 5,
                  "message": "Please add account widgets",
                  "metadata": {
                    "platform": "ios",
                    "screen": "settings"
                  }
                }
                """;

        String feedbackId = extractId(authedPostExpect("/api/v1/feedback", createBody, tokenA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("FEATURE_REQUEST"))
                .andExpect(jsonPath("$.data.metadata.platform").value("ios"))
                .andReturn());

        mockMvc.perform(get("/api/v1/feedback")
                        .param("type", "FEATURE_REQUEST")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(feedbackId));

        mockMvc.perform(get("/api/v1/feedback/{feedbackId}", feedbackId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Please add account widgets"));

        mockMvc.perform(get("/api/v1/feedback/{feedbackId}", feedbackId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FEEDBACK_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/feedback/{feedbackId}", feedbackId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/feedback/{feedbackId}", feedbackId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FEEDBACK_NOT_FOUND"));
    }
}
