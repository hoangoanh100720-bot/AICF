package vn.aicf.mobile.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.aicf.mobile.domain.Notification;

import java.time.LocalDateTime;

/** DTO cho thông báo (/api/notifications) và đăng ký thiết bị nhận FCM (/api/devices). */
public final class NotificationDtos {

    private NotificationDtos() {
    }

    /** POST /api/devices - app gửi mã FCM của thiết bị sau khi người dùng cho phép nhận thông báo. */
    public record DeviceTokenRequest(
            @NotBlank(message = "thiếu mã thiết bị") @Size(max = 4096) String token,
            @Pattern(regexp = "WEB|ANDROID|IOS", message = "chỉ nhận WEB, ANDROID hoặc IOS") String platform) {
    }

    public record NotificationResponse(Long id, String title, String body, Long orderId, boolean read,
                                       LocalDateTime createdAt) {

        public static NotificationResponse of(Notification n) {
            return new NotificationResponse(n.getId(), n.getTitle(), n.getBody(), n.getOrderId(), n.isRead(), n.getCreatedAt());
        }
    }

    /** Số thông báo chưa đọc (hiện chấm đỏ trên app). */
    public record UnreadCount(long unread) {
    }
}
