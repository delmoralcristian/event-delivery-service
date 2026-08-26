package com.delmoralcristian.notifier.config;

import com.delmoralcristian.notifier.infrastructure.adapter.in.web.interceptor.ApiKeyInterceptor;
import com.delmoralcristian.notifier.infrastructure.adapter.in.web.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
            .addPathPatterns("/notification_events/**");

        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/notification_events/*/replay");
    }
}
