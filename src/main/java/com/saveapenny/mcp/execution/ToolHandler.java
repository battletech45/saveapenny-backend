package com.saveapenny.mcp.execution;

import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.error.ToolErrorCode;
import com.saveapenny.mcp.error.ToolExecutionException;

public interface ToolHandler<I, O> {

    ToolDefinition definition();

    default void validate(ToolExecutionContext context, I input) {
    }

    default ToolResult<O> execute(ToolExecutionContext context, I input) {
        validate(context, input);
        try {
            return doExecute(context, input);
        } catch (ToolExecutionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ToolExecutionException(
                    ToolErrorCode.TOOL_EXECUTION_FAILED,
                    "Tool execution failed for " + definition().name(),
                    ex);
        }
    }

    ToolResult<O> doExecute(ToolExecutionContext context, I input);
}
