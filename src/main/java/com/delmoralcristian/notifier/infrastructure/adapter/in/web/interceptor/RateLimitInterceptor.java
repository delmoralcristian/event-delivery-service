package com.delmoralcristian.notifier.infrastructure.adapter.in.web.interceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${rate-limit.max-requests-per-minute}")
    private int maxRequestsPerMinute;

    @Value("${rate-limit.tokens-per-request}")
    private int tokensPerRequest;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {

        var key = resolveKey(request);
        var bucket = buckets.computeIfAbsent(key, k -> newBucket());

        if (bucket.tryConsume(tokensPerRequest)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("""
            {"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Max %d replay requests per minute."}
            """.formatted(maxRequestsPerMinute));
        return false;
    }

    private Bucket newBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(maxRequestsPerMinute)
                .refillGreedy(maxRequestsPerMinute, Duration.ofMinutes(1))
                .build())
            .build();
    }

    private String resolveKey(HttpServletRequest request) {
        var apiKey = request.getHeader(ApiKeyInterceptor.API_KEY_HEADER);
        return apiKey != null ? apiKey : request.getRemoteAddr();
    }
}
