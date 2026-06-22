package com.saveapenny.goal.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunEntity;
import com.saveapenny.goal.entity.GoalRunTrigger;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class GoalRunRepositoryTest {

    @Autowired
    private GoalRunRepository goalRunRepository;

    private UUID goalId;
    private GoalRunEntity run1;
    private GoalRunEntity run2;

    @BeforeEach
    void setUp() {
        goalId = UUID.randomUUID();
        UUID scenarioId = UUID.randomUUID();

        run1 = GoalRunEntity.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .scenarioId(scenarioId)
                .inputsSnapshotJson("{}")
                .outputSummaryJson("{\"projectedAmount\": 5000}")
                .feasibility(Feasibility.ON_TRACK)
                .triggeredBy(GoalRunTrigger.USER)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        run2 = GoalRunEntity.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .scenarioId(scenarioId)
                .inputsSnapshotJson("{}")
                .outputSummaryJson("{\"projectedAmount\": 4500}")
                .feasibility(Feasibility.TIGHT)
                .triggeredBy(GoalRunTrigger.PROGRESS_JOB)
                .createdAt(OffsetDateTime.now())
                .build();

        goalRunRepository.save(run1);
        goalRunRepository.save(run2);
    }

    @Test
    void findAllByGoalIdOrderByCreatedAtDesc_returnsRunsOrdered() {
        Page<GoalRunEntity> page = goalRunRepository.findAllByGoalIdOrderByCreatedAtDesc(
                goalId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().get(0).getCreatedAt()
                .isAfter(page.getContent().get(1).getCreatedAt())
                || page.getContent().get(0).getCreatedAt()
                .isEqual(page.getContent().get(1).getCreatedAt()));
    }

    @Test
    void findAllByGoalIdOrderByCreatedAtDesc_returnsEmptyForUnknownGoal() {
        Page<GoalRunEntity> page = goalRunRepository.findAllByGoalIdOrderByCreatedAtDesc(
                UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findTopByGoalIdOrderByCreatedAtDesc_returnsLatestRun() {
        Optional<GoalRunEntity> found = goalRunRepository.findTopByGoalIdOrderByCreatedAtDesc(goalId);
        assertTrue(found.isPresent());
        assertEquals(run2.getId(), found.get().getId());
    }

    @Test
    void findTopByGoalIdOrderByCreatedAtDesc_returnsEmptyForUnknownGoal() {
        Optional<GoalRunEntity> found = goalRunRepository.findTopByGoalIdOrderByCreatedAtDesc(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }
}
