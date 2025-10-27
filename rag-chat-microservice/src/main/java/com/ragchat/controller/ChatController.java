package com.ragchat.controller;

import com.ragchat.dto.*;
import com.ragchat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Management", description = "APIs for managing chat sessions and messages")
@SecurityRequirement(name = "API Key")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    @Operation(summary = "Create a new chat session", description = "Creates a new chat session for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Session created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request) {
        log.info("Received request to create session: {}", request);
        SessionResponse session = chatService.createSession(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(com.ragchat.dto.ApiResponse.success("Session created successfully", session));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Send a message", description = "Sends a message in a chat session and optionally gets AI response")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<MessageResponse>> sendMessage(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        log.info("Received request to send message to session: {}", sessionId);
        MessageResponse message = chatService.sendMessage(sessionId, request);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success("Message sent successfully", message));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Get session messages", description = "Retrieves all messages in a chat session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<List<MessageResponse>>> getSessionMessages(
            @Parameter(description = "Session ID") @PathVariable Long sessionId) {
        log.info("Received request to get messages for session: {}", sessionId);
        List<MessageResponse> messages = chatService.getSessionMessages(sessionId);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success(messages));
    }

    @GetMapping("/sessions/{sessionId}/messages/paginated")
    @Operation(summary = "Get paginated session messages", description = "Retrieves messages with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<PageResponse<MessageResponse>>> getSessionMessagesPaginated(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Received request to get paginated messages for session: {}", sessionId);
        PageResponse<MessageResponse> messages = chatService.getSessionMessagesPaginated(sessionId, page, size);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success(messages));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get session with messages", description = "Retrieves a session with all its messages")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<SessionWithMessagesResponse>> getSessionWithMessages(
            @Parameter(description = "Session ID") @PathVariable Long sessionId) {
        log.info("Received request to get session with messages: {}", sessionId);
        SessionWithMessagesResponse session = chatService.getSessionWithMessages(sessionId);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success(session));
    }

    @GetMapping("/users/{userId}/sessions")
    @Operation(summary = "Get user sessions", description = "Retrieves all sessions for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<List<SessionResponse>>> getUserSessions(
            @Parameter(description = "User ID") @PathVariable String userId) {
        log.info("Received request to get sessions for user: {}", userId);
        List<SessionResponse> sessions = chatService.getUserSessions(userId);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success(sessions));
    }

    @GetMapping("/users/{userId}/sessions/favorites")
    @Operation(summary = "Get favorite sessions", description = "Retrieves favorite sessions for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favorite sessions retrieved successfully")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<List<SessionResponse>>> getFavoriteSessions(
            @Parameter(description = "User ID") @PathVariable String userId) {
        log.info("Received request to get favorite sessions for user: {}", userId);
        List<SessionResponse> sessions = chatService.getFavoriteSessions(userId);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success(sessions));
    }

    @PutMapping("/sessions/{sessionId}/rename")
    @Operation(summary = "Rename session", description = "Renames a chat session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session renamed successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<SessionResponse>> renameSession(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Valid @RequestBody RenameSessionRequest request) {
        log.info("Received request to rename session: {}", sessionId);
        SessionResponse session = chatService.renameSession(sessionId, request);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success("Session renamed successfully", session));
    }

    @PutMapping("/sessions/{sessionId}/favorite")
    @Operation(summary = "Toggle favorite", description = "Marks or unmarks a session as favorite")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favorite status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<SessionResponse>> toggleFavorite(
            @Parameter(description = "Session ID") @PathVariable Long sessionId,
            @Valid @RequestBody ToggleFavoriteRequest request) {
        log.info("Received request to toggle favorite for session: {}", sessionId);
        SessionResponse session = chatService.toggleFavorite(sessionId, request);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success("Favorite status updated successfully", session));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete session", description = "Deletes a chat session and all its messages")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<com.ragchat.dto.ApiResponse<Void>> deleteSession(
            @Parameter(description = "Session ID") @PathVariable Long sessionId) {
        log.info("Received request to delete session: {}", sessionId);
        chatService.deleteSession(sessionId);
        return ResponseEntity.ok(
                com.ragchat.dto.ApiResponse.success("Session deleted successfully", null));
    }
}