package com.saveapenny.mcp.execution;

public record ToolResult<T>(T data) {

    public static <T> ToolResult<T> of(T data) {
        return new ToolResult<>(data);
    }
}
