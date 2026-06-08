package com.saveapenny.goal.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.dto.CompareScenariosRequest;
import com.saveapenny.goal.simulation.dto.DraftGoalSimulationRequest;
import com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse;
import com.saveapenny.goal.simulation.dto.GoalSimulationPromptRequest;
import com.saveapenny.goal.simulation.dto.GoalSimulationResponse;
import com.saveapenny.goal.simulation.dto.GoalWhatIfResponse;
import com.saveapenny.goal.simulation.dto.WhatIfRequest;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Goal Simulation", description = "Draft and existing-goal simulation endpoints.")
public class GoalSimulationController {

    private final GoalSimulationService goalSimulationService;

    public GoalSimulationController(GoalSimulationService goalSimulationService) {
        this.goalSimulationService = goalSimulationService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<GoalSimulationResponse>> simulatePrompt(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody GoalSimulationPromptRequest request) {
        return ResponseEntity.ok(ApiResponse.success(goalSimulationService.simulatePrompt(getCurrentUserId(principal), request.getPrompt())));
    }

    @PostMapping("/simulate/draft")
    public ResponseEntity<ApiResponse<GoalSimulationResponse>> simulateDraft(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody DraftGoalSimulationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(goalSimulationService.simulateDraft(getCurrentUserId(principal), request)));
    }

    @PostMapping("/{goalId}/simulate")
    public ResponseEntity<ApiResponse<SimulationResult>> simulateExistingGoal(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId) {
        return ResponseEntity.ok(ApiResponse.success(goalSimulationService.simulateGoal(getCurrentUserId(principal), goalId, null)));
    }

    @PostMapping("/{goalId}/scenarios/compare")
    public ResponseEntity<ApiResponse<GoalScenarioComparisonResponse>> compareScenarios(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody CompareScenariosRequest request) {
        return ResponseEntity.ok(ApiResponse.success(goalSimulationService.compareScenarios(getCurrentUserId(principal), goalId, request)));
    }

    @PostMapping("/{goalId}/what-if")
    public ResponseEntity<ApiResponse<GoalWhatIfResponse>> whatIf(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody WhatIfRequest request) {
        return ResponseEntity.ok(ApiResponse.success(goalSimulationService.whatIf(getCurrentUserId(principal), goalId, request)));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
