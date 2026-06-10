package com.saveapenny.automation.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AutomationDistributedLockServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AutomationDistributedLockServiceImpl lockService;

    @BeforeEach
    void setUp() {
        lockService = new AutomationDistributedLockServiceImpl(jdbcTemplate);
    }

    @Test
    void tryLock_whenPgLockAcquired_returnsTrue() {
        when(jdbcTemplate.queryForObject(eq("select pg_try_advisory_lock(?)"), eq(Boolean.class), any(Long.class)))
                .thenReturn(true);

        boolean result = lockService.tryLock("my-lock");

        assertTrue(result);
    }

    @Test
    void tryLock_whenPgLockNotAcquired_returnsFalse() {
        when(jdbcTemplate.queryForObject(eq("select pg_try_advisory_lock(?)"), eq(Boolean.class), any(Long.class)))
                .thenReturn(false);

        boolean result = lockService.tryLock("my-lock");

        assertFalse(result);
    }

    @Test
    void tryLock_whenPgFails_usesFallbackLock() {
        when(jdbcTemplate.queryForObject(eq("select pg_try_advisory_lock(?)"), eq(Boolean.class), any(Long.class)))
                .thenThrow(new DataAccessException("db down") {});

        boolean result = lockService.tryLock("my-lock");

        assertTrue(result);
    }

    @Test
    void tryLock_sameNameReusesFallbackLock() throws Exception {
        when(jdbcTemplate.queryForObject(eq("select pg_try_advisory_lock(?)"), eq(Boolean.class), any(Long.class)))
                .thenThrow(new DataAccessException("db down") {});

        assertTrue(lockService.tryLock("same-lock"));

        boolean acquiredByOtherThread = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> lockService.tryLock("same-lock")).get();

        assertFalse(acquiredByOtherThread);
    }

    @Test
    void unlock_callsPgAdvisoryUnlock() {
        when(jdbcTemplate.queryForObject(eq("select pg_try_advisory_lock(?)"), eq(Boolean.class), any(Long.class)))
                .thenReturn(true);
        lockService.tryLock("my-lock");

        lockService.unlock("my-lock");

        verify(jdbcTemplate).queryForObject(eq("select pg_advisory_unlock(?)"), eq(Boolean.class), any(Long.class));
    }

    @Test
    void unlock_whenPgFails_usesFallbackUnlock() {
        when(jdbcTemplate.queryForObject(eq("select pg_try_advisory_lock(?)"), eq(Boolean.class), any(Long.class)))
                .thenThrow(new DataAccessException("db down") {});
        assertTrue(lockService.tryLock("my-lock"));

        when(jdbcTemplate.queryForObject(eq("select pg_advisory_unlock(?)"), eq(Boolean.class), any(Long.class)))
                .thenThrow(new DataAccessException("db down") {});

        lockService.unlock("my-lock");

        assertTrue(lockService.tryLock("my-lock"));
    }

    @Test
    void tryLock_withNullLockName_usesZeroKey() {
        when(jdbcTemplate.queryForObject(eq("select pg_try_advisory_lock(?)"), eq(Boolean.class), eq(0L)))
                .thenReturn(true);

        boolean result = lockService.tryLock(null);

        assertTrue(result);
    }
}
