package vn.aicf.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KIỂM THỬ TÍCH HỢP toàn bộ REST API trên CSDL SQLite thật (file tạm, xóa sau khi chạy):
 * khởi động cả ứng dụng Spring, gọi API qua MockMvc (giả lập HTTP, không mở cổng mạng).
 * Chạy:  mvnw test
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {

    private static Path dataDir;

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    /** Trỏ CSDL vào thư mục tạm riêng cho lần chạy test (không đụng dữ liệu thật). */
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        dataDir = Files.createTempDirectory("aicf-test-");
        r.add("app.data-dir", () -> dataDir.toString().replace('\\', '/'));
        r.add("app.frontend-dir", () -> "");
        r.add("app.ai.gemini-api-key", () -> "");   // test luôn dùng chế độ cơ bản, không gọi AI thật
        r.add("app.ai.claude-api-key", () -> "");
    }

    // ---------- tiện ích ----------

    private String login(String user, String pass) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("token").asText();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode read(MockHttpServletRequestBuilder b, int expectedStatus) throws Exception {
        String body = mvc.perform(b).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : json.readTree(body);
    }

    // ---------- các bài kiểm thử ----------

    @Test
    void publicMenuIsSeededFromDataSql() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/products").param("categoryId", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Cà phê"));
        mvc.perform(get("/api/shop")).andExpect(jsonPath("$.aiMode").value("OFFLINE"))
                .andExpect(jsonPath("$.firebaseEnabled").value(false));
    }

    @Test
    void securityRules() throws Exception {
        mvc.perform(get("/api/orders/my")).andExpect(status().isUnauthorized());
        String customer = login("khach", "khach123");
        mvc.perform(auth(get("/api/stats/dashboard"), customer)).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/categories"), customer).content("{\"name\":\"X\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"khach\",\"password\":\"sai\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void fullOrderFlow() throws Exception {
        // Khách đăng ký -> đặt đơn tại bàn 1
        JsonNode reg = read(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test_order\",\"password\":\"123456\",\"fullName\":\"Người thử\"}"), 201);
        String customer = reg.path("token").asText();
        JsonNode order = read(auth(post("/api/orders"), customer).content("""
                {"orderType":"DINE_IN","tableId":1,"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}
                """), 201);
        long id = order.path("id").asLong();
        assertThat(order.path("status").asText()).isEqualTo("PENDING");
        assertThat(order.path("totalAmount").asLong()).isEqualTo(2 * 29000 + 25000);   // giá lấy từ CSDL
        assertThat(order.path("code").asText()).startsWith("DH").endsWith(String.format("%04d", id));

        // Nhân viên đi đúng luồng trạng thái; nhảy cóc / lùi lại bị từ chối 409
        String staff = login("staff", "staff123");
        read(auth(patch("/api/orders/" + id + "/status"), staff).content("{\"status\":\"READY\"}"), 409);
        for (String s : new String[]{"CONFIRMED", "PREPARING", "READY", "COMPLETED"}) {
            read(auth(patch("/api/orders/" + id + "/status"), staff).content("{\"status\":\"" + s + "\"}"), 200);
        }
        JsonNode done = read(auth(get("/api/orders/" + id), customer), 200);
        assertThat(done.path("paid").asBoolean()).isTrue();

        // Khách nhận thông báo cho mỗi lần đổi trạng thái
        JsonNode unread = read(auth(get("/api/notifications/unread-count"), customer), 200);
        assertThat(unread.path("unread").asLong()).isGreaterThanOrEqualTo(4);

        // Người khác không xem được đơn này (404, không lộ là đơn có tồn tại)
        String other = login("khach", "khach123");
        read(auth(get("/api/orders/" + id), other), 404);

        // Thống kê của Admin có doanh thu
        String admin = login("admin", "admin123");
        JsonNode dash = read(auth(get("/api/stats/dashboard"), admin), 200);
        assertThat(dash.path("revenue").asLong()).isGreaterThanOrEqualTo(83000);
    }

    @Test
    void customerCanCancelOnlyPendingOrder() throws Exception {
        String customer = login("khach", "khach123");
        JsonNode order = read(auth(post("/api/orders"), customer).content("""
                {"orderType":"TAKEAWAY","items":[{"productId":3,"quantity":1}]}
                """), 201);
        long id = order.path("id").asLong();
        JsonNode cancelled = read(auth(post("/api/orders/" + id + "/cancel"), customer).content("{}"), 200);
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        read(auth(post("/api/orders/" + id + "/cancel"), customer).content("{}"), 409);
    }

    @Test
    void validationAndBusinessRules() throws Exception {
        String customer = login("khach", "khach123");
        // Giỏ rỗng -> 400 kèm chi tiết trường
        read(auth(post("/api/orders"), customer).content("{\"orderType\":\"TAKEAWAY\",\"items\":[]}"), 400);
        // Tại chỗ mà không chọn bàn -> 400
        read(auth(post("/api/orders"), customer).content("{\"orderType\":\"DINE_IN\",\"items\":[{\"productId\":1,\"quantity\":1}]}"), 400);

        String admin = login("admin", "admin123");
        // Không xóa được danh mục còn món -> 409
        read(auth(delete("/api/categories/1"), admin), 409);
        // CRUD tổng quát: thêm, sửa, xóa 1 bàn
        JsonNode t = read(auth(post("/api/tables"), admin).content("{\"name\":\"Bàn test\",\"seats\":2,\"active\":true}"), 201);
        read(auth(post("/api/tables"), admin).content("{\"name\":\"Bàn test\",\"seats\":2,\"active\":true}"), 409); // trùng tên
        long tid = t.path("id").asLong();
        JsonNode upd = read(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tables/" + tid), admin)
                .content("{\"name\":\"Bàn test 2\",\"seats\":6,\"active\":false}"), 200);
        assertThat(upd.path("seats").asInt()).isEqualTo(6);
        read(auth(delete("/api/tables/" + tid), admin), 204);
        read(auth(get("/api/tables/" + tid), admin), 404);
    }

    @Test
    void chatAnswersInOfflineModeWithRealData() throws Exception {
        String admin = login("admin", "admin123");
        JsonNode r = read(auth(post("/api/chat"), admin).content("{\"message\":\"Thực đơn có gì?\"}"), 200);
        assertThat(r.path("mode").asText()).isEqualTo("OFFLINE");
        assertThat(r.path("reply").asText()).contains("Cà phê sữa đá");
        JsonNode history = read(auth(get("/api/chat"), admin), 200);
        assertThat(history.size()).isEqualTo(2);   // câu hỏi + câu trả lời được lưu vào chat_messages
    }
}
