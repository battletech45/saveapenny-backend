package com.saveapenny.goal.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.billing.entity.BillingEntitlement;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import com.saveapenny.billing.repository.BillingEntitlementRepository;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunEntity;
import com.saveapenny.goal.entity.GoalRunTrigger;
import com.saveapenny.goal.repository.GoalRunRepository;
import com.saveapenny.goal.scheduler.GoalProgressJob;
import com.saveapenny.goal.service.GoalService;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.repository.NotificationRepository;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.entity.User;
import com.saveapenny.user.repository.RoleRepository;
import com.saveapenny.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goal-progress-job;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "goal.progress.enabled=true"
})
class GoalProgressJobIntegrationTest {

    @Autowired
    private GoalService goalService;
    @Autowired
    private GoalRunRepository goalRunRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private GoalProgressJob goalProgressJob;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BillingEntitlementRepository billingEntitlementRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        User user = userRepository.save(User.builder()
                .email("goal.progress+" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .fullName("Goal Progress")
                .active(true)
                .build());
        userId = user.getId();

        // This test exercises the job across multiple active goals, which exceeds the FREE plan's cap.
        billingEntitlementRepository.save(BillingEntitlement.builder()
                .userId(userId)
                .plan(Plan.PLUS)
                .status(EntitlementStatus.ACTIVE)
                .willRenew(true)
                .build());
    }

    @Test
    void evaluateAllActiveGoals_createsSingleNotificationOnSecondOffTrackRun() throws Exception {
        UUID offTrackGoalId = createGoal("Off Track Goal", new BigDecimal("1000.00"), new BigDecimal("5000.00"));
        UUID onTrackGoalId = createGoal("On Track Goal", new BigDecimal("1000.00"), new BigDecimal("21000.00"));

        assertEquals(0, notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK).size());

        goalProgressJob.evaluateAllActiveGoals();
        assertEquals(0, notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK).size());

        goalProgressJob.evaluateAllActiveGoals();
        assertEquals(1, notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK).size());

        goalProgressJob.evaluateAllActiveGoals();
        assertEquals(1, notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK).size());
    }

    private UUID createGoal(String title, BigDecimal startBalance, BigDecimal projectedAmount) throws Exception {
        var goal = goalService.create(userId, com.saveapenny.goal.dto.CreateGoalRequest.builder()
                .type(com.saveapenny.goal.entity.GoalType.SAVINGS)
                .title(title)
                .targetAmount(new BigDecimal("20000.00"))
                .currency("USD")
                .targetDate(LocalDate.now().plusMonths(12))
                .inputs(objectMapper.readTree("{\"version\":1,\"type\":\"SAVINGS\",\"values\":{\"startBalance\":" + startBalance + ",\"targetAmount\":20000.00,\"currency\":\"USD\",\"targetDate\":\"2030-01-01\"}}"))
                .build());

        var detail = goalService.getById(userId, goal.getId());
        UUID baselineScenarioId = detail.getScenarios().getFirst().getId();
        goalRunRepository.save(GoalRunEntity.builder()
                .goalId(goal.getId())
                .scenarioId(baselineScenarioId)
                .inputsSnapshotJson(detail.getInputs().toString())
                .outputSummaryJson("{\"projectedAmount\":" + projectedAmount + "}")
                .feasibility(Feasibility.ON_TRACK)
                .triggeredBy(GoalRunTrigger.USER)
                .build());
        return goal.getId();
    }
}
