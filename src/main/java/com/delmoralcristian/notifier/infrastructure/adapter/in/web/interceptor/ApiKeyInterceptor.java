package com.delmoralcristian.notifier.infrastructure.adapter.in.web.interceptor;

import com.delmoralcristian.notifier.application.port.out.ClientPersistencePort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";

    @Value("${api.security.key}")
    private String globalApiKey;

    private final ClientPersistencePort clientPersistencePort;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {

        var providedKey = request.getHeader(API_KEY_HEADER);
        if (providedKey == null || providedKey.isBlank()) {
            return unauthorized(response);
        }

        var clientId = request.getParameter("clientId");
        boolean valid = clientId != null
            ? clientPersistencePort.existsByIdAndApiKey(clientId, providedKey)
            : globalApiKey.equals(providedKey) || clientPersistencePort.existsByApiKey(providedKey);

        if (valid) {
            return true;
        }

        return unauthorized(response);
    }

    private boolean unauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("""
            {"status":401,"error":"Unauthorized","message":"Invalid or missing API key"}
            """);
        return false;
    }
}
