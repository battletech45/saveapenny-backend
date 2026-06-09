package com.saveapenny.mcp.goal;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record WhatIfToolInput(UUID goalId, JsonNode overrides) {}
