package vn.hcmute.rebook.service;

import vn.hcmute.rebook.dto.ChatMessageDto;

import java.util.List;

public interface ChatService {
    /** Gửi tin nhắn; senderId phải là khách (customerId) hoặc chủ shop */
    ChatMessageDto send(Long senderId, Long storeId, Long customerId, String content);

    List<ChatMessageDto> history(Long viewerId, Long storeId, Long customerId);

    List<ChatMessageDto> conversationsOfStore(Long storeId);

    List<ChatMessageDto> conversationsOfCustomer(Long customerId);
}
