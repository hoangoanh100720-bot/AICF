package vn.aicf.mobile.service.ai;

import org.springframework.stereotype.Component;
import vn.aicf.mobile.config.AppProperties;
import vn.aicf.mobile.service.SettingsService;

/**
 * Đọc cấu hình AI đang có hiệu lực: ưu tiên giá trị Admin lưu trong bảng app_settings,
 * nếu trống thì lấy mặc định từ application.yml / biến môi trường (GEMINI_API_KEY, ANTHROPIC_API_KEY...).
 */
@Component
public class AiSettings {

    private final SettingsService settings;
    private final AppProperties.Ai defaults;

    public AiSettings(SettingsService settings, AppProperties props) {
        this.settings = settings;
        this.defaults = props.ai();
    }

    /** "gemini" hoặc "claude". */
    public String provider() {
        return settings.get(SettingsService.AI_PROVIDER, defaults.provider()).trim().toLowerCase();
    }

    public String geminiKey() {
        return settings.get(SettingsService.AI_GEMINI_KEY, defaults.geminiApiKey()).trim();
    }

    public String geminiModel() {
        return settings.get(SettingsService.AI_GEMINI_MODEL, defaults.geminiModel()).trim();
    }

    public String claudeKey() {
        return settings.get(SettingsService.AI_CLAUDE_KEY, defaults.claudeApiKey()).trim();
    }

    public String claudeModel() {
        return settings.get(SettingsService.AI_CLAUDE_MODEL, defaults.claudeModel()).trim();
    }
}
