package vn.aicf.mobile.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.aicf.mobile.domain.Role;
import vn.aicf.mobile.domain.User;
import vn.aicf.mobile.repository.UserRepository;

/**
 * Tạo TÀI KHOẢN MẪU lần đầu chạy (khi bảng users còn trống), để thử app ngay:
 * <pre>
 *   admin / admin123   - ADMIN    (chủ quán)
 *   staff / staff123   - STAFF    (nhân viên)
 *   khach / khach123   - CUSTOMER (khách hàng)
 * </pre>
 * Không đặt trong data.sql vì mật khẩu phải được băm BCrypt bằng Java.
 * Tắt bằng biến môi trường APP_SEED_DEMO=false. Nhớ đổi mật khẩu khi dùng thật!
 * (Danh mục, món, bàn mẫu nằm trong db/data.sql.)
 */
@Slf4j
@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public DataSeeder(UserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seedDemoUsers() || users.count() > 0) {
            return;
        }
        create("admin", "admin123", "Chủ quán", Role.ADMIN);
        create("staff", "staff123", "Nhân viên pha chế", Role.STAFF);
        create("khach", "khach123", "Khách hàng mẫu", Role.CUSTOMER);
        log.info("Đã tạo tài khoản mẫu: admin/admin123, staff/staff123, khach/khach123");
    }

    private void create(String username, String password, String fullName, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(fullName);
        u.setRole(role);
        users.save(u);
    }
}
