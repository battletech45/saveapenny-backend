package com.saveapenny.mcp.registry;

import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.execution.ToolHandler;
import java.util.List;
import java.util.Optional;

public interface ToolRegistry {

    Optional<ToolHandler<?, ?>> findByName(String name);

    List<ToolDefinition> getDefinitions();
}
