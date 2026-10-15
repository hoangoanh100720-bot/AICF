package vn.aicf.mobile.common;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ có kèm mã HTTP. Service chỉ việc {@code throw ApiException.notFound("...")},
 * {@link GlobalExceptionHandler} sẽ đổi thành JSON {@link ApiError} với đúng mã trạng thái.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 400: dữ liệu gửi lên sai. */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 401: chưa đăng nhập / sai mật khẩu. */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    /** 403: đã đăng nhập nhưng không đủ quyền. */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    /** 404: không tìm thấy. */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** 409: xung đột dữ liệu (trùng tên, sai trạng thái...). */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    /** 503: tính năng phụ thuộc dịch vụ ngoài chưa được cấu hình (vd. Firebase). */
    public static ApiException unavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
