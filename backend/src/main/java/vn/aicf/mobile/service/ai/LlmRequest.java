package vn.aicf.mobile.service.ai;

import vn.aicf.mobile.config.AuthUser;

import java.util.List;

/**
 * Một câu hỏi gửi cho dịch vụ AI.
 *
 * @param user         người hỏi (công cụ đọc dữ liệu chạy với đúng quyền của người này)
 * @param history      các lượt nói gần đây
 * @param message      câu hỏi hiện tại
 * @param systemPrompt lời dặn hệ thống (vai trò, quy tắc trả lời)
 * @param tools        các công cụ (hàm đọc dữ liệu) AI được phép gọi
 */
public record LlmRequest(AuthUser user, List<ChatTurn> history, String message, String systemPrompt, List<AiTool> tools) {
}
