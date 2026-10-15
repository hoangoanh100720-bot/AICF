package vn.aicf.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TimeZone;

/**
 * Điểm khởi động backend.
 *
 * <p>Kiến trúc phân lớp (mỗi gói một trách nhiệm, gọi theo một chiều từ trên xuống):
 * <pre>
 *   web (Controller)  : nhận HTTP request, kiểm tra dữ liệu vào, trả JSON          -> gói vn.aicf.mobile.web
 *        │
 *   service           : nghiệp vụ (đặt đơn, đổi trạng thái, chat AI, gửi thông báo) -> gói vn.aicf.mobile.service
 *        │
 *   repository        : truy vấn CSDL qua Spring Data JPA                          -> gói vn.aicf.mobile.repository
 *        │
 *   domain (Entity)   : lớp Java ánh xạ 1-1 với bảng SQLite                        -> gói vn.aicf.mobile.domain
 * </pre>
 * Gói {@code common} chứa phần dùng lại ở mọi nơi (lớp CRUD tổng quát, xử lý lỗi, chuyển đổi ngày giờ);
 * gói {@code config} chứa cấu hình (bảo mật JWT, Firebase, CORS, Swagger, dữ liệu mẫu).
 */
// Tắt cấu hình đăng nhập mặc định của Spring (tự tạo user "user" + mật khẩu ngẫu nhiên): hệ thống dùng JWT riêng
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan   // tự nạp các lớp @ConfigurationProperties (AppProperties)
@EnableAsync                   // cho phép @Async: gửi thông báo đẩy chạy nền, không làm chậm API
public class MobileBackendApplication {

    public static void main(String[] args) throws IOException {
        // Toàn hệ thống dùng giờ Việt Nam (thống kê "hôm nay" mới đúng khi chạy trên máy chủ giờ UTC)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        // SQLite tự tạo FILE .db nhưng KHÔNG tự tạo THƯ MỤC chứa nó -> tạo trước khi Spring mở kết nối
        String dataDir = System.getenv().getOrDefault("APP_DATA_DIR", "./data");
        Files.createDirectories(Path.of(dataDir));

        SpringApplication.run(MobileBackendApplication.class, args);
    }
}
