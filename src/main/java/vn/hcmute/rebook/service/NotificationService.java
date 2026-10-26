package vn.hcmute.rebook.service;

import vn.hcmute.rebook.entity.Notification;
import vn.hcmute.rebook.entity.User;

import java.util.List;

public interface NotificationService {
    /** Lưu thông báo và đẩy realtime qua WebSocket */
    void notify(User user, String message, String link);

    List<Notification> latest(Long userId, int limit);

    long countUnread(Long userId);

    void markAllRead(Long userId);
}
