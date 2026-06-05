package com.saveapenny.mcp.registry;

import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.execution.ToolHandler;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class InMemoryToolRegistry implements ToolRegistry {

    private final Map<String, ToolHandler<?, ?>> handlersByName;
    private final List<ToolDefinition> definitions;

    public InMemoryToolRegistry(List<ToolHandler<?, ?>> handlers) {
        Map<String, ToolHandler<?, ?>> indexedHandlers = new LinkedHashMap<>();
        for (ToolHandler<?, ?> handler : handlers) {
            String toolName = handler.definition().name();
            if (indexedHandlers.putIfAbsent(toolName, handler) != null) {
                throw new IllegalStateException("Duplicate MCP tool handler registered for name: " + toolName);
            }
        }
        this.handlersByName = Map.copyOf(indexedHandlers);
        this.definitions = indexedHandlers.values().stream()
                .map(ToolHandler::definition)
                .sorted(Comparator.comparing(ToolDefinition::name))
                .toList();
    }

    @Override
    public Optional<ToolHandler<?, ?>> findByName(String name) {
        return Optional.ofNullable(handlersByName.get(name));
    }

    @Override
    public List<ToolDefinition> getDefinitions() {
        return definitions;
    }
}
