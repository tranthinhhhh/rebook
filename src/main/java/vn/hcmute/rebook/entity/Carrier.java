package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;

/** Nhà vận chuyển */
@Entity
@Table(name = "carriers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Carrier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Phí vận chuyển (VNĐ) */
    private long fee;

    @Column(length = 50)
    private String estimatedTime;

    private boolean active;
}
