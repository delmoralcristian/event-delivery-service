package com.delmoralcristian.notifier.advice;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ProcessingTimeTrackerAdvice {

    @Around("@annotation(com.delmoralcristian.notifier.advice.TrackProcessingTime)")
    public Object processingTrackTime(ProceedingJoinPoint joinPoint) throws Throwable {
        var start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            var elapsedMs = System.currentTimeMillis() - start;
            log.info("method={} elapsed={}ms", joinPoint.getSignature().toShortString(), elapsedMs);
        }
    }
}
