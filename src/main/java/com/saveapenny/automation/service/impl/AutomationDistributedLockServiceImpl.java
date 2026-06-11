package com.saveapenny.automation.service.impl;

import com.saveapenny.automation.service.AutomationDistributedLockService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AutomationDistributedLockServiceImpl implements AutomationDistributedLockService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, ReentrantLock> fallbackLocks = new ConcurrentHashMap<>();

    public AutomationDistributedLockServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryLock(String lockName) {
        Long key = lockKey(lockName);
        try {
            Boolean acquired = jdbcTemplate.queryForObject("select pg_try_advisory_lock(?)", Boolean.class, key);
            return Boolean.TRUE.equals(acquired);
        } catch (DataAccessException ex) {
            return fallbackLocks.computeIfAbsent(lockName, ignored -> new ReentrantLock()).tryLock();
        }
    }

    @Override
    public void unlock(String lockName) {
        Long key = lockKey(lockName);
        try {
            jdbcTemplate.queryForObject("select pg_advisory_unlock(?)", Boolean.class, key);
        } catch (DataAccessException ex) {
            ReentrantLock lock = fallbackLocks.get(lockName);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private long lockKey(String lockName) {
        if (lockName == null) {
            return 0L;
        }
        long hash = 0xcbf29ce484222325L;
        for (byte value : lockName.getBytes(StandardCharsets.UTF_8)) {
            hash ^= value;
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
