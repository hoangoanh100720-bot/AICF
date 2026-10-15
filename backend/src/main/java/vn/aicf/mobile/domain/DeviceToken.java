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

/** Bảng DEVICE_TOKENS - mã FCM của từng thiết bị để gửi thông báo đẩy. */
@Getter
@Setter
@Entity
@Table(name = "device_tokens")
public class DeviceToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Mã do Firebase cấp cho thiết bị (chuỗi dài ~150 ký tự). */
    @Column(nullable = false, unique = true)
    private String token;

    /** WEB (PWA trên trình duyệt) / ANDROID / IOS. */
    @Column(nullable = false)
    private String platform = "WEB";
}
