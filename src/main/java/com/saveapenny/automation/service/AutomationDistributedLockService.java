package com.saveapenny.automation.service;

public interface AutomationDistributedLockService {

    boolean tryLock(String lockName);

    void unlock(String lockName);
}
