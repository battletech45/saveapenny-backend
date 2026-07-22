package com.saveapenny.goal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.analytics.dto.AnalyticsEvent;
import com.saveapenny.analytics.service.AnalyticsEventPublisher;
import com.saveapenny.billing.service.BillingAccessService;
import com.saveapenny.config.TimeService;
import com.saveapenny.goal.dto.CreateGoalRequest;
import com.saveapenny.goal.dto.CreateScenarioRequest;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.dto.UpdateGoalRequest;
import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalRunEntity;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.entity.ScenarioEntity;
import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.exception.InvalidGoalDateException;
import com.saveapenny.goal.exception.InvalidGoalStatusTransitionException;
import com.saveapenny.goal.exception.InvalidGoalTypeException;
import com.saveapenny.goal.exception.LinkedAccountNotFoundException;
import com.saveapenny.goal.mapper.GoalMapper;
import com.saveapenny.goal.repository.GoalRepository;
import com.saveapenny.goal.repository.GoalRunRepository;
import com.saveapenny.goal.repository.ScenarioRepository;
import com.saveapenny.goal.service.GoalService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalServiceImpl implements GoalService {

    private static final String BASELINE_SCENARIO_NAME = "Baseline";

    private final GoalRepository goalRepository;
    private final ScenarioRepository scenarioRepository;
    private final GoalRunRepository goalRunRepository;
    private final AccountRepository accountRepository;
    private final GoalMapper goalMapper;
    private final TimeService timeService;
    private final AnalyticsEventPublisher analyticsEventPublisher;
    private final BillingAccessService billingAccessService;

    public GoalServiceImpl(
            GoalRepository goalRepository,
            ScenarioRepository scenarioRepository,
            GoalRunRepository goalRunRepository,
            AccountRepository accountRepository,
            GoalMapper goalMapper,
            TimeService timeService,
            AnalyticsEventPublisher analyticsEventPublisher,
            BillingAccessService billingAccessService) {
        this.goalRepository = goalRepository;
        this.scenarioRepository = scenarioRepository;
        this.goalRunRepository = goalRunRepository;
        this.accountRepository = accountRepository;
        this.goalMapper = goalMapper;
        this.timeService = timeService;
        this.analyticsEventPublisher = analyticsEventPublisher;
        this.billingAccessService = billingAccessService;
    }

    @Override
    public GoalResponse create(UUID currentUserId, CreateGoalRequest request) {
        validateFutureDate(request.getTargetDate());
        validateInputsType(request.getType(), request.getInputs());
        billingAccessService.enforceGoalCreationLimit(currentUserId);
        ensureLinkedAccountOwned(currentUserId, request.getLinkedAccountId());

        GoalEntity goal = goalMapper.toEntity(request);
        goal.setUserId(currentUserId);
        goal.setTitle(normalizeTitle(request.getTitle()));
        goal.setCurrency(normalizeCurrency(request.getCurrency()));
        goal.setStatus(GoalStatus.ACTIVE);

        GoalEntity saved = goalRepository.save(goal);

        ScenarioEntity baseline = ScenarioEntity.builder()
                .goalId(saved.getId())
                .name(BASELINE_SCENARIO_NAME)
                .inputsJson(saved.getInputsJson())
                .isBaseline(true)
                .build();
        scenarioRepository.save(baseline);

        return goalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GoalResponse> getAll(UUID currentUserId, GoalStatus status, GoalType type, Pageable pageable) {
        Page<GoalEntity> page;
        if (status != null && type != null) {
            page = goalRepository.findAllByUserIdAndStatusAndTypeAndDeletedAtIsNull(currentUserId, status, type, pageable);
        } else if (status != null) {
            page = goalRepository.findAllByUserIdAndStatusAndDeletedAtIsNull(currentUserId, status, pageable);
        } else if (type != null) {
            page = goalRepository.findAllByUserIdAndTypeAndDeletedAtIsNull(currentUserId, type, pageable);
        } else {
            page = goalRepository.findAllByUserIdAndDeletedAtIsNull(currentUserId, pageable);
        }
        return page.map(goalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public GoalDetailResponse getById(UUID currentUserId, UUID goalId) {
        GoalEntity goal = findOwnedGoal(currentUserId, goalId);
        GoalResponse response = goalMapper.toResponse(goal);
        List<ScenarioResponse> scenarios = scenarioRepository.findAllByGoalIdOrderByCreatedAtAsc(goalId).stream()
                .map(goalMapper::toResponse)
                .toList();
        GoalRunResponse latestRun = goalRunRepository.findTopByGoalIdOrderByCreatedAtDesc(goalId)
                .map(goalMapper::toResponse)
                .orElse(null);

        return GoalDetailResponse.builder()
                .id(response.getId())
                .type(response.getType())
                .title(response.getTitle())
                .targetAmount(response.getTargetAmount())
                .currency(response.getCurrency())
                .targetDate(response.getTargetDate())
                .linkedAccountId(response.getLinkedAccountId())
                .status(response.getStatus())
                .inputs(response.getInputs())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .scenarios(scenarios)
                .latestRun(latestRun)
                .build();
    }

    @Override
    public GoalResponse update(UUID currentUserId, UUID goalId, UpdateGoalRequest request) {
        GoalEntity goal = findOwnedGoal(currentUserId, goalId);

        if (request.getTitle() != null) {
            goal.setTitle(normalizeTitle(request.getTitle()));
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getCurrency() != null) {
            goal.setCurrency(normalizeCurrency(request.getCurrency()));
        }
        if (request.getTargetDate() != null) {
            validateFutureDate(request.getTargetDate());
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getLinkedAccountId() != null) {
            ensureLinkedAccountOwned(currentUserId, request.getLinkedAccountId());
            goal.setLinkedAccountId(request.getLinkedAccountId());
        }
        if (request.getInputs() != null) {
            validateInputsType(goal.getType(), request.getInputs());
            goal.setInputsJson(goalMapper.jsonNodeToString(request.getInputs()));
        }

        GoalEntity saved = goalRepository.save(goal);
        return goalMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID goalId) {
        GoalEntity goal = findOwnedGoal(currentUserId, goalId);
        goal.setDeletedAt(OffsetDateTime.now());
        goal.setStatus(GoalStatus.ABANDONED);
        goalRepository.save(goal);
    }

    @Override
    public ScenarioResponse createScenario(UUID currentUserId, UUID goalId, CreateScenarioRequest request) {
        GoalEntity goal = findOwnedGoal(currentUserId, goalId);
        validateInputsType(goal.getType(), request.getInputs());

        boolean makeBaseline = Boolean.TRUE.equals(request.getIsBaseline())
                || scenarioRepository.findByGoalIdAndIsBaselineTrue(goalId).isEmpty();

        if (makeBaseline) {
            scenarioRepository.findByGoalIdAndIsBaselineTrue(goalId).ifPresent(existing -> {
                existing.setIsBaseline(false);
                scenarioRepository.save(existing);
            });
        }

        ScenarioEntity scenario = ScenarioEntity.builder()
                .goalId(goalId)
                .name(request.getName().trim())
                .inputsJson(goalMapper.jsonNodeToString(request.getInputs()))
                .isBaseline(makeBaseline)
                .build();

        return goalMapper.toResponse(scenarioRepository.save(scenario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScenarioResponse> listScenarios(UUID currentUserId, UUID goalId) {
        findOwnedGoal(currentUserId, goalId);
        return scenarioRepository.findAllByGoalIdOrderByCreatedAtAsc(goalId).stream()
                .map(goalMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GoalRunResponse> listRuns(UUID currentUserId, UUID goalId, Pageable pageable) {
        findOwnedGoal(currentUserId, goalId);
        return goalRunRepository.findAllByGoalIdOrderByCreatedAtDesc(goalId, pageable)
                .map(goalMapper::toResponse);
    }

    @Override
    public GoalResponse updateStatus(UUID currentUserId, UUID goalId, GoalStatus status) {
        GoalEntity goal = findOwnedGoal(currentUserId, goalId);
        if (!isAllowedTransition(goal.getStatus(), status)) {
            throw new InvalidGoalStatusTransitionException(goalId, goal.getStatus(), status);
        }
        goal.setStatus(status);
        GoalResponse response = goalMapper.toResponse(goalRepository.save(goal));
        if (status == GoalStatus.ACHIEVED) {
            analyticsEventPublisher.publish(new AnalyticsEvent(
                    "goal_achieved",
                    Map.of("goal_id", goalId.toString(), "goal_type", goal.getType().name())));
        }
        return response;
    }

    private GoalEntity findOwnedGoal(UUID currentUserId, UUID goalId) {
        return goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, currentUserId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
    }

    private void validateFutureDate(LocalDate targetDate) {
        if (targetDate == null || !targetDate.isAfter(timeService.today())) {
            throw new InvalidGoalDateException(targetDate);
        }
    }

    private void ensureLinkedAccountOwned(UUID currentUserId, UUID linkedAccountId) {
        if (linkedAccountId == null) {
            return;
        }
        if (!accountRepository.existsByIdAndUserIdAndActiveTrue(linkedAccountId, currentUserId)) {
            throw new LinkedAccountNotFoundException(linkedAccountId);
        }
    }

    private void validateInputsType(GoalType expectedType, JsonNode inputs) {
        if (inputs == null || !inputs.isObject()) {
            throw new InvalidGoalTypeException(expectedType, null);
        }

        JsonNode versionNode = inputs.get("version");
        JsonNode typeNode = inputs.get("type");
        JsonNode valuesNode = inputs.get("values");
        if (versionNode == null || !versionNode.canConvertToInt() || typeNode == null || typeNode.isNull() || valuesNode == null) {
            throw new InvalidGoalTypeException(expectedType, typeNode == null ? null : typeNode.asText());
        }

        String actualType = typeNode.asText();
        if (!expectedType.name().equals(actualType)) {
            throw new InvalidGoalTypeException(expectedType, actualType);
        }
    }

    private boolean isAllowedTransition(GoalStatus currentStatus, GoalStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return true;
        }

        return switch (currentStatus) {
            case DRAFT -> nextStatus == GoalStatus.ACTIVE || nextStatus == GoalStatus.ABANDONED;
            case ACTIVE -> nextStatus == GoalStatus.ACHIEVED || nextStatus == GoalStatus.ABANDONED;
            case ACHIEVED, ABANDONED -> false;
        };
    }

    private String normalizeTitle(String title) {
        return title == null ? null : title.trim();
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }
}
