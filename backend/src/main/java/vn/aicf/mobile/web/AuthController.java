package vn.aicf.mobile.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.aicf.mobile.service.AuthService;
import vn.aicf.mobile.web.dto.AuthDtos.AuthResponse;
import vn.aicf.mobile.web.dto.AuthDtos.FirebaseLoginRequest;
import vn.aicf.mobile.web.dto.AuthDtos.LoginRequest;
import vn.aicf.mobile.web.dto.AuthDtos.RegisterRequest;

/**
 * API XÁC THỰC (không cần đăng nhập).
 * <pre>
 *   POST /api/auth/register   đăng ký tài khoản khách        -> 201 + token
 *   POST /api/auth/login      đăng nhập tên + mật khẩu       -> 200 + token | 401
 *   POST /api/auth/firebase   đăng nhập Google (Firebase)    -> 200 + token | 401 | 503
 * </pre>
 * Controller chỉ nhận request, kiểm tra dữ liệu (@Valid) rồi giao cho Service - không chứa nghiệp vụ.
 *
 * <p>Hai chú thích dành cho SWAGGER (chỉ ảnh hưởng trang tài liệu /swagger-ui.html, không đổi cách API chạy):
 * <ul>
 *   <li>{@code @Tag} đặt trên lớp: tên NHÓM chứa các API của controller này trên trang Swagger.</li>
 *   <li>{@code @Operation(summary = ...)} đặt trên từng hàm: dòng mô tả hiện cạnh đường dẫn API.</li>
 * </ul>
 * Mọi controller khác dùng cùng cách này (xem giải thích đầy đủ ở config/OpenApiConfig.java).
 */
@Tag(name = "01. Xác thực")   // Swagger: nhóm "01. Xác thực" (đánh số để các nhóm xếp đúng thứ tự)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    // Swagger: dòng mô tả hiện cạnh "POST /api/auth/register"; thư viện tự đọc thêm kiểu dữ liệu
    // RegisterRequest (các trường + điều kiện @NotBlank, @Size) để vẽ mẫu JSON cần gửi
    @Operation(summary = "Đăng ký tài khoản khách hàng (đăng ký xong trả luôn token)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng tên đăng nhập + mật khẩu")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/firebase")
    @Operation(summary = "Đăng nhập Google: gửi Firebase ID token, nhận JWT của hệ thống")
    public AuthResponse firebase(@Valid @RequestBody FirebaseLoginRequest request) {
        return auth.loginWithFirebase(request.idToken());
    }
}
