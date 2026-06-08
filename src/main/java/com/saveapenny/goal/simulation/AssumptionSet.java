package com.saveapenny.goal.simulation;

import java.util.LinkedHashMap;
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
public class AssumptionSet {

    @Builder.Default
    private Map<String, Object> values = new LinkedHashMap<>();
}
