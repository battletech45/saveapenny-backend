package com.saveapenny.mcp.execution;

@FunctionalInterface
public interface ToolInputValidator<T> {

    void validate(T input);
}
