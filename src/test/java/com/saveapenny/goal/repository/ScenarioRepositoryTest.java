package com.saveapenny.goal.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.goal.entity.ScenarioEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ScenarioRepositoryTest {

    @Autowired
    private ScenarioRepository scenarioRepository;

    private UUID goalId;
    private ScenarioEntity baseline;
    private ScenarioEntity alternative;

    @BeforeEach
    void setUp() {
        goalId = UUID.randomUUID();

        baseline = ScenarioEntity.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .name("Baseline")
                .inputsJson("{\"monthlyContribution\": 300}")
                .isBaseline(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        alternative = ScenarioEntity.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .name("Aggressive")
                .inputsJson("{\"monthlyContribution\": 500}")
                .isBaseline(false)
                .createdAt(OffsetDateTime.now())
                .build();

        scenarioRepository.save(baseline);
        scenarioRepository.save(alternative);
    }

    @Test
    void findAllByGoalIdOrderByCreatedAtAsc_returnsScenarios() {
        List<ScenarioEntity> results = scenarioRepository.findAllByGoalIdOrderByCreatedAtAsc(goalId);
        assertEquals(2, results.size());
    }

    @Test
    void findAllByGoalIdOrderByCreatedAtAsc_returnsEmptyForUnknownGoal() {
        List<ScenarioEntity> results = scenarioRepository.findAllByGoalIdOrderByCreatedAtAsc(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void findByIdAndGoalId_returnsScenario() {
        Optional<ScenarioEntity> found = scenarioRepository.findByIdAndGoalId(baseline.getId(), goalId);
        assertTrue(found.isPresent());
        assertEquals("Baseline", found.get().getName());
    }

    @Test
    void findByIdAndGoalId_returnsEmptyForWrongGoal() {
        Optional<ScenarioEntity> found = scenarioRepository.findByIdAndGoalId(baseline.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findByGoalIdAndIsBaselineTrue_returnsBaseline() {
        Optional<ScenarioEntity> found = scenarioRepository.findByGoalIdAndIsBaselineTrue(goalId);
        assertTrue(found.isPresent());
        assertTrue(found.get().getIsBaseline());
    }

    @Test
    void findByGoalIdAndIsBaselineTrue_returnsEmptyWhenNoneBaseline() {
        baseline.setIsBaseline(false);
        scenarioRepository.save(baseline);

        Optional<ScenarioEntity> found = scenarioRepository.findByGoalIdAndIsBaselineTrue(goalId);
        assertTrue(found.isEmpty());
    }
}
