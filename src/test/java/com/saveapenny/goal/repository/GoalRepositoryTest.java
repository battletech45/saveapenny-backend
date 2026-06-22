package com.saveapenny.goal.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class GoalRepositoryTest {

    @Autowired
    private GoalRepository goalRepository;

    private UUID userId;
    private GoalEntity entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entity = GoalEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(GoalType.SAVINGS)
                .title("Emergency Fund")
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .status(GoalStatus.ACTIVE)
                .inputsJson("{\"version\":1}")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        goalRepository.save(entity);
    }

    @Test
    void findAllByUserIdAndDeletedAtIsNull_returnsUserGoals() {
        Page<GoalEntity> page = goalRepository.findAllByUserIdAndDeletedAtIsNull(userId, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertEquals(entity.getId(), page.getContent().getFirst().getId());
    }

    @Test
    void findAllByUserIdAndDeletedAtIsNull_excludesOtherUsers() {
        Page<GoalEntity> page = goalRepository.findAllByUserIdAndDeletedAtIsNull(UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndStatusAndDeletedAtIsNull_filtersByStatus() {
        Page<GoalEntity> page = goalRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(
                userId, GoalStatus.ACTIVE, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndStatusAndDeletedAtIsNull_excludesWrongStatus() {
        Page<GoalEntity> page = goalRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(
                userId, GoalStatus.DRAFT, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndTypeAndDeletedAtIsNull_filtersByType() {
        Page<GoalEntity> page = goalRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(
                userId, GoalType.SAVINGS, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndTypeAndDeletedAtIsNull_excludesWrongType() {
        Page<GoalEntity> page = goalRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(
                userId, GoalType.DEBT_PAYOFF, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndStatusAndTypeAndDeletedAtIsNull_combinedFilter() {
        Page<GoalEntity> page = goalRepository.findAllByUserIdAndStatusAndTypeAndDeletedAtIsNull(
                userId, GoalStatus.ACTIVE, GoalType.SAVINGS, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findByIdAndUserIdAndDeletedAtIsNull_returnsGoal() {
        Optional<GoalEntity> found = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(entity.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(entity.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserIdAndDeletedAtIsNull_returnsEmptyForWrongUser() {
        Optional<GoalEntity> found = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(
                entity.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserIdAndDeletedAtIsNull_excludesSoftDeleted() {
        entity.setDeletedAt(OffsetDateTime.now());
        goalRepository.save(entity);

        Page<GoalEntity> page = goalRepository.findAllByUserIdAndDeletedAtIsNull(userId, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }
}
