package vn.aicf.mobile.common;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Định dạng JSON THỐNG NHẤT cho mọi lỗi API. Ví dụ:
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Dữ liệu gửi lên không hợp lệ",
 *   "details": ["name: không được để trống"],
 *   "timestamp": "2026-10-15T09:30:00"
 * }
 * </pre>
 * App chỉ cần đọc trường "message" để hiện cho người dùng.
 */
public record ApiError(int status, String error, String message, List<String> details, LocalDateTime timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, List.of(), LocalDateTime.now());
    }

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(status, error, message, details, LocalDateTime.now());
    }
}
