package vn.aicf.mobile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.aicf.mobile.common.BaseEntity;

/** Bảng PRODUCTS - món trong thực đơn. */
@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    /**
     * Quan hệ NHIỀU món - MỘT danh mục (products.category_id -> categories.id).
     * LAZY: chỉ nạp danh mục khi thật sự cần, tránh truy vấn thừa.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Giá bán (đồng). */
    @Column(nullable = false)
    private long price;

    @Column(name = "image_url")
    private String imageUrl;

    /** true = đang bán; false = tạm hết (app ẩn nút đặt). */
    @Column(nullable = false)
    private boolean available = true;
}
