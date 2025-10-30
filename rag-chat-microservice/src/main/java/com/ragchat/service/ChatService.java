package com.ragchat.service;

import com.ragchat.dto.*;
import com.ragchat.entity.ChatMessage;
import com.ragchat.entity.ChatSession;
import com.ragchat.exception.ResourceNotFoundException;
import com.ragchat.repository.ChatMessageRepository;
import com.ragchat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AIAssistantService aiAssistantService;

    public SessionResponse createSession(CreateSessionRequest request) {
        log.info("Creating new chat session for user: {}", request.getUserId());

        ChatSession session = ChatSession.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .isFavorite(false)
                .build();

        session = sessionRepository.save(session);
        log.info("Chat session created successfully with ID: {}", session.getId());

        return mapToSessionResponse(session);
    }

    public MessageResponse sendMessage(Long sessionId, SendMessageRequest request) {
        log.info("Sending message to session: {}", sessionId);

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        // Save user message
        ChatMessage userMessage = ChatMessage.builder()
                .session(session)
                .sender(ChatMessage.SenderType.USER)
                .content(request.getContent())
                .context(request.getContext())
                .build();

        session.addMessage(userMessage);
        messageRepository.save(userMessage);

        // Generate AI response if enabled
        if (request.getUseAI() != null && request.getUseAI() && aiAssistantService.isAIEnabled()) {
            String aiResponse = aiAssistantService.generateResponse(
                    request.getContent(),
                    request.getContext()
            );

            ChatMessage aiMessage = ChatMessage.builder()
                    .session(session)
                    .sender(ChatMessage.SenderType.AI_ASSISTANT)
                    .content(aiResponse)
                    .context(request.getContext())
                    .build();

            session.addMessage(aiMessage);
            messageRepository.save(aiMessage);

            log.info("AI response generated and saved for session: {}", sessionId);
            return mapToMessageResponse(aiMessage);
        }

        log.info("User message saved successfully");
        return mapToMessageResponse(userMessage);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getSessionMessages(Long sessionId) {
        log.info("Retrieving messages for session: {}", sessionId);

        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found with ID: " + sessionId);
        }

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getSessionMessagesPaginated(Long sessionId, int page, int size) {
        log.info("Retrieving paginated messages for session: {} (page: {}, size: {})", sessionId, page, size);

        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found with ID: " + sessionId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").ascending());
        Page<ChatMessage> messagePage = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId, pageable);

        List<MessageResponse> messages = messagePage.getContent().stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());

        return PageResponse.<MessageResponse>builder()
                .content(messages)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .last(messagePage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public SessionWithMessagesResponse getSessionWithMessages(Long sessionId) {
        log.info("Retrieving session with messages: {}", sessionId);

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        List<MessageResponse> messages = session.getMessages().stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());

        return SessionWithMessagesResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .isFavorite(session.getIsFavorite())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messages(messages)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(String userId) {
        log.info("Retrieving all sessions for user: {}", userId);

        List<ChatSession> sessions = sessionRepository.findByUserId(userId);
        return sessions.stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getFavoriteSessions(String userId) {
        log.info("Retrieving favorite sessions for user: {}", userId);

        List<ChatSession> sessions = sessionRepository.findByUserIdAndIsFavorite(userId, true);
        return sessions.stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    public SessionResponse renameSession(Long sessionId, RenameSessionRequest request) {
        log.info("Renaming session: {} to '{}'", sessionId, request.getNewTitle());

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        session.setTitle(request.getNewTitle());
        session = sessionRepository.save(session);

        log.info("Session renamed successfully");
        return mapToSessionResponse(session);
    }

    public SessionResponse toggleFavorite(Long sessionId, ToggleFavoriteRequest request) {
        log.info("Toggling favorite status for session: {} to {}", sessionId, request.getIsFavorite());

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        session.setIsFavorite(request.getIsFavorite());
        session = sessionRepository.save(session);

        log.info("Favorite status updated successfully");
        return mapToSessionResponse(session);
    }

    public void deleteSession(Long sessionId) {
        log.info("Deleting session: {}", sessionId);

        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found with ID: " + sessionId);
        }

        sessionRepository.deleteById(sessionId);
        log.info("Session deleted successfully");
    }

    private SessionResponse mapToSessionResponse(ChatSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .isFavorite(session.getIsFavorite())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(session.getMessages().size())
                .build();
    }

    private MessageResponse mapToMessageResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .sender(message.getSender().name())
                .content(message.getContent())
                .context(message.getContext())
                .timestamp(message.getTimestamp())
                .build();
    }
}