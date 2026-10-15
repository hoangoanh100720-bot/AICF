package vn.aicf.mobile.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** DTO cho Chat AI /api/chat. */
public final class ChatDtos {

    private ChatDtos() {
    }

    /** POST /api/chat - câu hỏi của người dùng. */
    public record ChatRequest(@NotBlank(message = "hãy nhập câu hỏi") @Size(max = 2000, message = "tối đa 2000 ký tự") String message) {
    }

    /**
     * Câu trả lời của AI.
     *
     * @param reply       nội dung trả lời
     * @param mode        "LLM" = AI thật trả lời, "OFFLINE" = chế độ cơ bản (chưa có khóa API hoặc AI lỗi)
     * @param notice      ghi chú cho người dùng (vd. "AI thật đang lỗi nên trả lời bằng chế độ cơ bản"), có thể null
     * @param suggestions các câu hỏi gợi ý để bấm nhanh
     */
    public record ChatReply(String reply, String mode, String notice, List<String> suggestions) {
    }

    /** Một tin nhắn trong lịch sử. */
    public record MessageResponse(Long id, String role, String content, String mode, LocalDateTime createdAt) {
    }

    /** GET /api/chat/status - app hiển thị "AI thật" hay "chế độ cơ bản". */
    public record ChatStatus(String mode, String provider, List<String> suggestions) {
    }
}
