package vn.aicf.mobile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Ánh xạ nhóm cấu hình "app:" trong application.yml sang đối tượng Java có kiểu rõ ràng
 * (thay vì rải @Value("${...}") khắp nơi). Spring tự điền giá trị khi khởi động.
 *
 * @param dataDir       thư mục dữ liệu (file SQLite, khóa JWT, file Firebase)
 * @param frontendDir   thư mục PWA để backend phục vụ tĩnh (trống = không phục vụ)
 * @param jwt           cấu hình token đăng nhập
 * @param corsOrigins   các nguồn (origin) được phép gọi API từ trình duyệt
 * @param firebase      cấu hình Firebase Admin SDK
 * @param ai            cấu hình mặc định cho Chat AI (có thể bị ghi đè bởi bảng app_settings)
 * @param seedDemoUsers có tạo tài khoản mẫu lần đầu chạy hay không
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String dataDir,
        String frontendDir,
        Jwt jwt,
        List<String> corsOrigins,
        Firebase firebase,
        Ai ai,
        boolean seedDemoUsers) {

    /** @param secret khóa ký HMAC (>= 32 ký tự), trống thì tự sinh; @param ttlHours token sống bao nhiêu giờ */
    public record Jwt(String secret, int ttlHours) {
    }

    /** @param credentialsFile đường dẫn file service-account JSON tải từ Firebase Console */
    public record Firebase(String credentialsFile) {
    }

    /** Khóa API và tên model mặc định của 2 dịch vụ AI. */
    public record Ai(String provider, String geminiApiKey, String geminiModel, String claudeApiKey, String claudeModel) {
    }
}
