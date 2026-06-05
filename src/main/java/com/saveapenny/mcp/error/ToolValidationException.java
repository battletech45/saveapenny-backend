package com.saveapenny.mcp.error;

import java.util.List;

public class ToolValidationException extends ToolExecutionException {

    public ToolValidationException(String message, List<ToolError> errors) {
        super(ToolErrorCode.VALIDATION_ERROR, message, errors);
    }
}
