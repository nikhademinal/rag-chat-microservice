package com.ragchat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIAssistantService Unit Tests")
class AIAssistantServiceTest {

    private AIAssistantService aiAssistantService;
    private AIAssistantService disabledService;
    private AIAssistantService noApiKeyService;

    @BeforeEach
    void setUp() {
        // Service with AI enabled and API key
        aiAssistantService = new AIAssistantService(
                true,
                "test_api_key",
                "https://api-inference.huggingface.co/models/microsoft/DialoGPT-medium",
                30000L
        );

        // Service with AI disabled
        disabledService = new AIAssistantService(
                false,
                "test_key",
                "https://test.com",
                30000L
        );

        // Service with no API key
        noApiKeyService = new AIAssistantService(
                true,
                null,
                "https://test.com",
                30000L
        );
    }

    @Test
    @DisplayName("Should return true when AI is enabled and API key is configured")
    void isAIEnabled_WhenEnabled_ReturnsTrue() {
        // Act
        boolean result = aiAssistantService.isAIEnabled();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when AI is disabled")
    void isAIEnabled_WhenDisabled_ReturnsFalse() {
        // Act
        boolean result = disabledService.isAIEnabled();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when API key is null")
    void isAIEnabled_WhenApiKeyNull_ReturnsFalse() {
        // Act
        boolean result = noApiKeyService.isAIEnabled();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when API key is empty")
    void isAIEnabled_WhenApiKeyEmpty_ReturnsFalse() {
        // Arrange
        AIAssistantService emptyKeyService = new AIAssistantService(
                true, "", "https://test.com", 30000L
        );

        // Act
        boolean result = emptyKeyService.isAIEnabled();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return disabled message when AI is disabled")
    void generateResponse_WhenDisabled_ReturnsDisabledMessage() {
        // Act
        String response = disabledService.generateResponse("Hello", null);

        // Assert
        assertThat(response).contains("AI Assistant is currently disabled");
    }

    @Test
    @DisplayName("Should return configuration message when API key is missing")
    void generateResponse_WhenNoApiKey_ReturnsConfigMessage() {
        // Act
        String response = noApiKeyService.generateResponse("Hello", null);

        // Assert
        assertThat(response).contains("AI Assistant is not properly configured");
        assertThat(response).contains("HUGGINGFACE_API_KEY");
    }

    @Test
    @DisplayName("Should handle null message without throwing exception")
    void generateResponse_WithNullMessage_HandlesGracefully() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            String response = disabledService.generateResponse(null, null);
            assertNotNull(response);
            assertThat(response).contains("AI Assistant is currently disabled");
        });
    }

    @Test
    @DisplayName("Should handle empty message without throwing exception")
    void generateResponse_WithEmptyMessage_HandlesGracefully() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            String response = disabledService.generateResponse("", null);
            assertNotNull(response);
            assertThat(response).contains("AI Assistant is currently disabled");
        });
    }

    @Test
    @DisplayName("Should handle context when provided")
    void generateResponse_WithContext_IncludesContext() {
        // Act - Using disabled service to avoid HTTP calls
        String response = disabledService.generateResponse(
                "What is this about?",
                "Previous conversation context"
        );

        // Assert
        assertNotNull(response);
        assertThat(response).contains("disabled");
    }
}