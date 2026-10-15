package vn.aicf.mobile.service.firebase;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vn.aicf.mobile.repository.DeviceTokenRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gửi THÔNG BÁO ĐẨY qua Firebase Cloud Messaging tới danh sách mã thiết bị.
 *
 * <p>{@code @Async}: chạy ở luồng nền -> API đổi trạng thái đơn trả kết quả ngay, không phải chờ Google.
 * (Tách thành bean riêng vì @Async chỉ có tác dụng khi được gọi TỪ BEAN KHÁC qua proxy của Spring.)
 *
 * <p>Mã nào Firebase báo "không còn đăng ký" (app đã gỡ, người dùng chặn thông báo) thì xóa khỏi bảng device_tokens.
 */
@Slf4j
@Component
public class PushSender {

    /** FCM cho phép tối đa 500 mã mỗi lần gửi. */
    private static final int BATCH = 500;

    private final FirebaseService firebase;
    private final DeviceTokenRepository tokens;
    private final TransactionTemplate tx;

    public PushSender(FirebaseService firebase, DeviceTokenRepository tokens, TransactionTemplate tx) {
        this.firebase = firebase;
        this.tokens = tokens;
        this.tx = tx;
    }

    /**
     * @param deviceTokens mã FCM nhận thông báo
     * @param title        tiêu đề
     * @param body         nội dung
     * @param data         dữ liệu kèm theo (app đọc để mở đúng màn hình, vd {"orderId": "12"})
     */
    @Async
    public void send(List<String> deviceTokens, String title, String body, Map<String, String> data) {
        if (!firebase.isEnabled() || deviceTokens.isEmpty()) {
            return;
        }
        List<String> invalid = new ArrayList<>();
        for (int from = 0; from < deviceTokens.size(); from += BATCH) {
            List<String> batch = deviceTokens.subList(from, Math.min(from + BATCH, deviceTokens.size()));
            MulticastMessage msg = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    // Với PWA: bấm vào thông báo thì mở app tại trang đơn hàng
                    .setWebpushConfig(WebpushConfig.builder()
                            .setFcmOptions(WebpushFcmOptions.withLink("/#/orders"))
                            .build())
                    .build();
            try {
                BatchResponse res = firebase.messaging().sendEachForMulticast(msg);
                List<SendResponse> results = res.getResponses();
                for (int i = 0; i < results.size(); i++) {
                    SendResponse r = results.get(i);
                    if (!r.isSuccessful() && r.getException() != null) {
                        MessagingErrorCode code = r.getException().getMessagingErrorCode();
                        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                            invalid.add(batch.get(i));
                        }
                    }
                }
                log.debug("FCM: gửi {} thiết bị, thành công {}", batch.size(), res.getSuccessCount());
            } catch (Exception e) {
                log.warn("FCM: gửi thông báo lỗi: {}", e.getMessage());
            }
        }
        if (!invalid.isEmpty()) {
            tx.executeWithoutResult(s -> tokens.deleteTokens(invalid));
            log.info("FCM: xóa {} mã thiết bị đã hết hiệu lực", invalid.size());
        }
    }
}
