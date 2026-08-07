package com.saveapenny.feedback.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.test.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedback-status-admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class FeedbackStatusAdminIntegrationTest extends IntegrationTestBase {

    @Test
    void adminCanUpdateFeedbackStatus_andOwnerSeesItAndIsNotified() throws Exception {
        String userToken = register("feedback.status.user@example.com", "Feedback Status User");
        String adminToken = register("feedback.status.admin@example.com", "Feedback Status Admin");
        grantAdminRole(adminToken);

        String createBody = """
                {
                  "type": "BUG_REPORT",
                  "rating": 2,
                  "message": "App crashes when exporting reports"
                }
                """;
        String feedbackId = extractId(authedPostExpect("/api/v1/feedback", createBody, userToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn());

        authedPatchExpect(
                        "/api/v1/admin/feedback/" + feedbackId + "/status",
                        "{\"status\": \"RESOLVED\"}",
                        adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/feedback/{feedbackId}", feedbackId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("FEEDBACK_STATUS_UPDATED"));
    }

    @Test
    void nonAdminCannotUpdateFeedbackStatus() throws Exception {
        String userToken = register("feedback.status.plain@example.com", "Feedback Status Plain");

        String createBody = """
                {
                  "type": "GENERAL",
                  "message": "Just a note"
                }
                """;
        String feedbackId = extractId(authedPostExpect("/api/v1/feedback", createBody, userToken)
                .andExpect(status().isCreated())
                .andReturn());

        authedPatchExpect(
                        "/api/v1/admin/feedback/" + feedbackId + "/status",
                        "{\"status\": \"RESOLVED\"}",
                        userToken)
                .andExpect(status().isForbidden());
    }
}
