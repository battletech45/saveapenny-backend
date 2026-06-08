package com.saveapenny.goal.simulation;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class SimulationResult {

    private int version;
    private GoalType type;
    private Feasibility feasibility;
    private OffsetDateTime asOf;
    private int horizonMonths;
    private String currency;

    @Builder.Default
    private Map<String, Object> summary = new LinkedHashMap<>();

    @Builder.Default
    private AssumptionSet assumptions = AssumptionSet.builder().build();

    @Builder.Default
    private List<SimulationWarning> warnings = new ArrayList<>();

    @Builder.Default
    private List<MonthlyProjectionPoint> series = new ArrayList<>();
}
