package com.saveapenny.goal.simulation.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
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
public class CompareScenariosRequest {

    @Size(max = 10)
    private List<UUID> scenarioIds;
}
