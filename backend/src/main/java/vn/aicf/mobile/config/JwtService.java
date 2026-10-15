package vn.aicf.mobile.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import vn.aicf.mobile.domain.User;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Tạo và kiểm tra JWT (JSON Web Token) - "vé" đăng nhập dùng cho REST API không trạng thái (stateless).
 *
 * <p>Luồng hoạt động:
 * <ol>
 *   <li>App gửi tên đăng nhập + mật khẩu tới POST /api/auth/login</li>
 *   <li>Backend kiểm tra đúng -> {@link #issue} tạo token có chữ ký HMAC-SHA, hạn {@code ttl-hours} giờ</li>
 *   <li>App lưu token, mỗi request sau gửi kèm header {@code Authorization: Bearer <token>}</li>
 *   <li>{@link JwtAuthFilter} gọi {@link #parse} để kiểm tra chữ ký + hạn, biết ai đang gọi</li>
 * </ol>
 * Máy chủ không cần lưu phiên đăng nhập; ai sửa nội dung token thì chữ ký sai -> bị từ chối.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(resolveSecret(props));
        this.ttl = Duration.ofHours(props.jwt().ttlHours());
    }

    /** Token vừa cấp và thời điểm hết hạn. */
    public record Issued(String token, Instant expiresAt) {
    }

    /** Cấp token cho người dùng. "sub" (subject) = id người dùng; thêm tên + vai trò để app đọc nhanh nếu cần. */
    public Issued issue(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        String token = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
        return new Issued(token, exp);
    }

    /** Đọc id người dùng từ token hợp lệ; token sai chữ ký / hết hạn / hỏng -> rỗng. */
    public Optional<Long> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Khóa ký: lấy từ cấu hình nếu có; nếu không thì sinh ngẫu nhiên 1 lần và lưu vào data/jwt.secret
     * để khởi động lại backend thì token cũ vẫn dùng được.
     */
    private static byte[] resolveSecret(AppProperties props) {
        String configured = props.jwt().secret();
        if (configured != null && !configured.isBlank()) {
            byte[] bytes = configured.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("APP_JWT_SECRET phải dài ít nhất 32 ký tự");
            }
            return bytes;
        }
        Path file = Path.of(props.dataDir(), "jwt.secret");
        try {
            if (Files.exists(file)) {
                return Base64.getDecoder().decode(Files.readString(file).trim());
            }
            byte[] bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, Base64.getEncoder().encodeToString(bytes));
            return bytes;
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc/ghi được " + file, e);
        }
    }
}
