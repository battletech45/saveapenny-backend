package com.saveapenny.mcp.definition;

public record ToolPropertyDefinition(
        String name,
        ToolDataType type,
        String description,
        boolean required) {
}
