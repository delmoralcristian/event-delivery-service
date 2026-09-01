package com.delmoralcristian.notifier.utils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {

    private static final String LOCK_KEY = "LOCK_%s_BY_REFERENCE_%s";
    private static final long WAIT_SECONDS = 1L;

    @Value("${redis.lock.lease-time-seconds:60}")
    private long leaseTimeSeconds;

    private final RedissonClient redissonClient;

    public boolean executeWithLock(String type, String id, Runnable action) {
        var key = LOCK_KEY.formatted(type, id);
        var lock = redissonClient.getLock(key);
        try {
            if (!lock.tryLock(WAIT_SECONDS, leaseTimeSeconds, TimeUnit.SECONDS)) {
                log.warn("[LOCK] Could not acquire lock for key {} — another instance is processing it", key);
                return false;
            }
            log.debug("[LOCK] Acquired lock for key {}", key);
            action.run();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[LOCK] Interrupted while waiting for lock {}", key);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[LOCK] Released lock for key {}", key);
            }
        }
    }
}
