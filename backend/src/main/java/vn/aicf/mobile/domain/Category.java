package vn.aicf.mobile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.aicf.mobile.common.BaseEntity;

/** Bảng CATEGORIES - danh mục món (Cà phê, Trà...). */
@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    /** Emoji hiển thị trên app, vd "☕". */
    private String icon;

    /** Thứ tự hiển thị (nhỏ đứng trước). */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
