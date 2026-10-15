package vn.aicf.mobile.service.ai;

/**
 * Một lượt nói trong lịch sử chat gửi kèm cho AI để AI hiểu ngữ cảnh.
 *
 * @param role    "USER" hoặc "ASSISTANT"
 * @param content nội dung
 */
public record ChatTurn(String role, String content) {

    public boolean fromUser() {
        return "USER".equals(role);
    }
}
