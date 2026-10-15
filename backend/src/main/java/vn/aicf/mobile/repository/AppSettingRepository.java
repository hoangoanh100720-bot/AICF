package vn.aicf.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.aicf.mobile.domain.AppSetting;

/** Truy vấn bảng APP_SETTINGS (khóa chính là chuỗi setting_key). */
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
