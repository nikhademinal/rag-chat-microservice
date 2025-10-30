package com.ragchat.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ragchat.dto.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(2)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // FIXED: Configure ObjectMapper with JavaTimeModule for LocalDateTime support
    private final ObjectMapper objectMapper;

    @Value("${rate.limit.capacity}")
    private long capacity;

    @Value("${rate.limit.refill.tokens}")
    private long refillTokens;

    @Value("${rate.limit.refill.duration}")
    private long refillDuration;

    public RateLimitingFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            apiKey = request.getRemoteAddr(); // Use IP address as fallback
        }

        Bucket bucket = cache.computeIfAbsent(apiKey, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            log.debug("Request allowed for key: {}", apiKey);
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {}", apiKey);
            sendRateLimitErrorResponse(response);
        }
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.intervally(refillTokens, Duration.ofSeconds(refillDuration)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private void sendRateLimitErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Object> apiResponse = ApiResponse.error(
                "Rate limit exceeded. Please try again later.");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}