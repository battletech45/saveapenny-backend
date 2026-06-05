package com.saveapenny.mcp.error;

public record ToolError(
        ToolErrorCode code,
        String message,
        String field) {

    public ToolError {
        code = code == null ? ToolErrorCode.VALIDATION_ERROR : code;
    }
}
