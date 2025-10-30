package com.ragchat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIAssistantService {

    private final WebClient webClient;
    private final boolean aiEnabled;
    private final String apiKey;
    private final long timeout;

    public AIAssistantService(
            @Value("${ai.assistant.enabled}") boolean aiEnabled,
            @Value("${ai.assistant.api.key}") String apiKey,
            @Value("${ai.assistant.api.url}") String apiUrl,
            @Value("${ai.assistant.timeout}") long timeout) {

        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.timeout = timeout;

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String generateResponse(String userMessage, String context) {
        if (!aiEnabled) {
            log.debug("AI Assistant is disabled");
            return "AI Assistant is currently disabled.";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Hugging Face API key is not configured");
            return "AI Assistant is not properly configured. Please set HUGGINGFACE_API_KEY.";
        }

        try {
            log.info("Sending request to AI Assistant for message: {}", userMessage);

            // Build the prompt with optional context
            String userPrompt = (context != null && !context.isEmpty())
                    ? String.format("Context: %s\n\nUser: %s", context, userMessage)
                    : userMessage;

            // Build request body in the new OpenAI-style format
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "meta-llama/Meta-Llama-3-8B-Instruct"); // You can replace with your own
            requestBody.put("max_tokens", 300);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", "You are a concise and helpful AI assistant."),
                    Map.of("role", "user", "content", userPrompt)
            );

            requestBody.put("messages", messages);

            // Send request
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .onErrorResume(error -> {
                        log.error("Error calling AI Assistant: {}", error.getMessage());
                        return Mono.just("{\"error\": \"" + error.getMessage() + "\"}");
                    })
                    .block();

            log.debug("Raw AI response: {}", response);

            // Parse the response
            String aiResponse = parseChatResponse(response);
            log.info("AI Assistant response generated successfully");
            return aiResponse;

        } catch (Exception e) {
            log.error("Failed to generate AI response", e);
            return "I apologize, but I encountered an error while processing your message.";
        }
    }

    /**
     * Extracts message text from Hugging Face /chat/completions API response.
     */
    private String parseChatResponse(String response) {
        try {
            // naive but safe substring parsing — replace with Jackson/Gson in production
            if (response.contains("\"content\"")) {
                int start = response.indexOf("\"content\"") + 11;
                int end = response.indexOf("\"", start);
                if (end > start) {
                    return response.substring(start, end).trim();
                }
            }
            return "I'm here to help. Could you clarify your question?";
        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            return "I'm here to help, but I had trouble understanding the response.";
        }
    }

    public boolean isAIEnabled() {
        return aiEnabled && apiKey != null && !apiKey.isEmpty();
    }
}
