package com.saveapenny.mcp.definition;

import java.util.List;

public record ToolSchemaDefinition(
        String name,
        ToolDataType type,
        String description,
        List<ToolPropertyDefinition> properties) {

    public ToolSchemaDefinition {
        properties = properties == null ? List.of() : List.copyOf(properties);
    }
}
