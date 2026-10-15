package vn.aicf.mobile.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.aicf.mobile.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Bảng ORDERS - đầu đơn hàng.
 * Tên lớp là CustomerOrder (không phải "Order") vì ORDER là từ khóa của SQL/JPQL ("ORDER BY").
 */
@Getter
@Setter
@Entity
@Table(name = "orders")
public class CustomerOrder extends BaseEntity {

    /** Mã đơn dễ đọc, vd DH20261015-0007. */
    @Column(nullable = false, unique = true)
    private String code;

    /** Người đặt (NHIỀU đơn - MỘT người dùng). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Bàn (null nếu mang đi). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private DiningTable table;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType = OrderType.TAKEAWAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    /** Tổng tiền (đồng) - tính lại mỗi khi danh sách món thay đổi, xem {@link #recalculateTotal()}. */
    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(nullable = false)
    private boolean paid;

    private String note;

    @Column(name = "cancel_reason")
    private String cancelReason;

    /**
     * Quan hệ MỘT đơn - NHIỀU dòng món.
     * cascade ALL   : lưu / xóa đơn thì lưu / xóa luôn các dòng món.
     * orphanRemoval : bỏ 1 dòng khỏi danh sách thì dòng đó bị xóa khỏi CSDL.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    /** Thêm 1 dòng món và gắn 2 chiều quan hệ (item.order = this). */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    /** Tổng tiền = tổng (đơn giá x số lượng). Luôn tính ở backend, KHÔNG tin số tiền app gửi lên. */
    public void recalculateTotal() {
        totalAmount = items.stream().mapToLong(i -> i.getUnitPrice() * i.getQuantity()).sum();
    }
}
