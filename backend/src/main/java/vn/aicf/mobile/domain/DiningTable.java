package vn.aicf.mobile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import vn.aicf.mobile.common.BaseEntity;

/** Bảng DINING_TABLES - bàn trong quán. */
@Getter
@Setter
@Entity
@Table(name = "dining_tables")
public class DiningTable extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    /** Khu vực: Trong nhà, Sân vườn... */
    private String area;

    @Column(nullable = false)
    private int seats = 4;

    @Column(nullable = false)
    private boolean active = true;
}
