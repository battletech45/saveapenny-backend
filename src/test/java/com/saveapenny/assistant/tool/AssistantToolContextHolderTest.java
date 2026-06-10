package com.saveapenny.assistant.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssistantToolContextHolderTest {

    private AssistantToolContextHolder holder;

    @BeforeEach
    void setUp() {
        holder = new AssistantToolContextHolder();
    }

    @Test
    void setAndRequire_returnsSameUserId() {
        UUID userId = UUID.randomUUID();
        holder.setCurrentUserId(userId);
        assertEquals(userId, holder.requireCurrentUserId());
    }

    @Test
    void requireCurrentUserId_whenNotSet_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                holder::requireCurrentUserId);
        assertEquals("Assistant tool context is missing authenticated user id.", ex.getMessage());
    }

    @Test
    void clear_removesUserId() {
        holder.setCurrentUserId(UUID.randomUUID());
        holder.clear();

        assertThrows(IllegalStateException.class, holder::requireCurrentUserId);
    }

    @Test
    void threadsAreIsolated() throws Exception {
        UUID mainThreadId = UUID.randomUUID();
        holder.setCurrentUserId(mainThreadId);

        CompletableFuture<Boolean> otherThreadHasValue = CompletableFuture.supplyAsync(() -> {
            try {
                holder.requireCurrentUserId();
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
        });

        assertFalse(otherThreadHasValue.get());
        assertEquals(mainThreadId, holder.requireCurrentUserId());
    }
}
