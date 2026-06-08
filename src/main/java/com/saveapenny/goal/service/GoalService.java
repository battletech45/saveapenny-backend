package com.saveapenny.goal.service;

import com.saveapenny.goal.dto.CreateGoalRequest;
import com.saveapenny.goal.dto.CreateScenarioRequest;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.dto.UpdateGoalRequest;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GoalService {

    GoalResponse create(UUID currentUserId, CreateGoalRequest request);

    Page<GoalResponse> getAll(UUID currentUserId, GoalStatus status, GoalType type, Pageable pageable);

    GoalDetailResponse getById(UUID currentUserId, UUID goalId);

    GoalResponse update(UUID currentUserId, UUID goalId, UpdateGoalRequest request);

    void delete(UUID currentUserId, UUID goalId);

    ScenarioResponse createScenario(UUID currentUserId, UUID goalId, CreateScenarioRequest request);

    List<ScenarioResponse> listScenarios(UUID currentUserId, UUID goalId);

    Page<GoalRunResponse> listRuns(UUID currentUserId, UUID goalId, Pageable pageable);

    GoalResponse updateStatus(UUID currentUserId, UUID goalId, GoalStatus status);
}
