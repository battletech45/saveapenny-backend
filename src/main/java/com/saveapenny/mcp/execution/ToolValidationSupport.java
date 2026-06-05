package com.saveapenny.mcp.execution;

import com.saveapenny.mcp.error.ToolError;
import com.saveapenny.mcp.error.ToolValidationException;
import java.util.List;

public final class ToolValidationSupport {

    private ToolValidationSupport() {
    }

    public static void requirePositiveInteger(Integer value, String fieldName, String description) {
        if (value != null && value < 1) {
            throw new ToolValidationException(
                    description + " must be greater than 0.",
                    List.of(new ToolError(null, description + " must be greater than 0.", fieldName)));
        }
    }
}
