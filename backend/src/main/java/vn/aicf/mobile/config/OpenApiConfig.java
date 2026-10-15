package vn.aicf.mobile.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tài liệu REST API tự sinh (OpenAPI 3) + Swagger UI tại http://localhost:8080/swagger-ui.html.
 * Bấm nút "Authorize", dán token lấy từ /api/auth/login là gọi thử được các API cần đăng nhập.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        String scheme = "bearer-jwt";
        return new OpenAPI()
                .info(new Info()
                        .title("AICF Coffee Mobile - REST API")
                        .version("1.0.0")
                        .description("API cho app di động đặt món quán cà phê: xác thực JWT / Firebase, thực đơn, "
                                + "đơn hàng, thông báo FCM, thống kê và Chat AI. Dữ liệu lưu trong SQLite."))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
