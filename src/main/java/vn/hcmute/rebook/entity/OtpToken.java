package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.hcmute.rebook.entity.enums.OtpPurpose;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens", indexes = @Index(columnList = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    private LocalDateTime expiresAt;
    private boolean used;
    private int attempts;
}
