package com.saveapenny.goal.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.goal.dto.CreateGoalRequest;
import com.saveapenny.goal.dto.CreateScenarioRequest;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.dto.UpdateGoalRequest;
import com.saveapenny.goal.dto.UpdateGoalStatusRequest;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.service.GoalService;
import com.saveapenny.shared.api.ApiResponse;
import com.saveapenny.shared.api.PagedResponse;
import com.saveapenny.shared.api.PagedResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Goals", description = "Goal CRUD, scenarios, and run history endpoints.")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> create(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody CreateGoalRequest request) {
        GoalResponse response = goalService.create(getCurrentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List goals", description = "Returns paginated goals with optional status and type filters.")
    public ResponseEntity<ApiResponse<PagedResponse<GoalResponse>>> getAll(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) GoalStatus status,
            @RequestParam(required = false) GoalType type,
            @ParameterObject Pageable pageable) {
        Page<GoalResponse> response = goalService.getAll(getCurrentUserId(principal), status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponses.from(response)));
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalDetailResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId) {
        GoalDetailResponse response = goalService.getById(getCurrentUserId(principal), goalId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalResponse>> update(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request) {
        GoalResponse response = goalService.update(getCurrentUserId(principal), goalId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId) {
        goalService.delete(getCurrentUserId(principal), goalId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{goalId}/status")
    public ResponseEntity<ApiResponse<GoalResponse>> updateStatus(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalStatusRequest request) {
        GoalResponse response = goalService.updateStatus(getCurrentUserId(principal), goalId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{goalId}/scenarios")
    public ResponseEntity<ApiResponse<ScenarioResponse>> createScenario(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateScenarioRequest request) {
        ScenarioResponse response = goalService.createScenario(getCurrentUserId(principal), goalId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{goalId}/scenarios")
    public ResponseEntity<ApiResponse<List<ScenarioResponse>>> listScenarios(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId) {
        List<ScenarioResponse> response = goalService.listScenarios(getCurrentUserId(principal), goalId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{goalId}/runs")
    public ResponseEntity<ApiResponse<PagedResponse<GoalRunResponse>>> listRuns(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID goalId,
            @ParameterObject Pageable pageable) {
        Page<GoalRunResponse> response = goalService.listRuns(getCurrentUserId(principal), goalId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponses.from(response)));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
