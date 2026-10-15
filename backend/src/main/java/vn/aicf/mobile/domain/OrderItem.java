package vn.aicf.mobile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Bảng ORDER_ITEMS - một dòng món trong đơn.
 * Không kế thừa BaseEntity vì bảng này không có cột created_at / updated_at (đi theo đơn cha).
 */
@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    /** Món gốc; có thể null nếu món đã bị xóa khỏi thực đơn sau này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Tên món TẠI THỜI ĐIỂM ĐẶT (snapshot). */
    @Column(name = "product_name", nullable = false)
    private String productName;

    /** Đơn giá TẠI THỜI ĐIỂM ĐẶT (snapshot). */
    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int quantity;

    /** Ghi chú riêng của món: "ít đá", "không đường"... */
    private String note;
}
