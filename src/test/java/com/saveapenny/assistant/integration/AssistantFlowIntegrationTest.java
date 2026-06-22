package com.saveapenny.assistant.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.assistant.dto.AssistantChatRequest;
import com.saveapenny.assistant.dto.AssistantChatResponse;
import com.saveapenny.assistant.service.AssistantService;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistant-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class AssistantFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private AssistantService assistantService;

    @BeforeEach
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @Test
    void chatFlow_worksForAuthenticatedUser() throws Exception {
        String token = registerAndGetToken("assistant.flow@example.com", "Assistant Flow");

        UUID sessionId = UUID.randomUUID();
        AssistantChatResponse mockResponse = AssistantChatResponse.builder()
                .sessionId(sessionId)
                .reply("Try tracking daily expenses.")
                .disclaimer("This assistant provides general budgeting guidance, not financial, tax, or legal advice.")
                .build();

        org.mockito.Mockito.when(assistantService.chat(any(UUID.class), any(AssistantChatRequest.class)))
                .thenReturn(mockResponse);

        String chatBody = """
                {"message":"How can I save more?"}
                """;

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.data.reply").value("Try tracking daily expenses."))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void chatFlow_withSessionResumption() throws Exception {
        String token = registerAndGetToken("assistant.session@example.com", "Session Resume");

        UUID existingSessionId = UUID.randomUUID();
        AssistantChatResponse firstResponse = AssistantChatResponse.builder()
                .sessionId(existingSessionId)
                .reply("Hello! How can I help?")
                .build();

        org.mockito.Mockito.when(assistantService.chat(any(UUID.class), any(AssistantChatRequest.class)))
                .thenReturn(firstResponse);

        String chatBody = """
                {"sessionId":"%s","message":"Hello again"}
                """.formatted(existingSessionId.toString());

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(existingSessionId.toString()))
                .andExpect(jsonPath("$.data.reply").value("Hello! How can I help?"));
    }

    @Test
    void chatFlow_returnsUnauthorized_whenNoToken() throws Exception {
        String chatBody = """
                {"message":"How can I save more?"}
                """;

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private String registerAndGetToken(String email, String fullName) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Strong@123",
                  "fullName": "%s"
                }
                """.formatted(email, fullName);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }
}
