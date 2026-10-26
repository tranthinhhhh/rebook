package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private OrderItem orderItem;

    private int rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String mediaUrl;

    /** image | video */
    @Column(length = 10)
    private String mediaType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
