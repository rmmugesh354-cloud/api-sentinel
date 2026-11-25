package com.sentinel.apisentinel.notification;

import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.common.ApiResponse;
import com.sentinel.apisentinel.common.PageResponse;
import com.sentinel.apisentinel.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User alert notifications and history")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get paginated notifications for current user")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<NotificationResponse> response = notificationService.getUserNotifications(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread notification count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        int updated = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", updated));
    }
}
