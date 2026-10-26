package vn.hcmute.rebook.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import vn.hcmute.rebook.dto.ChatMessageDto;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.ChatService;

import java.security.Principal;

/** Nhận tin nhắn chat qua WebSocket (STOMP): client gửi tới /app/chat.send */
@Controller
@RequiredArgsConstructor
public class ChatSocketController {
    private final ChatService chatService;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessageDto message, Principal principal) {
        if (principal == null) return;
        userRepository.findByEmail(principal.getName()).ifPresent(sender ->
                chatService.send(sender.getId(), message.getStoreId(), message.getCustomerId(), message.getContent()));
    }
}
