package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.ChatMessageDto;
import vn.hcmute.rebook.entity.ChatMessage;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.ChatMessageRepository;
import vn.hcmute.rebook.repository.StoreRepository;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.ChatService;
import vn.hcmute.rebook.util.Formatter;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {
    private final ChatMessageRepository chatRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Formatter fmt;

    @Override
    public ChatMessageDto send(Long senderId, Long storeId, Long customerId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty() || text.length() > 1000) throw new BusinessException("Tin nhắn từ 1 đến 1000 ký tự");
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("Không tìm thấy cửa hàng"));
        boolean isOwner = store.getOwner().getId().equals(senderId);
        if (!isOwner && !senderId.equals(customerId)) throw new BusinessException("Bạn không thuộc cuộc trò chuyện này");
        if (isOwner && senderId.equals(customerId)) throw new BusinessException("Không thể tự nhắn cho cửa hàng của mình");
        User customer = userRepository.findById(customerId).orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng"));
        User sender = isOwner ? store.getOwner() : customer;

        ChatMessage msg = chatRepository.save(ChatMessage.builder()
                .store(store).customer(customer).sender(sender).content(text).build());
        ChatMessageDto dto = toDto(msg);
        // Đẩy realtime tới cả 2 phía
        messagingTemplate.convertAndSendToUser(customer.getEmail(), "/queue/chat", dto);
        messagingTemplate.convertAndSendToUser(store.getOwner().getEmail(), "/queue/chat", dto);
        return dto;
    }

    @Override
    public List<ChatMessageDto> history(Long viewerId, Long storeId, Long customerId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("Không tìm thấy cửa hàng"));
        if (!viewerId.equals(customerId) && !store.getOwner().getId().equals(viewerId)) {
            throw new BusinessException("Bạn không thuộc cuộc trò chuyện này");
        }
        chatRepository.markRead(storeId, customerId, viewerId);
        return chatRepository.findByStoreIdAndCustomerIdOrderByCreatedAtAsc(storeId, customerId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> conversationsOfStore(Long storeId) {
        return chatRepository.findLatestPerCustomer(storeId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> conversationsOfCustomer(Long customerId) {
        return chatRepository.findLatestPerStore(customerId).stream().map(this::toDto).toList();
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return new ChatMessageDto(m.getId(), m.getStore().getId(), m.getStore().getName(),
                m.getCustomer().getId(), m.getCustomer().getFullName(),
                m.getSender().getId(), m.getSender().getFullName(), m.getContent(), fmt.dateTime(m.getCreatedAt()));
    }
}
