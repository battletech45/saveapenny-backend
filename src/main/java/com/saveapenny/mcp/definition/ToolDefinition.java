package com.saveapenny.mcp.definition;

public record ToolDefinition(
        String name,
        String description,
        ToolSchemaDefinition inputSchema,
        ToolSchemaDefinition outputSchema,
        Class<?> inputType,
        Class<?> outputType) {
}
