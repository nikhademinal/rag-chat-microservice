package com.ragchat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchat.dto.*;
import com.ragchat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "api.key=test-api-key-12345"
})
@DisplayName("ChatController Integration Tests")
class ChatControllerTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TEST_API_KEY = "test-api-key-12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    private SessionResponse testSessionResponse;
    private MessageResponse testMessageResponse;

    @BeforeEach
    void setUp() {
        testSessionResponse = SessionResponse.builder()
                .id(1L)
                .userId("user123")
                .title("Test Session")
                .isFavorite(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .messageCount(0)
                .build();

        testMessageResponse = MessageResponse.builder()
                .id(1L)
                .sender("USER")
                .content("Hello")
                .context("Test")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ==================== Session Creation Tests ====================

    @Test
    @DisplayName("POST /api/v1/chat/sessions - Should create session successfully")
    void createSession_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        CreateSessionRequest request = CreateSessionRequest.builder()
                .userId("user123")
                .title("New Session")
                .build();

        when(chatService.createSession(any(CreateSessionRequest.class)))
                .thenReturn(testSessionResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/sessions")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.userId", is("user123")))
                .andExpect(jsonPath("$.data.title", is("Test Session")));
    }

    @Test
    @DisplayName("POST /api/v1/chat/sessions - Should return 400 for missing userId")
    void createSession_MissingUserId_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateSessionRequest request = CreateSessionRequest.builder()
                .title("New Session")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/sessions")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @Test
    @DisplayName("POST /api/v1/chat/sessions - Should return 400 for empty title")
    void createSession_EmptyTitle_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateSessionRequest request = CreateSessionRequest.builder()
                .userId("user123")
                .title("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/sessions")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== Message Sending Tests ====================

    @Test
    @DisplayName("POST /api/v1/chat/sessions/{id}/messages - Should send message successfully")
    void sendMessage_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello!")
                .context("Greeting")
                .useAI(false)
                .build();

        when(chatService.sendMessage(eq(1L), any(SendMessageRequest.class)))
                .thenReturn(testMessageResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sender", is("USER")))
                .andExpect(jsonPath("$.data.content", is("Hello")));
    }

    @Test
    @DisplayName("POST /api/v1/chat/sessions/{id}/messages - Should return 400 for empty content")
    void sendMessage_EmptyContent_ReturnsBadRequest() throws Exception {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("")
                .useAI(false)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat/sessions/1/messages")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== Message Retrieval Tests ====================

    @Test
    @DisplayName("GET /api/v1/chat/sessions/{id}/messages - Should retrieve messages successfully")
    void getSessionMessages_ValidId_ReturnsOk() throws Exception {
        // Arrange
        List<MessageResponse> messages = Arrays.asList(
                testMessageResponse,
                MessageResponse.builder()
                        .id(2L)
                        .sender("AI_ASSISTANT")
                        .content("Hi there!")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        when(chatService.getSessionMessages(1L)).thenReturn(messages);

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/sessions/1/messages")
                        .header(API_KEY_HEADER, TEST_API_KEY))  // ADDED: API Key
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].sender", is("USER")))
                .andExpect(jsonPath("$.data[1].sender", is("AI_ASSISTANT")));
    }

    @Test
    @DisplayName("GET /api/v1/chat/sessions/{id}/messages/paginated - Should retrieve paginated messages")
    void getSessionMessagesPaginated_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        PageResponse<MessageResponse> pageResponse = PageResponse.<MessageResponse>builder()
                .content(Arrays.asList(testMessageResponse))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .last(true)
                .build();

        when(chatService.getSessionMessagesPaginated(1L, 0, 20)).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/sessions/1/messages/paginated")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.size", is(20)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    // ==================== Session Retrieval Tests ====================

    @Test
    @DisplayName("GET /api/v1/chat/users/{userId}/sessions - Should retrieve user sessions")
    void getUserSessions_ValidUserId_ReturnsOk() throws Exception {
        // Arrange
        List<SessionResponse> sessions = Arrays.asList(
                testSessionResponse,
                SessionResponse.builder()
                        .id(2L)
                        .userId("user123")
                        .title("Session 2")
                        .isFavorite(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .messageCount(5)
                        .build()
        );

        when(chatService.getUserSessions("user123")).thenReturn(sessions);

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/users/user123/sessions")
                        .header(API_KEY_HEADER, TEST_API_KEY))  // ADDED: API Key
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].title", is("Test Session")))
                .andExpect(jsonPath("$.data[1].title", is("Session 2")));
    }

    @Test
    @DisplayName("GET /api/v1/chat/users/{userId}/sessions/favorites - Should retrieve favorites")
    void getFavoriteSessions_ValidUserId_ReturnsOk() throws Exception {
        // Arrange
        SessionResponse favoriteSession = SessionResponse.builder()
                .id(1L)
                .userId("user123")
                .title("Favorite")
                .isFavorite(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .messageCount(10)
                .build();

        when(chatService.getFavoriteSessions("user123"))
                .thenReturn(Arrays.asList(favoriteSession));

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/users/user123/sessions/favorites")
                        .header(API_KEY_HEADER, TEST_API_KEY))  // ADDED: API Key
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].isFavorite", is(true)));
    }

    // ==================== Session Update Tests ====================

    @Test
    @DisplayName("PUT /api/v1/chat/sessions/{id}/rename - Should rename session")
    void renameSession_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        RenameSessionRequest request = RenameSessionRequest.builder()
                .newTitle("Updated Title")
                .build();

        SessionResponse updatedSession = SessionResponse.builder()
                .id(1L)
                .userId("user123")
                .title("Updated Title")
                .isFavorite(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .messageCount(0)
                .build();

        when(chatService.renameSession(eq(1L), any(RenameSessionRequest.class)))
                .thenReturn(updatedSession);

        // Act & Assert
        mockMvc.perform(put("/api/v1/chat/sessions/1/rename")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Updated Title")));
    }

    @Test
    @DisplayName("PUT /api/v1/chat/sessions/{id}/favorite - Should toggle favorite")
    void toggleFavorite_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        ToggleFavoriteRequest request = ToggleFavoriteRequest.builder()
                .isFavorite(true)
                .build();

        SessionResponse favoriteSession = SessionResponse.builder()
                .id(1L)
                .userId("user123")
                .title("Test Session")
                .isFavorite(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .messageCount(0)
                .build();

        when(chatService.toggleFavorite(eq(1L), any(ToggleFavoriteRequest.class)))
                .thenReturn(favoriteSession);

        // Act & Assert
        mockMvc.perform(put("/api/v1/chat/sessions/1/favorite")
                        .header(API_KEY_HEADER, TEST_API_KEY)  // ADDED: API Key
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isFavorite", is(true)));
    }

    // ==================== Session Deletion Tests ====================

    @Test
    @DisplayName("DELETE /api/v1/chat/sessions/{id} - Should delete session")
    void deleteSession_ValidId_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/chat/sessions/1")
                        .header(API_KEY_HEADER, TEST_API_KEY))  // ADDED: API Key
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Session deleted successfully")));
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should return 404 when session not found")
    void getSession_NonExistentId_ReturnsNotFound() throws Exception {
        // Arrange
        when(chatService.getSessionMessages(999L))
                .thenThrow(new com.ragchat.exception.ResourceNotFoundException(
                        "Session not found with ID: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/sessions/999/messages")
                        .header(API_KEY_HEADER, TEST_API_KEY))  // ADDED: API Key
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Session not found")));
    }

    // ==================== Authentication Tests ====================

    @Test
    @DisplayName("Should return 401 when API key is missing")
    void request_WithoutApiKey_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/users/user123/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("API key")));
    }

    @Test
    @DisplayName("Should return 401 when API key is invalid")
    void request_WithInvalidApiKey_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/users/user123/sessions")
                        .header(API_KEY_HEADER, "invalid-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Invalid API key")));
    }
}