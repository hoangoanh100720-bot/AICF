package vn.aicf.mobile.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * BẮT LỖI TẬP TRUNG cho toàn bộ REST API ({@code @RestControllerAdvice}).
 * Controller / Service không cần try-catch: ném lỗi ra là lớp này đổi thành JSON {@link ApiError}
 * với mã HTTP phù hợp. Nhờ vậy app di động luôn nhận lỗi cùng một định dạng.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Lỗi nghiệp vụ do chính code ném ra. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {
        HttpStatus s = e.getStatus();
        return ResponseEntity.status(s).body(ApiError.of(s.value(), s.getReasonPhrase(), e.getMessage()));
    }

    /** Dữ liệu vi phạm @NotBlank, @Min... trên DTO (khi controller dùng @Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        String first = details.isEmpty() ? "Dữ liệu gửi lên không hợp lệ" : details.get(0);
        return ResponseEntity.badRequest().body(ApiError.of(400, "Bad Request", first, details));
    }

    /** JSON sai cú pháp, sai kiểu (gửi chữ vào trường số), thiếu tham số bắt buộc. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleUnreadable(Exception e) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Bad Request", "Không đọc được dữ liệu gửi lên (sai định dạng hoặc thiếu tham số)"));
    }

    /** Đã đăng nhập nhưng không đủ vai trò (@PreAuthorize từ chối). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", "Bạn không có quyền thực hiện thao tác này"));
    }

    /** Vi phạm ràng buộc CSDL: trùng UNIQUE, khóa ngoại (xóa danh mục còn món)... */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", "Dữ liệu bị trùng hoặc đang được dùng ở nơi khác"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of(405, "Method Not Allowed", "API không hỗ trợ phương thức " + e.getMethod()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", "Không tìm thấy đường dẫn " + e.getResourcePath()));
    }

    /** Lỗi bất ngờ: ghi log đầy đủ để sửa, nhưng chỉ trả thông báo chung (không lộ chi tiết nội bộ). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e) {
        log.error("Lỗi không mong đợi", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal Server Error", "Máy chủ gặp lỗi, vui lòng thử lại sau"));
    }
}
