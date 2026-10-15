package vn.aicf.mobile.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * LỚP CHA DÙNG LẠI cho mọi Entity có 3 cột chung: id, created_at, updated_at.
 *
 * <p>{@code @MappedSuperclass}: lớp này KHÔNG phải một bảng; các cột của nó được "chép" vào bảng của lớp con.
 * Nhờ vậy Category, Product, DiningTable... không phải khai báo lại id / ngày tạo / ngày sửa,
 * và lớp CRUD tổng quát {@link CrudService} có thể làm việc với mọi lớp con qua kiểu BaseEntity.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    /** Khóa chính, SQLite tự tăng (INTEGER PRIMARY KEY AUTOINCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Thời điểm tạo bản ghi (không đổi sau khi tạo). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm sửa gần nhất. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** JPA gọi tự động ngay trước khi INSERT. */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    /** JPA gọi tự động ngay trước khi UPDATE. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
