package vn.aicf.mobile.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Cho backend phục vụ luôn giao diện PWA trong thư mục frontend/www.
 * Lợi ích: mở http://localhost:8090 trên trình duyệt / điện thoại cùng mạng là dùng được app,
 * và PWA gọi API CÙNG NGUỒN nên không vướng CORS. (App Android vẫn có bản sao giao diện riêng bên trong.)
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = props.frontendDir();
        if (dir == null || dir.isBlank() || !Files.isDirectory(Path.of(dir))) {
            log.info("Không phục vụ giao diện PWA (không thấy thư mục '{}'). Chỉ chạy REST API.", dir);
            return;
        }
        String location = Path.of(dir).toAbsolutePath().normalize().toUri().toString();
        log.info("Phục vụ giao diện PWA từ {}", location);
        registry.addResourceHandler("/**")
                .addResourceLocations(location)
                // Không cache lâu: sửa giao diện xong tải lại là thấy (service worker tự lo phần chạy offline)
                .setCacheControl(CacheControl.noCache());
    }

    /** Vào "/" thì trả index.html. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
