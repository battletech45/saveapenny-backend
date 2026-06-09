package com.saveapenny.mcp.goal;

import java.util.List;
import java.util.UUID;

public record CompareScenariosToolInput(UUID goalId, List<UUID> scenarioIds) {}
