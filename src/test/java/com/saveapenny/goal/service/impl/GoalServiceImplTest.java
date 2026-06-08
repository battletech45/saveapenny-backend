package com.saveapenny.goal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.goal.dto.CreateGoalRequest;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.UpdateGoalRequest;
import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.entity.ScenarioEntity;
import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.exception.InvalidGoalDateException;
import com.saveapenny.goal.exception.InvalidGoalStatusTransitionException;
import com.saveapenny.goal.exception.LinkedAccountNotFoundException;
import com.saveapenny.goal.mapper.GoalMapper;
import com.saveapenny.goal.repository.GoalRepository;
import com.saveapenny.goal.repository.GoalRunRepository;
import com.saveapenny.goal.repository.ScenarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private GoalRunRepository goalRunRepository;

    @Mock
    private AccountRepository accountRepository;

    @Spy
    private GoalMapper goalMapper = Mappers.getMapper(GoalMapper.class);

    @InjectMocks
    private GoalServiceImpl goalService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UUID userId;
    private UUID goalId;
    private UUID accountId;
    private GoalEntity goal;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(goalMapper, "objectMapper", objectMapper);

        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        goal = GoalEntity.builder()
                .id(goalId)
                .userId(userId)
                .type(GoalType.SAVINGS)
                .title("House Fund")
                .targetAmount(new BigDecimal("20000.0000"))
                .currency("USD")
                .targetDate(LocalDate.now().plusYears(2))
                .linkedAccountId(accountId)
                .status(GoalStatus.ACTIVE)
                .inputsJson("{\"version\":1,\"type\":\"SAVINGS\",\"values\":{\"targetAmount\":20000}}")
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_persistsGoalAndBaselineScenario_whenValid() {
        CreateGoalRequest request = CreateGoalRequest.builder()
                .type(GoalType.SAVINGS)
                .title(" House Fund ")
                .targetAmount(new BigDecimal("20000.0000"))
                .currency("USD")
                .targetDate(LocalDate.now().plusYears(2))
                .linkedAccountId(accountId)
                .inputs(objectMapper.createObjectNode()
                        .put("version", 1)
                        .put("type", "SAVINGS")
                        .set("values", objectMapper.createObjectNode().put("targetAmount", 20000)))
                .build();

        when(accountRepository.existsByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(true);
        when(goalRepository.save(any(GoalEntity.class))).thenAnswer(invocation -> {
            GoalEntity entity = invocation.getArgument(0);
            entity.setId(goalId);
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            return entity;
        });

        GoalResponse result = goalService.create(userId, request);

        assertNotNull(result.getId());
        assertEquals("House Fund", result.getTitle());
        assertEquals(GoalStatus.ACTIVE, result.getStatus());

        ArgumentCaptor<ScenarioEntity> scenarioCaptor = ArgumentCaptor.forClass(ScenarioEntity.class);
        verify(scenarioRepository).save(scenarioCaptor.capture());
        assertEquals(goalId, scenarioCaptor.getValue().getGoalId());
        assertEquals("Baseline", scenarioCaptor.getValue().getName());
        assertEquals(Boolean.TRUE, scenarioCaptor.getValue().getIsBaseline());
    }

    @Test
    void create_throwsWhenTargetDateIsNotFuture() {
        CreateGoalRequest request = CreateGoalRequest.builder()
                .type(GoalType.SAVINGS)
                .title("Goal")
                .targetAmount(BigDecimal.ONE)
                .currency("USD")
                .targetDate(LocalDate.now())
                .inputs(objectMapper.createObjectNode()
                        .put("version", 1)
                        .put("type", "SAVINGS")
                        .set("values", objectMapper.createObjectNode()))
                .build();

        assertThrows(InvalidGoalDateException.class, () -> goalService.create(userId, request));
        verify(goalRepository, never()).save(any());
    }

    @Test
    void create_throwsWhenLinkedAccountIsMissing() {
        CreateGoalRequest request = CreateGoalRequest.builder()
                .type(GoalType.SAVINGS)
                .title("Goal")
                .targetAmount(BigDecimal.ONE)
                .currency("USD")
                .targetDate(LocalDate.now().plusMonths(1))
                .linkedAccountId(accountId)
                .inputs(objectMapper.createObjectNode()
                        .put("version", 1)
                        .put("type", "SAVINGS")
                        .set("values", objectMapper.createObjectNode()))
                .build();

        when(accountRepository.existsByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(false);

        assertThrows(LinkedAccountNotFoundException.class, () -> goalService.create(userId, request));
    }

    @Test
    void getAll_returnsFilteredPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(goalRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(goal), pageable, 1));

        Page<GoalResponse> result = goalService.getAll(userId, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("House Fund", result.getContent().get(0).getTitle());
    }

    @Test
    void update_appliesProvidedFieldsOnly() {
        UpdateGoalRequest request = UpdateGoalRequest.builder()
                .title(" Emergency Fund ")
                .currency("EUR")
                .build();

        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(goal)).thenReturn(goal);

        GoalResponse result = goalService.update(userId, goalId, request);

        assertEquals("Emergency Fund", result.getTitle());
        assertEquals("EUR", result.getCurrency());
        assertEquals(new BigDecimal("20000.0000"), result.getTargetAmount());
    }

    @Test
    void updateStatus_throwsWhenTransitionIsInvalid() {
        goal.setStatus(GoalStatus.ABANDONED);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.of(goal));

        assertThrows(InvalidGoalStatusTransitionException.class,
                () -> goalService.updateStatus(userId, goalId, GoalStatus.ACTIVE));
    }

    @Test
    void delete_softDeletesGoal() {
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.of(goal));

        goalService.delete(userId, goalId);

        assertEquals(GoalStatus.ABANDONED, goal.getStatus());
        assertNotNull(goal.getDeletedAt());
        verify(goalRepository).save(goal);
    }

    @Test
    void getById_throwsWhenGoalMissing() {
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.empty());

        assertThrows(GoalNotFoundException.class, () -> goalService.getById(userId, goalId));
    }
}
