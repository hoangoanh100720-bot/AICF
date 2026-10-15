package vn.aicf.mobile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Bảng APP_SETTINGS - cấu hình dạng khóa / giá trị. Khóa chính là chính tên cấu hình. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    @Column(name = "setting_key")
    private String key;

    @Column(name = "setting_value")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
