package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.hcmute.rebook.entity.enums.VoucherType;

import java.time.LocalDateTime;

/** Mã khuyến mãi: store = null là mã toàn sàn (Admin/Manager tạo), ngược lại là mã của shop */
@Entity
@Table(name = "vouchers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Voucher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherType type;

    private int percent;

    /** Số tiền giảm tối đa (0 = không giới hạn) */
    private long maxDiscount;

    /** Giá trị đơn tối thiểu */
    private long minOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    private Store store;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private int quantity;
    private int usedCount;
    private boolean active;

    public boolean isUsable() {
        LocalDateTime now = LocalDateTime.now();
        return active && usedCount < quantity
                && (startAt == null || !now.isBefore(startAt))
                && (endAt == null || !now.isAfter(endAt));
    }
}
