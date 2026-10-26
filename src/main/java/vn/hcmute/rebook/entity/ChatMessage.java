package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Tin nhắn giữa khách hàng và cửa hàng (gửi realtime qua WebSocket) */
@Entity
@Table(name = "chat_messages", indexes = @Index(columnList = "store_id,customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String content;

    private boolean readFlag;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
