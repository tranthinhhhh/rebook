package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import vn.hcmute.rebook.entity.enums.StoreStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Store {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(unique = true)
    private User owner;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;

    @Column(length = 15)
    private String phone;

    private String logoUrl;
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status;

    /** Chiết khấu (%) app thu trên doanh thu của shop */
    private double commissionRate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
