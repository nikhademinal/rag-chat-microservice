package com.ragchat.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ragchat.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs"
    );

    @Value("${api.key}")
    private String validApiKey;

    // FIXED: Configure ObjectMapper with JavaTimeModule for LocalDateTime support
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Skip authentication for excluded paths
        if (shouldSkipAuthentication(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Missing API key for request to: {}", requestPath);
            sendErrorResponse(response, "API key is missing. Please provide X-API-Key header.");
            return;
        }

        if (!validApiKey.equals(apiKey)) {
            log.warn("Invalid API key attempt for request to: {}", requestPath);
            sendErrorResponse(response, "Invalid API key");
            return;
        }

        log.debug("API key validated successfully for request to: {}", requestPath);
        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipAuthentication(String requestPath) {
        return EXCLUDED_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}