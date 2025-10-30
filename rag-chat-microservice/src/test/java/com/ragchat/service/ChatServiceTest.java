package com.ragchat.service;

import com.ragchat.dto.*;
import com.ragchat.entity.ChatMessage;
import com.ragchat.entity.ChatSession;
import com.ragchat.exception.ResourceNotFoundException;
import com.ragchat.repository.ChatMessageRepository;
import com.ragchat.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for ChatService
 *
 * Test Coverage:
 * - Session creation
 * - Session retrieval (all, favorites, by ID)
 * - Session updates (rename, favorite toggle)
 * - Session deletion
 * - Message sending (with and without AI)
 * - Message retrieval (all, paginated)
 * - Error handling
 * - Edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private AIAssistantService aiAssistantService;

    @InjectMocks
    private ChatService chatService;

    private ChatSession testSession;
    private ChatMessage testUserMessage;
    private ChatMessage testAIMessage;

    @BeforeEach
    void setUp() {
        testSession = ChatSession.builder()
                .id(1L)
                .userId("user123")
                .title("Test Session")
                .isFavorite(false)
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUserMessage = ChatMessage.builder()
                .id(1L)
                .session(testSession)
                .sender(ChatMessage.SenderType.USER)
                .content("Hello")
                .context("Test context")
                .timestamp(LocalDateTime.now())
                .build();

        testAIMessage = ChatMessage.builder()
                .id(2L)
                .session(testSession)
                .sender(ChatMessage.SenderType.AI_ASSISTANT)
                .content("Hi there!")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ==================== Session Creation Tests ====================

    @Test
    @DisplayName("Should create session successfully")
    void createSession_Success() {
        // Arrange
        CreateSessionRequest request = CreateSessionRequest.builder()
                .userId("user123")
                .title("New Session")
                .build();

        when(sessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        // Act
        SessionResponse response = chatService.createSession(request);

        // Assert
        assertNotNull(response);
        assertEquals(testSession.getId(), response.getId());
        assertEquals(testSession.getUserId(), response.getUserId());
        assertEquals(testSession.getTitle(), response.getTitle());
        assertFalse(response.getIsFavorite());
        assertEquals(0, response.getMessageCount());

        verify(sessionRepository, times(1)).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("Should create session with correct initial values")
    void createSession_InitialValues() {
        // Arrange
        CreateSessionRequest request = CreateSessionRequest.builder()
                .userId("newUser")
                .title("Initial Session")
                .build();

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        when(sessionRepository.save(sessionCaptor.capture())).thenReturn(testSession);

        // Act
        chatService.createSession(request);

        // Assert
        ChatSession capturedSession = sessionCaptor.getValue();
        assertEquals("newUser", capturedSession.getUserId());
        assertEquals("Initial Session", capturedSession.getTitle());
        assertFalse(capturedSession.getIsFavorite());
        assertNotNull(capturedSession.getMessages());
    }

    // ==================== Message Sending Tests ====================

    @Test
    @DisplayName("Should send message without AI successfully")
    void sendMessage_WithoutAI_Success() {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test message")
                .context("Test context")
                .useAI(false)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(testUserMessage);

        // Act
        MessageResponse response = chatService.sendMessage(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("USER", response.getSender());

        verify(sessionRepository, times(1)).findById(1L);
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
        verify(aiAssistantService, never()).generateResponse(anyString(), anyString());
    }

    @Test
    @DisplayName("Should send message with AI successfully")
    void sendMessage_WithAI_Success() {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello AI")
                .context("Greeting")
                .useAI(true)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(messageRepository.save(any(ChatMessage.class)))
                .thenReturn(testUserMessage)
                .thenReturn(testAIMessage);
        when(aiAssistantService.isAIEnabled()).thenReturn(true);
        when(aiAssistantService.generateResponse("Hello AI", "Greeting"))
                .thenReturn("Hi there!");

        // Act
        MessageResponse response = chatService.sendMessage(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("AI_ASSISTANT", response.getSender());
        assertEquals("Hi there!", response.getContent());

        verify(sessionRepository, times(1)).findById(1L);
        verify(messageRepository, times(2)).save(any(ChatMessage.class));
        verify(aiAssistantService, times(1)).generateResponse("Hello AI", "Greeting");
    }

    @Test
    @DisplayName("Should throw exception when sending message to non-existent session")
    void sendMessage_SessionNotFound() {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .useAI(false)
                .build();

        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> chatService.sendMessage(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found with ID: 999");

        verify(sessionRepository, times(1)).findById(999L);
        verify(messageRepository, never()).save(any(ChatMessage.class));
    }

    // ==================== Message Retrieval Tests ====================

    @Test
    @DisplayName("Should retrieve all messages for a session")
    void getSessionMessages_Success() {
        // Arrange
        List<ChatMessage> messages = Arrays.asList(testUserMessage, testAIMessage);

        when(sessionRepository.existsById(1L)).thenReturn(true);
        when(messageRepository.findBySessionIdOrderByTimestampAsc(1L)).thenReturn(messages);

        // Act
        List<MessageResponse> responses = chatService.getSessionMessages(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("USER", responses.get(0).getSender());
        assertEquals("AI_ASSISTANT", responses.get(1).getSender());

        verify(sessionRepository, times(1)).existsById(1L);
        verify(messageRepository, times(1)).findBySessionIdOrderByTimestampAsc(1L);
    }

    @Test
    @DisplayName("Should retrieve paginated messages")
    void getSessionMessagesPaginated_Success() {
        // Arrange
        List<ChatMessage> messages = Arrays.asList(testUserMessage, testAIMessage);
        Page<ChatMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 2);

        when(sessionRepository.existsById(1L)).thenReturn(true);
        when(messageRepository.findBySessionIdOrderByTimestampAsc(eq(1L), any(Pageable.class)))
                .thenReturn(messagePage);

        // Act
        PageResponse<MessageResponse> response = chatService.getSessionMessagesPaginated(1L, 0, 20);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());
        assertEquals(2L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.getLast());

        verify(sessionRepository, times(1)).existsById(1L);
        verify(messageRepository, times(1))
                .findBySessionIdOrderByTimestampAsc(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw exception when retrieving messages for non-existent session")
    void getSessionMessages_SessionNotFound() {
        // Arrange
        when(sessionRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> chatService.getSessionMessages(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found with ID: 999");

        verify(sessionRepository, times(1)).existsById(999L);
        verify(messageRepository, never()).findBySessionIdOrderByTimestampAsc(anyLong());
    }

    // ==================== Session Retrieval Tests ====================

    @Test
    @DisplayName("Should retrieve all user sessions")
    void getUserSessions_Success() {
        // Arrange
        ChatSession session2 = ChatSession.builder()
                .id(2L)
                .userId("user123")
                .title("Session 2")
                .isFavorite(true)
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<ChatSession> sessions = Arrays.asList(testSession, session2);
        when(sessionRepository.findByUserId("user123")).thenReturn(sessions);

        // Act
        List<SessionResponse> responses = chatService.getUserSessions("user123");

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Test Session", responses.get(0).getTitle());
        assertEquals("Session 2", responses.get(1).getTitle());

        verify(sessionRepository, times(1)).findByUserId("user123");
    }

    @Test
    @DisplayName("Should retrieve only favorite sessions")
    void getFavoriteSessions_Success() {
        // Arrange
        ChatSession favoriteSession = ChatSession.builder()
                .id(2L)
                .userId("user123")
                .title("Favorite Session")
                .isFavorite(true)
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<ChatSession> favoriteSessions = Arrays.asList(favoriteSession);
        when(sessionRepository.findByUserIdAndIsFavorite("user123", true))
                .thenReturn(favoriteSessions);

        // Act
        List<SessionResponse> responses = chatService.getFavoriteSessions("user123");

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getIsFavorite());

        verify(sessionRepository, times(1)).findByUserIdAndIsFavorite("user123", true);
    }

    @Test
    @DisplayName("Should retrieve session with all messages")
    void getSessionWithMessages_Success() {
        // Arrange
        testSession.getMessages().add(testUserMessage);
        testSession.getMessages().add(testAIMessage);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));

        // Act
        SessionWithMessagesResponse response = chatService.getSessionWithMessages(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("user123", response.getUserId());
        assertEquals(2, response.getMessages().size());

        verify(sessionRepository, times(1)).findById(1L);
    }

    // ==================== Session Update Tests ====================

    @Test
    @DisplayName("Should rename session successfully")
    void renameSession_Success() {
        // Arrange
        RenameSessionRequest request = RenameSessionRequest.builder()
                .newTitle("Updated Title")
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        // Act
        SessionResponse response = chatService.renameSession(1L, request);

        // Assert
        assertNotNull(response);
        verify(sessionRepository, times(1)).findById(1L);
        verify(sessionRepository, times(1)).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("Should throw exception when renaming non-existent session")
    void renameSession_SessionNotFound() {
        // Arrange
        RenameSessionRequest request = RenameSessionRequest.builder()
                .newTitle("New Title")
                .build();

        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> chatService.renameSession(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found with ID: 999");

        verify(sessionRepository, times(1)).findById(999L);
        verify(sessionRepository, never()).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("Should toggle favorite to true")
    void toggleFavorite_SetToTrue_Success() {
        // Arrange
        ToggleFavoriteRequest request = ToggleFavoriteRequest.builder()
                .isFavorite(true)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        // Act
        SessionResponse response = chatService.toggleFavorite(1L, request);

        // Assert
        assertNotNull(response);
        verify(sessionRepository, times(1)).findById(1L);
        verify(sessionRepository, times(1)).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("Should toggle favorite to false")
    void toggleFavorite_SetToFalse_Success() {
        // Arrange
        testSession.setIsFavorite(true);
        ToggleFavoriteRequest request = ToggleFavoriteRequest.builder()
                .isFavorite(false)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(testSession);

        // Act
        SessionResponse response = chatService.toggleFavorite(1L, request);

        // Assert
        assertNotNull(response);
        verify(sessionRepository, times(1)).findById(1L);
        verify(sessionRepository, times(1)).save(any(ChatSession.class));
    }

    // ==================== Session Deletion Tests ====================

    @Test
    @DisplayName("Should delete session successfully")
    void deleteSession_Success() {
        // Arrange
        when(sessionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(sessionRepository).deleteById(1L);

        // Act
        chatService.deleteSession(1L);

        // Assert
        verify(sessionRepository, times(1)).existsById(1L);
        verify(sessionRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent session")
    void deleteSession_SessionNotFound() {
        // Arrange
        when(sessionRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> chatService.deleteSession(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found with ID: 999");

        verify(sessionRepository, times(1)).existsById(999L);
        verify(sessionRepository, never()).deleteById(anyLong());
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Should handle empty message list")
    void getSessionMessages_EmptyList() {
        // Arrange
        when(sessionRepository.existsById(1L)).thenReturn(true);
        when(messageRepository.findBySessionIdOrderByTimestampAsc(1L))
                .thenReturn(new ArrayList<>());

        // Act
        List<MessageResponse> responses = chatService.getSessionMessages(1L);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(messageRepository, times(1)).findBySessionIdOrderByTimestampAsc(1L);
    }

    @Test
    @DisplayName("Should handle user with no sessions")
    void getUserSessions_NoSessions() {
        // Arrange
        when(sessionRepository.findByUserId("newUser")).thenReturn(new ArrayList<>());

        // Act
        List<SessionResponse> responses = chatService.getUserSessions("newUser");

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(sessionRepository, times(1)).findByUserId("newUser");
    }

    @Test
    @DisplayName("Should handle message with null context")
    void sendMessage_NullContext() {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Message without context")
                .context(null)
                .useAI(false)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(testUserMessage);

        // Act
        MessageResponse response = chatService.sendMessage(1L, request);

        // Assert
        assertNotNull(response);
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    // ==================== AI Integration Tests ====================

    @Test
    @DisplayName("Should not call AI when disabled")
    void sendMessage_AIDisabled() {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .useAI(true)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(testUserMessage);
        when(aiAssistantService.isAIEnabled()).thenReturn(false);

        // Act
        MessageResponse response = chatService.sendMessage(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("USER", response.getSender());

        verify(aiAssistantService, times(1)).isAIEnabled();
        verify(aiAssistantService, never()).generateResponse(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle AI service returning error message")
    void sendMessage_AIServiceError() {
        // Arrange
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Test")
                .useAI(true)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(messageRepository.save(any(ChatMessage.class)))
                .thenReturn(testUserMessage)
                .thenReturn(testAIMessage);
        when(aiAssistantService.isAIEnabled()).thenReturn(true);
        when(aiAssistantService.generateResponse(anyString(), nullable(String.class)))
                .thenReturn("I apologize, but I encountered an error while processing your message.");

        // Act
        MessageResponse response = chatService.sendMessage(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("AI_ASSISTANT", response.getSender());
        assertThat(response.getContent()).contains("error");

        verify(messageRepository, times(2)).save(any(ChatMessage.class));
    }
}