package com.saveapenny.mcp.error;

import java.util.List;

public class ToolExecutionException extends RuntimeException {

    private final ToolErrorCode code;
    private final List<ToolError> errors;

    public ToolExecutionException(ToolErrorCode code, String message) {
        super(message);
        this.code = code;
        this.errors = List.of(new ToolError(code, message, null));
    }

    public ToolExecutionException(ToolErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errors = List.of(new ToolError(code, message, null));
    }

    public ToolExecutionException(ToolErrorCode code, String message, List<ToolError> errors) {
        super(message);
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public ToolErrorCode getCode() {
        return code;
    }

    public List<ToolError> getErrors() {
        return errors;
    }
}
