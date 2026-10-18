# Luồng hoạt động các chức năng

Các sơ đồ viết bằng **Mermaid**: GitHub và khung xem trước Markdown của VS Code tự vẽ thành hình.
Muốn chèn vào Word / PowerPoint: dán từng khối vào <https://mermaid.live> → *Actions* → tải PNG/SVG.

| # | Sơ đồ | Loại |
|---|---|---|
| 1 | [Kiến trúc tổng thể](#1-kiến-trúc-tổng-thể) | thành phần |
| 2 | [Điều hướng màn hình theo vai trò](#2-điều-hướng-màn-hình-theo-vai-trò) (2a vai trò, 2b router) | flowchart |
| 3 | [Xử lý 1 request REST (bảo mật JWT)](#3-xử-lý-1-request-rest-bảo-mật-jwt) | sequence |
| 4 | [Đăng ký / đăng nhập mật khẩu](#4-đăng-ký--đăng-nhập-bằng-mật-khẩu) | sequence |
| 5 | [Đăng nhập Google (Firebase)](#5-đăng-nhập-google-firebase-authentication) | sequence |
| 6 | [Khách vãng lai → giỏ hàng → đặt món](#6-khách-vãng-lai--giỏ-hàng--đặt-món) | flowchart |
| 7 | [Đặt đơn (bên trong máy chủ)](#7-đặt-đơn-bên-trong-máy-chủ) | sequence |
| 8 | [Vòng đời trạng thái đơn](#8-vòng-đời-trạng-thái-đơn) | state |
| 9 | [Nhân viên xử lý đơn → khách nhận thông báo](#9-nhân-viên-xử-lý-đơn--khách-nhận-thông-báo) | sequence |
| 10 | [Thông báo trong app + đẩy FCM](#10-thông-báo-trong-app--thông-báo-đẩy-fcm) | sequence |
| 11 | [Chat AI](#11-chat-ai) | flowchart |
| 12 | [Quản lý thực đơn (CRUD tổng quát)](#12-quản-lý-thực-đơn-crud-tổng-quát) | sequence |
| 13 | [Thống kê doanh thu](#13-thống-kê-doanh-thu-admin) | sequence |
| 14 | [Dùng khi mất mạng (SQLite trên máy)](#14-dùng-khi-mất-mạng-sqlite-trên-điện-thoại) | flowchart |

---

## 1. Kiến trúc tổng thể

Một máy chủ duy nhất (Spring Boot, cổng 8090) vừa phục vụ giao diện PWA vừa cung cấp REST API.
App Android chứa sẵn bản sao giao diện, chỉ gọi REST API qua mạng.

```mermaid
flowchart TB
    subgraph DT["📱 ĐIỆN THOẠI / TRÌNH DUYỆT (frontend)"]
        direction LR
        APP["App AICF Coffee<br/>PWA trên trình duyệt hoặc app Android (Capacitor)<br/>cùng một bộ giao diện frontend/www"]
        LDB[("Lưu trên máy<br/>SQLite aicf_local (Android)<br/>localStorage (PWA)<br/>giỏ hàng · phiên · thực đơn tạm")]
        APP --- LDB
    end

    subgraph SV["🖥️ MÁY CHỦ SPRING BOOT :8090 (backend)"]
        direction LR
        WEB["Phục vụ giao diện PWA<br/>+ Swagger UI"]
        API["REST API /api/**<br/>Controller → Service → Repository"]
        DB[("SQLite<br/>aicf-mobile.db")]
        API --> DB
    end

    subgraph EXT["☁️ DỊCH VỤ NGOÀI (tùy chọn)"]
        direction LR
        FA["Firebase Auth<br/>đăng nhập Google"]
        FCM["Firebase Cloud Messaging<br/>thông báo đẩy"]
        AI["Gemini / Claude<br/>Chat AI"]
    end

    APP -- "① tải giao diện (PWA)" --> WEB
    APP -- "② gọi API: HTTP + JSON + JWT" --> API
    APP -. "③ đăng nhập Google → ID token" .-> FA
    API -. "④ kiểm tra ID token" .-> FA
    API -. "⑤ gửi thông báo" .-> FCM
    FCM -. "⑥ đẩy tới điện thoại" .-> APP
    API -. "⑦ câu hỏi + công cụ dữ liệu" .-> AI
```

---

## 2. Điều hướng màn hình theo vai trò

**2a. Mỗi vai trò thấy gì.** Khách chưa đăng nhập vẫn xem thực đơn và dùng giỏ hàng; tới lúc cần tài khoản mới phải đăng nhập.

```mermaid
flowchart TD
    START(["Mở app"]) --> HAS{"Đã đăng nhập?<br/>(token lưu trên máy)"}
    HAS -- "Chưa" --> G["👤 KHÁCH VÃNG LAI<br/>Trang đầu: Thực đơn<br/>Tab: Thực đơn · Giỏ hàng · Trợ lý AI · Đăng nhập"]
    HAS -- "Rồi" --> ROLE{"Vai trò?"}
    G -- "bấm Đặt món,<br/>mở Đơn hàng / Trợ lý AI" --> L["🔐 Đăng nhập / Đăng ký<br/>(nhớ màn đang mở: ?next=...)"]
    L -- "thành công" --> ROLE
    ROLE -- "CUSTOMER" --> C["🧑 KHÁCH HÀNG<br/>Quay lại màn đang làm dở (vd Giỏ hàng)<br/>Tab: Thực đơn · Giỏ hàng · Đơn hàng · Trợ lý AI · Tài khoản"]
    ROLE -- "STAFF" --> S["👷 NHÂN VIÊN<br/>Trang đầu: Bảng đơn cần làm<br/>Tab: Đơn cần làm · Thực đơn · Trợ lý AI · Tài khoản"]
    ROLE -- "ADMIN" --> A["👑 QUẢN TRỊ<br/>Trang đầu: Tổng quan doanh thu<br/>Tab: Tổng quan · Đơn hàng · Quản lý · Trợ lý AI · Tài khoản<br/>+ Người dùng, Cài đặt quán & AI"]
    C -. "Đăng xuất" .-> G
    S -. "Đăng xuất" .-> G
    A -. "Đăng xuất" .-> G
```

**2b. Router kiểm tra mỗi lần đổi màn** (`js/core/router.js`):

```mermaid
flowchart LR
    N(["Đổi màn hình<br/>vd #/orders/12"]) --> R1{"Có màn này?"}
    R1 -- "Không" --> HOME["Về trang đầu<br/>theo vai trò"]
    R1 -- "Có" --> R2{"Màn cần đăng nhập<br/>mà chưa đăng nhập?"}
    R2 -- "Đúng" --> LOGIN["#/login?next=/orders/12"]
    R2 -- "Không" --> R3{"Vai trò được phép?<br/>(vd /admin chỉ ADMIN)"}
    R3 -- "Không" --> HOME
    R3 -- "Có" --> OK["Tải code màn hình<br/>(import động) và vẽ"]
```

---

## 3. Xử lý 1 request REST (bảo mật JWT)

Mọi API đi qua cùng một đường: bộ lọc JWT → luật quyền → Controller → Service → Repository → SQLite. Lỗi ở bất kỳ bước nào đều được đổi thành cùng một dạng JSON.

```mermaid
sequenceDiagram
    autonumber
    participant App as App (HttpClient)
    participant F as JwtAuthFilter
    participant SC as SecurityConfig<br/>(luật theo đường dẫn)
    participant C as Controller
    participant S as Service
    participant R as Repository (JPA)
    participant DB as SQLite
    participant EH as GlobalExceptionHandler

    App->>F: GET /api/orders/my<br/>Authorization: Bearer <JWT>
    F->>F: kiểm tra chữ ký + hạn token
    F->>R: tìm người dùng theo id trong token
    R->>DB: SELECT * FROM users WHERE id = ?
    DB-->>F: người dùng (active?)
    F->>SC: đặt người đang gọi + ROLE_CUSTOMER
    alt Thiếu / sai token mà API cần đăng nhập
        SC-->>App: 401 {"message": "Bạn cần đăng nhập..."}
    else Không đủ vai trò (vd khách gọi /api/stats)
        SC-->>App: 403 {"message": "Bạn không có quyền..."}
    else Hợp lệ
        SC->>C: chuyển tới OrderController.my()
        C->>S: orderService.myOrders(me)
        S->>R: findByUserIdOrderByIdDesc(me.id)
        R->>DB: SELECT ... FROM orders WHERE user_id = ? ORDER BY id DESC
        DB-->>S: danh sách đơn
        S-->>C: List<OrderResponse> (DTO)
        C-->>App: 200 JSON
    end
    Note over S,EH: Service ném ApiException (404, 409...) hoặc dữ liệu vi phạm @Valid (400)<br/>→ GlobalExceptionHandler trả {status, message, details}
```

---

## 4. Đăng ký / đăng nhập bằng mật khẩu

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant App as App (login.js)
    participant API as AuthController
    participant S as AuthService
    participant DB as SQLite (users)
    participant J as JwtService
    participant L as SQLite / localStorage<br/>trên máy

    alt Đăng ký (tab Đăng ký)
        U->>App: họ tên, tên đăng nhập, mật khẩu, email
        App->>API: POST /api/auth/register
        API->>API: @Valid (độ dài, ký tự, email)
        API->>S: register()
        S->>DB: tên đăng nhập / email đã có chưa?
        alt Trùng
            S-->>App: 409 "Tên đăng nhập đã có người dùng"
        else Chưa có
            S->>S: băm mật khẩu BCrypt, vai trò = CUSTOMER
            S->>DB: INSERT INTO users
        end
    else Đăng nhập
        U->>App: tên đăng nhập + mật khẩu
        App->>API: POST /api/auth/login
        API->>S: login()
        S->>DB: SELECT user WHERE username = ?
        alt Sai tên hoặc sai mật khẩu
            S-->>App: 401 "Sai tên đăng nhập hoặc mật khẩu" (cùng 1 câu)
        else Tài khoản bị khóa
            S-->>App: 403 "Tài khoản đã bị khóa"
        end
    end
    S->>J: issue(user) — ký HMAC, hạn 72 giờ
    J-->>App: 200/201 {token, expiresAt, user}
    App->>L: lưu session.token, session.user
    App->>App: khách: quay lại màn ?next (vd giỏ hàng)<br/>nhân viên / quản trị: về trang chủ theo vai trò
```

---

## 5. Đăng nhập Google (Firebase Authentication)

```mermaid
sequenceDiagram
    autonumber
    actor U as Khách
    participant App as App (PWA)
    participant G as Firebase Auth<br/>(cửa sổ Google)
    participant API as AuthController<br/>POST /api/auth/firebase
    participant FS as FirebaseService<br/>(Admin SDK)
    participant DB as SQLite (users)

    U->>App: bấm "Tiếp tục với Google"
    App->>G: signInWithPopup(GoogleAuthProvider)
    G-->>App: Firebase ID token
    App->>API: {idToken}
    API->>FS: verifyIdToken(idToken)
    alt Máy chủ chưa cấu hình Firebase
        FS-->>App: 503 "Máy chủ chưa cấu hình Firebase"
    else Token sai / hết hạn
        FS-->>App: 401
    else Hợp lệ
        FS-->>API: uid, email, tên, ảnh
        API->>DB: tìm theo firebase_uid
        alt Chưa có
            API->>DB: tìm theo email (liên kết tài khoản đã đăng ký trước)
            alt Vẫn chưa có
                API->>DB: tạo tài khoản CUSTOMER mới (không mật khẩu)
            end
        end
        API->>DB: lưu firebase_uid, ảnh đại diện
        API-->>App: {token JWT của hệ thống, user}
    end
```

---

## 6. Khách vãng lai → giỏ hàng → đặt món

```mermaid
flowchart TD
    A(["Mở app (chưa đăng nhập)"]) --> B["Thực đơn<br/>GET /api/categories + /api/products"]
    B --> C["Tìm / lọc danh mục<br/>(lọc ngay trên máy)"]
    C --> D["Bấm ＋ thêm món"]
    D --> E[("Lưu vào giỏ<br/>cart_items (SQLite) / localStorage")]
    E --> F["Mở Giỏ hàng<br/>sửa số lượng, ghi chú từng món"]
    F --> G{"Đã đăng nhập?"}
    G -- "Chưa" --> H["Nút 'Đăng nhập để đặt món'<br/>→ /login?next=/cart"]
    H --> I{"Có tài khoản?"}
    I -- "Có" --> J["Đăng nhập"]
    I -- "Chưa" --> K["Đăng ký nhanh / Google"]
    J --> L["Tự quay lại Giỏ hàng<br/>(món còn nguyên)"]
    K --> L
    G -- "Rồi" --> M
    L --> M["Chọn Tại quán + bàn / Mang đi,<br/>cách thanh toán, ghi chú cho quán"]
    M --> N{"Tại quán mà chưa chọn bàn?"}
    N -- "Đúng" --> M
    N -- "Không" --> O["POST /api/orders<br/>(chỉ gửi id món + số lượng)"]
    O --> P{"Kết quả"}
    P -- "201" --> Q["Xóa giỏ trên máy<br/>→ Chi tiết đơn (Chờ xác nhận)"]
    P -- "409 món tạm hết / 400 dữ liệu sai" --> R["Báo lỗi, giữ nguyên giỏ"] --> F
```

---

## 7. Đặt đơn (bên trong máy chủ)

```mermaid
sequenceDiagram
    autonumber
    participant App as App (cart.js)
    participant C as OrderController
    participant S as OrderService
    participant DB as SQLite
    participant N as NotificationService
    participant P as PushSender (@Async)

    App->>C: POST /api/orders {orderType, tableId, items[productId, quantity, note]}
    C->>C: @Valid: giỏ không rỗng, số lượng 1–99
    C->>S: create(me, request)
    Note over S,DB: Cả khối dưới chạy trong 1 GIAO DỊCH:<br/>lỗi ở bất kỳ món nào thì không lưu gì
    opt Tại quán
        S->>DB: bàn tồn tại và đang hoạt động?
    end
    loop Từng món trong giỏ
        S->>DB: SELECT product (còn trong thực đơn? đang bán?)
        S->>S: chụp lại tên + ĐƠN GIÁ HIỆN TẠI vào order_items
    end
    S->>S: tổng tiền = Σ đơn giá × số lượng (máy chủ tự tính)
    S->>DB: INSERT orders (mã tạm) + order_items
    S->>S: mã đơn = DH<ngày>-<id> (dựa trên id nên không trùng)
    S->>N: notifyStaff("Đơn mới DH...", ...)
    N->>DB: INSERT notifications cho mọi STAFF + ADMIN
    N->>P: gửi FCM tới thiết bị của nhân viên (chạy nền)
    S-->>App: 201 OrderResponse (mã đơn, tổng tiền, trạng thái PENDING)
```

---

## 8. Vòng đời trạng thái đơn

Luật chuyển trạng thái nằm ở **một chỗ** (`OrderStatus.canMoveTo`). Chuyển sai luật → 409.

```mermaid
stateDiagram-v2
    [*] --> PENDING: Khách đặt món
    PENDING --> CONFIRMED: Nhân viên "Nhận đơn"
    CONFIRMED --> PREPARING: "Bắt đầu pha"
    PREPARING --> READY: "Đã xong"
    READY --> COMPLETED: "Đã giao & thu tiền"<br/>(tự đánh dấu đã thanh toán)
    PENDING --> CANCELLED: Khách tự hủy<br/>hoặc nhân viên hủy
    CONFIRMED --> CANCELLED: Nhân viên hủy
    PREPARING --> CANCELLED: Nhân viên hủy
    READY --> CANCELLED: Nhân viên hủy
    COMPLETED --> [*]
    CANCELLED --> [*]

    note right of PENDING
        Chờ xác nhận — khách còn tự hủy được
    end note
    note right of COMPLETED
        Chỉ đơn COMPLETED được tính vào doanh thu
    end note
```

---

## 9. Nhân viên xử lý đơn → khách nhận thông báo

```mermaid
sequenceDiagram
    autonumber
    actor NV as Nhân viên
    participant B as Bảng đơn (staff-board.js)
    participant API as OrderController
    participant S as OrderService
    participant DB as SQLite
    participant N as NotificationService
    participant K as App của khách

    loop Mỗi 10 giây (khi đang mở bảng)
        B->>API: GET /api/orders/active
        API-->>B: đơn PENDING / CONFIRMED / PREPARING / READY, cũ trước
    end
    NV->>B: bấm "Nhận đơn" / "Bắt đầu pha" / "Đã xong" / "Đã giao & thu tiền"
    B->>API: PATCH /api/orders/{id}/status {status}
    API->>API: @PreAuthorize: chỉ STAFF / ADMIN
    API->>S: updateStatus(id, status)
    S->>S: canMoveTo? (sai luật → 409)
    S->>DB: UPDATE orders SET status = ? (COMPLETED → paid = 1)
    S->>N: notifyUsers([chủ đơn], "Đơn DH...: Đã xong, chờ lấy", ...)
    N->>DB: INSERT notifications
    N-->>K: FCM (nếu đã bật Firebase)
    API-->>B: 200 đơn mới → vẽ lại bảng
    par Khách đang mở chi tiết đơn
        K->>API: GET /api/orders/{id} mỗi 15 giây → thanh tiến trình cập nhật
    and Khách ở màn khác
        K->>API: GET /api/notifications/unread-count mỗi 30 giây → chấm đỏ ở tab Tài khoản
    end
```

---

## 10. Thông báo trong app + thông báo đẩy (FCM)

```mermaid
sequenceDiagram
    autonumber
    actor U as Người dùng
    participant App as App
    participant FB as Firebase (JS SDK /<br/>plugin Push Android)
    participant API as Máy chủ
    participant DB as SQLite
    participant FCM as Firebase Cloud Messaging

    Note over U,FCM: ① Đăng ký thiết bị (1 lần, sau khi đăng nhập)
    U->>App: Tài khoản → "Bật thông báo đẩy"
    App->>FB: xin quyền thông báo + lấy mã thiết bị
    FB-->>App: FCM token
    App->>API: POST /api/devices {token, platform: WEB | ANDROID}
    API->>DB: lưu device_tokens (mã đã có thì chuyển sang người dùng hiện tại)

    Note over U,FCM: ② Khi có sự kiện (đơn mới, đổi trạng thái, khách hủy)
    API->>DB: INSERT notifications (luôn lưu, kể cả chưa bật Firebase)
    API->>DB: lấy các mã thiết bị của người nhận
    API->>FCM: sendEachForMulticast (chạy nền)
    FCM-->>App: thông báo hiện trên điện thoại / trình duyệt
    FCM-->>API: mã nào đã hết hiệu lực
    API->>DB: xóa các mã hết hiệu lực

    Note over U,FCM: ③ Người dùng bấm vào thông báo
    U->>App: bấm thông báo (data.orderId)
    App->>App: mở #/orders/{orderId}
    App->>API: POST /api/notifications/{id}/read (khi mở từ hộp thư)

    Note over U,FCM: ④ Đăng xuất
    App->>API: DELETE /api/devices?token=... (không nhận thông báo của tài khoản cũ)
```

---

## 11. Chat AI

Tận dụng thiết kế Trợ lý AI của dự án AICF: chọn nhà cung cấp (Strategy), AI chỉ lấy số liệu qua **công cụ** (function calling), lỗi thì tự chuyển sang chế độ cơ bản.

```mermaid
flowchart TD
    Q(["Người dùng gửi câu hỏi<br/>POST /api/chat"]) --> H["Lấy 12 lượt chat gần nhất<br/>+ lưu câu hỏi vào chat_messages"]
    H --> K{"Dịch vụ AI đang chọn<br/>đã có khóa API?"}
    K -- "Chưa" --> OFF
    K -- "Có" --> P{"ai.provider"}
    P -- "gemini" --> GEM["GeminiProvider<br/>(REST)"]
    P -- "claude" --> CLA["ClaudeProvider<br/>(SDK anthropic-java)"]
    GEM --> L1
    CLA --> L1
    L1["① Gửi cho AI: lời dặn hệ thống + lịch sử + câu hỏi<br/>+ công cụ được phép theo vai trò"] --> L2{"② AI muốn<br/>gọi công cụ?"}
    L2 -- "Có" --> L3["③ AiToolRegistry chạy công cụ<br/>(kiểm tra quyền lần 2), lấy số liệu thật"]
    L3 -- "④ gửi kết quả JSON, lặp lại<br/>(tối đa 6 vòng)" --> L1
    L2 -- "Không" --> L5["Câu trả lời bằng chữ<br/>mode = LLM"]
    L1 -- "lỗi mạng / khóa sai / hết lượt" --> NOTICE["Kèm ghi chú:<br/>'AI đang gặp sự cố...'"]
    NOTICE --> OFF["OfflineAssistant — chế độ cơ bản<br/>trả lời theo từ khóa, vẫn dùng số liệu thật<br/>mode = OFFLINE"]
    L5 --> SAVE["Lưu câu trả lời vào chat_messages"]
    OFF --> SAVE
    SAVE --> R(["Trả về {reply, mode, notice, suggestions}"])
    L3 -.- TOOLS["Công cụ:<br/>get_menu · get_shop_info · get_my_orders (mọi người)<br/>get_sales_summary · get_active_orders (nhân viên, quản trị)"]
```

---

## 12. Quản lý thực đơn (CRUD tổng quát)

Món, Danh mục, Bàn dùng **chung một khung** ở cả hai phía: `crudPage()` + `createCrudRepository()` trên app, `CrudController` + `CrudService` trên máy chủ.

```mermaid
sequenceDiagram
    autonumber
    actor AD as Quản trị
    participant UI as crudPage() + formSheet()<br/>(form tự sinh từ cấu hình)
    participant R as createCrudRepository('products')
    participant C as CrudController<br/>(ProductController)
    participant S as CrudService<br/>(ProductService)
    participant DB as SQLite

    AD->>UI: mở Quản lý → tab Món
    UI->>R: list()
    R->>C: GET /api/products
    C->>S: findAll()
    S->>DB: SELECT products JOIN categories
    DB-->>UI: danh sách → vẽ các dòng
    AD->>UI: "Thêm món" → điền form → Lưu
    UI->>UI: kiểm tra required / min trên máy
    UI->>R: create(data)
    R->>C: POST /api/products (chỉ ADMIN)
    C->>S: create() — khung chung của lớp cha
    S->>S: validate() [lớp con: danh mục tồn tại? tên trùng?]
    S->>S: newEntity() + apply() [lớp con chép trường]
    S->>DB: INSERT
    S-->>UI: 201 → toast "Đã thêm món" → tải lại danh sách
    AD->>UI: bấm 🗑 xóa danh mục
    UI->>AD: hộp xác nhận
    UI->>C: DELETE /api/categories/{id}
    C->>S: delete() → beforeDelete() [còn món? → 409]
    S-->>UI: 204 hoặc 409 "Danh mục còn 5 món..."
```

---

## 13. Thống kê doanh thu (Admin)

```mermaid
sequenceDiagram
    autonumber
    actor AD as Quản trị
    participant UI as Tổng quan (admin-dashboard.js)
    participant C as StatsController
    participant S as StatsService
    participant DB as SQLite

    AD->>UI: mở Tổng quan / chọn ngày
    UI->>C: GET /api/stats/dashboard?date=YYYY-MM-DD (chỉ ADMIN)
    C->>S: dashboard(date)
    S->>DB: COUNT đơn trong ngày (tổng / hoàn thành / hủy)
    S->>DB: SUM(total_amount) đơn COMPLETED trong ngày
    loop 7 ngày gần nhất
        S->>DB: SUM doanh thu từng ngày
    end
    S->>DB: TOP 5 món: GROUP BY product_name (tên đã chụp lại lúc đặt)
    S-->>UI: {revenue, orderCount, completedCount, cancelledCount, activeCount, topProducts, last7Days}
    UI->>UI: thẻ số liệu + biểu đồ cột 7 ngày + danh sách món bán chạy
```

---

## 14. Dùng khi mất mạng (SQLite trên điện thoại)

```mermaid
flowchart TD
    S(["Mở app"]) --> I["initLocalDb()"]
    I --> N{"Đang chạy trong app Android<br/>và có plugin SQLite?"}
    N -- "Có" --> SQ[("SQLite aicf_local<br/>kv · cart_items · cached_products")]
    N -- "Không (PWA)" --> WS[("localStorage<br/>cùng các hàm kvGet, cartList...")]
    SQ --> M
    WS --> M
    M["Mở Thực đơn: productRepo.menu()"] --> NET{"Gọi GET /api/products được?"}
    NET -- "Được" --> OK["Hiện thực đơn mới<br/>+ lưu bản sao vào cached_products"]
    NET -- "Mất mạng (lỗi status 0)" --> C{"Có bản sao?"}
    C -- "Có" --> OLD["Hiện thực đơn đã lưu<br/>+ dải báo 'Đang mất mạng'"]
    C -- "Không" --> ERR["Màn 'Mất kết nối' + nút Thử lại"]
    OK --> CART["Thêm món vào giỏ<br/>(ghi vào cart_items, không cần mạng)"]
    OLD --> CART
    CART --> ORDER["Đặt món: cần mạng<br/>(máy chủ tính giá thật)"]

    subgraph PWA["PWA: service worker (sw.js)"]
        W1["Tải giao diện: có mạng lấy bản mới + cập nhật cache,<br/>mất mạng dùng bản trong cache"]
        W2["/api/**: luôn đi mạng"]
    end
```
