package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    /** Lưu lại tên sách tại thời điểm đặt */
    @Column(nullable = false)
    private String productTitle;

    private long price;
    private int quantity;
    private boolean reviewed;

    public long getLineTotal() {
        return price * quantity;
    }
}
