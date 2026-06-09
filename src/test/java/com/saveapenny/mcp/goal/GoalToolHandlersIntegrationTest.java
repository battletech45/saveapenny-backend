package com.saveapenny.mcp.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.goal.service.GoalService;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.registry.ToolRegistry;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.repository.RoleRepository;
import com.saveapenny.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goal-tool-handlers;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class GoalToolHandlersIntegrationTest {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private GoalService goalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID goalId;

    @BeforeEach
    void setUp() throws Exception {
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        User user = userRepository.save(User.builder()
                .email("goal.tool+" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .fullName("Goal Tool")
                .active(true)
                .build());
        userId = user.getId();

        goalId = goalService.create(userId, com.saveapenny.goal.dto.CreateGoalRequest.builder()
                .type(com.saveapenny.goal.entity.GoalType.SAVINGS)
                .title("House Fund")
                .targetAmount(new java.math.BigDecimal("20000.00"))
                .currency("USD")
                .targetDate(java.time.LocalDate.now().plusYears(2))
                .inputs(objectMapper.readTree("{\"version\":1,\"type\":\"SAVINGS\",\"values\":{\"targetAmount\":20000.00,\"currency\":\"USD\",\"targetDate\":\"2030-01-01\",\"startBalance\":1000.00}}"))
                .build()).getId();
    }

    @Test
    void registry_exposesGoalReadTools() {
        assertTrue(toolRegistry.findByName("list_goals").isPresent());
        assertTrue(toolRegistry.findByName("get_goal").isPresent());
        assertTrue(toolRegistry.findByName("get_goal_progress").isPresent());
        assertTrue(toolRegistry.findByName("list_goal_scenarios").isPresent());
        assertTrue(toolRegistry.findByName("list_goal_runs").isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getGoalTool_readsOwnedGoal() {
        var handler = (com.saveapenny.mcp.execution.ToolHandler<GetGoalToolInput, GetGoalToolResult>) toolRegistry.findByName("get_goal")
                .orElseThrow();

        GetGoalToolResult result = handler.execute(new ToolExecutionContext(userId), new GetGoalToolInput(goalId)).data();

        assertEquals(goalId, result.goal().goalId());
        assertEquals("House Fund", result.goal().title());
    }
}
