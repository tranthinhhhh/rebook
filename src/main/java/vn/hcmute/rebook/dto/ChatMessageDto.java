package vn.hcmute.rebook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long storeId;
    private String storeName;
    private Long customerId;
    private String customerName;
    private Long senderId;
    private String senderName;
    private String content;
    private String createdAt;
}
