package vn.aicf.mobile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aicf.mobile.domain.AppSetting;
import vn.aicf.mobile.repository.AppSettingRepository;

/**
 * Đọc / ghi cấu hình trong bảng APP_SETTINGS.
 * Quy tắc ưu tiên: giá trị trong CSDL (Admin sửa trên app) > giá trị mặc định truyền vào (từ application.yml).
 */
@Service
public class SettingsService {

    // Tên các khóa cấu hình dùng trong hệ thống (gom một chỗ, tránh gõ sai chuỗi)
    public static final String SHOP_NAME = "shop.name";
    public static final String SHOP_ADDRESS = "shop.address";
    public static final String SHOP_HOURS = "shop.hours";
    public static final String AI_PROVIDER = "ai.provider";
    public static final String AI_GEMINI_KEY = "ai.gemini_api_key";
    public static final String AI_GEMINI_MODEL = "ai.gemini_model";
    public static final String AI_CLAUDE_KEY = "ai.claude_api_key";
    public static final String AI_CLAUDE_MODEL = "ai.claude_model";

    private final AppSettingRepository repository;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    /** Lấy giá trị; chưa có hoặc rỗng thì trả {@code defaultValue}. */
    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        return repository.findById(key)
                .map(AppSetting::getValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue == null ? "" : defaultValue);
    }

    /** Ghi (thêm mới hoặc cập nhật) 1 cấu hình. Giá trị null được lưu thành chuỗi rỗng. */
    @Transactional
    public void set(String key, String value) {
        AppSetting s = repository.findById(key).orElseGet(() -> new AppSetting(key, null));
        s.setValue(value == null ? "" : value.trim());
        repository.save(s);
    }
}
