package com.saveapenny.goal.dto;

import com.saveapenny.goal.entity.GoalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGoalStatusRequest {

    @NotNull
    private GoalStatus status;
}
