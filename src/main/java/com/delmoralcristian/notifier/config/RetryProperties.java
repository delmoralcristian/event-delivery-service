package com.delmoralcristian.notifier.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "retry")
@Getter
@Setter
public class RetryProperties {

    private Integer maxAttempts;
    private Long backoffDelay;

}
