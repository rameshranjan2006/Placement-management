package com.placement.controller;

import com.placement.model.Notification;
import com.placement.security.AccessGuard;
import com.placement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AccessGuard guard;

    @GetMapping("/mine")
    public ResponseEntity<List<Notification>> mine() {
        return ResponseEntity.ok(notificationService.mine(guard.user()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unread() {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(guard.user())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, guard.user());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(guard.user());
        return ResponseEntity.noContent().build();
    }
}
