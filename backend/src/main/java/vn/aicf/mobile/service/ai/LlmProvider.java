package vn.aicf.mobile.service.ai;

import java.io.IOException;

/**
 * Một dịch vụ AI thật (Gemini, Claude...) - áp dụng mẫu thiết kế <b>Strategy</b>.
 *
 * <p>Muốn thêm hãng AI mới (vd. OpenAI) chỉ cần viết 1 lớp {@code @Component} cài đặt interface này;
 * {@link ChatService} tự nhận ra qua danh sách bean và chọn theo cấu hình "ai.provider",
 * KHÔNG phải sửa code ở nơi gọi (nguyên lý Mở/Đóng - Open/Closed).
 */
public interface LlmProvider {

    /** Tên dùng trong cấu hình ai.provider, vd "gemini", "claude". */
    String id();

    /** Đã có khóa API cho dịch vụ này chưa. */
    boolean hasKey();

    /** Tên model đang dùng (hiển thị cho Admin). */
    String model();

    /**
     * Trả lời câu hỏi; AI có thể gọi các công cụ trong request nhiều vòng trước khi trả lời.
     *
     * @throws IOException lỗi mạng / dịch vụ, có thông báo tiếng Việt (nơi gọi sẽ chuyển sang chế độ cơ bản)
     */
    String chat(LlmRequest request) throws IOException, InterruptedException;
}
