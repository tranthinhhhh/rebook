package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.entity.Notification;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.repository.NotificationRepository;
import vn.hcmute.rebook.service.NotificationService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void notify(User user, String message, String link) {
        notificationRepository.save(Notification.builder().user(user).message(message).link(link).build());
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notify",
                Map.of("message", message, "link", link == null ? "" : link));
    }

    @Override
    public List<Notification> latest(Long userId, int limit) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }
}
