# AICF Coffee Mobile — Đồ án môn Lập trình thiết bị di động

Ứng dụng di động đặt món cho quán cà phê, phát triển lại từ dự án **AICF** theo hướng gọn, dễ đọc, dễ chấm:

- **Frontend** (`frontend/`): app di động viết bằng HTML/CSS/JavaScript thuần (ES Modules, không cần công cụ build), chạy được theo 2 cách:
  - **PWA**: mở trên trình duyệt điện thoại → "Thêm vào màn hình chính", chạy toàn màn hình, mở được khi mất mạng.
  - **App Android** đóng gói bằng **Capacitor** (`frontend/android`), lưu dữ liệu cục bộ bằng **SQLite**.
- **Backend** (`backend/`): **REST API Spring Boot 3 (Java 21)**, cơ sở dữ liệu **SQLite**, xác thực **JWT**, tích hợp **Firebase** (đăng nhập Google + thông báo đẩy FCM) và **Chat AI** dùng lại từ AICF (Gemini / Claude, có chế độ cơ bản khi không có khóa API).

Bỏ phần web quản lý và app desktop của AICF; mọi chức năng (khách, nhân viên, quản trị) nằm trong một app di động.

```
AICF-DoAn/
├── backend/                     Spring Boot REST API
│   ├── src/main/java/vn/aicf/mobile/
│   │   ├── common/              phần dùng lại: CrudService, CrudController, BaseEntity, xử lý lỗi
│   │   ├── config/              bảo mật JWT, CORS, Swagger, dữ liệu mẫu, phục vụ PWA
│   │   ├── domain/              Entity (ánh xạ bảng SQLite)
│   │   ├── repository/          truy vấn Spring Data JPA
│   │   ├── service/             nghiệp vụ (đơn hàng, thống kê, thông báo…)
│   │   │   ├── ai/              Chat AI: Gemini, Claude, công cụ dữ liệu, chế độ cơ bản
│   │   │   └── firebase/        Firebase Admin SDK: xác thực Google, gửi FCM
│   │   └── web/                 Controller REST + DTO
│   ├── src/main/resources/
│   │   ├── application.yml      cấu hình (có chú thích)
│   │   └── db/schema.sql        LƯỢC ĐỒ SQLITE (chú thích từng bảng, từng cột)
│   │       db/data.sql          dữ liệu mẫu
│   ├── src/test/…/ApiFlowTest   kiểm thử tích hợp toàn bộ API
│   └── run.cmd
├── frontend/
│   ├── www/                     giao diện app (PWA)
│   │   ├── index.html, manifest.webmanifest, sw.js (service worker)
│   │   ├── css/app.css          design token + component
│   │   └── js/
│   │       ├── core/            HTTP client, router, store, DOM, định dạng
│   │       ├── data/            repository REST, SQLite cục bộ, phiên đăng nhập, giỏ hàng
│   │       ├── firebase/        cấu hình, đăng nhập Google, thông báo đẩy
│   │       ├── ui/              component tái sử dụng, trang CRUD tổng quát
│   │       └── pages/           từng màn hình
│   ├── android/                 dự án Android (Capacitor) – mở bằng Android Studio
│   ├── tests/e2e.mjs            kiểm thử giao diện đầu-cuối (Chrome headless)
│   ├── capacitor.config.json
│   └── package.json
└── docs/
    ├── flows.md                 sơ đồ luồng hoạt động các chức năng (Mermaid)
    ├── database.md              mô tả cơ sở dữ liệu (ERD, từng bảng)
    └── api.md                   danh sách REST API + ví dụ
```

---

## 1. Chạy thử nhanh (5 phút)

**Cần:** JDK 21+ (đã có sẵn trên máy: `C:\Program Files\Java\jdk-21.0.11`). Không cần cài Maven, không cần cài CSDL.

```bat
cd backend
run.cmd
```

Lần đầu Maven tải thư viện mất 1–3 phút. Khi thấy `Started MobileBackendApplication`:

| Mở | Để làm gì |
|---|---|
| <http://localhost:8090> | **App (PWA)**. Trên Chrome máy tính: F12 → bật chế độ điện thoại (Ctrl+Shift+M) cho giống app |
| <http://localhost:8090/swagger-ui.html> | **Tài liệu REST API** tự sinh, gọi thử từng API (bấm *Authorize*, dán token) |
| `backend/data/aicf-mobile.db` | **File SQLite** – mở bằng [DB Browser for SQLite](https://sqlitebrowser.org/) để xem bảng |

**Khách không cần tài khoản để xem:** mở app là vào thẳng *Thực đơn*, tìm món, thêm vào giỏ. Chỉ khi bấm *Đặt món* (hoặc mở *Đơn hàng*, *Trợ lý AI*) app mới mời đăng nhập / đăng ký, xong tự quay lại giỏ hàng với món còn nguyên. Đơn hàng cần gắn với tài khoản để khách theo dõi đơn, nhận thông báo, tự hủy đơn và để quán chống đơn ảo.

Tài khoản mẫu (tạo tự động lần đầu):

| Tên đăng nhập | Mật khẩu | Vai trò | Vào app thấy |
|---|---|---|---|
| `khach` | `khach123` | Khách hàng | Thực đơn, Giỏ hàng, Đơn hàng, Trợ lý AI, Tài khoản |
| `staff` | `staff123` | Nhân viên | Bảng đơn cần làm, Thực đơn, Trợ lý AI |
| `admin` | `admin123` | Quản trị | Tổng quan doanh thu, Đơn hàng, Quản lý món/danh mục/bàn, Người dùng, Cài đặt AI |

**Dùng trên điện thoại thật (PWA):** điện thoại và máy tính cùng Wi-Fi → mở `http://<IP máy tính>:8090`.
(Muốn cài PWA lên màn hình chính và nhận Web Push cần HTTPS; với `localhost` trên máy tính thì Chrome cho phép sẵn.)

### Kịch bản demo gợi ý
1. **Chưa đăng nhập**: mở app → tìm "latte", lọc danh mục → thêm món vào giỏ → **tắt hẳn app rồi mở lại: giỏ vẫn còn** (SQLite/LocalDb).
2. Giỏ hàng → *Đăng nhập để đặt món* → đăng nhập `khach / khach123` → app quay lại giỏ → chọn bàn, ghi chú "ít đá" → *Đặt món* → màn chi tiết đơn có thanh tiến trình.
3. Mở cửa sổ ẩn danh, đăng nhập `staff` → *Đơn cần làm* → *Nhận đơn → Bắt đầu pha → Đã xong*.
4. Quay lại cửa sổ khách: trạng thái tự cập nhật, chấm đỏ ở *Tài khoản → Thông báo* (có Firebase thì nhận thêm thông báo đẩy).
5. *Trợ lý AI*: khách hỏi "Gợi ý cho mình 1 món", "Đơn của tôi tới đâu rồi?"; admin hỏi "Doanh thu hôm nay?".
6. Đăng nhập `admin` → *Tổng quan* (doanh thu, biểu đồ 7 ngày, món bán chạy) → *Quản lý* thêm/sửa/xóa món, bàn.

---

## 2. App Android (Capacitor)

File cài sẵn: `frontend/android/app/build/outputs/apk/debug/app-debug.apk` (đã build và chạy thử trên máy ảo Pixel 7).

Build lại sau khi sửa giao diện:

```bat
cd frontend
npm install               (lần đầu)
npx cap sync android      (chép www/ vào app Android + cập nhật plugin)
npx cap open android      (mở Android Studio → Run)
:: hoặc build APK bằng dòng lệnh:
cd android && gradlew.bat assembleDebug
```

- **Địa chỉ máy chủ:** app trên máy ảo mặc định gọi `http://10.0.2.2:8090` (= máy tính chạy máy ảo). Điện thoại thật: ở màn Đăng nhập mở *Địa chỉ máy chủ* → nhập `http://<IP máy tính>:8090` → *Kiểm tra*.
- **SQLite trên điện thoại:** plugin `@capacitor-community/sqlite`, CSDL `aicf_local` gồm 3 bảng `kv`, `cart_items`, `cached_products` (xem `frontend/www/js/data/local-db.js`). Trên trình duyệt (PWA) không có SQLite nên dùng localStorage với cùng giao diện hàm.
- Plugin dùng: `@capacitor-community/sqlite`, `@capacitor/push-notifications`, `@capacitor/app` (nút Back cứng).

---

## 3. Firebase (tùy chọn – app vẫn chạy đầy đủ khi chưa cấu hình)

| Tính năng Firebase | Dùng ở đâu |
|---|---|
| **Authentication (Google)** | Nút *Tiếp tục với Google* (PWA). App lấy ID token → `POST /api/auth/firebase` → backend kiểm tra bằng Firebase Admin SDK → cấp JWT riêng |
| **Cloud Messaging (FCM)** | Đơn đổi trạng thái → khách nhận thông báo đẩy; có đơn mới → nhân viên nhận thông báo. Backend gửi bằng Admin SDK (`PushSender`), app nhận qua service worker (PWA) hoặc plugin Push (Android) |

Các bước:
1. Tạo dự án tại <https://console.firebase.google.com>.
2. **Backend:** *Project settings → Service accounts → Generate new private key* → lưu thành `backend/data/firebase-service-account.json` → khởi động lại backend (log báo `Firebase: đã kết nối dự án …`).
3. **App web:** *Project settings → General → Add app (Web)* → chép cấu hình vào `frontend/www/js/firebase/firebase-config.js`.
   *Authentication → Sign-in method* → bật **Google**. *Cloud Messaging → Web Push certificates* → tạo khóa → dán vào `VAPID_KEY`.
4. **App Android:** *Add app (Android)*, package `vn.aicf.mobile` → tải `google-services.json` vào `frontend/android/app/` → đặt `NATIVE_PUSH_ENABLED = true` → `npx cap sync android` và build lại.
5. Trong app: *Tài khoản → Bật thông báo đẩy*.

> File service account là **bí mật** (đã có trong `.gitignore`). Cấu hình web trong `firebase-config.js` là khóa công khai phía client, đưa vào app được.

---

## 4. Chat AI (tận dụng từ dự án AICF)

Giữ nguyên kiến trúc Trợ lý AI của AICF, rút gọn cho đồ án:

- `LlmProvider` (mẫu **Strategy**): `GeminiProvider` (REST) và `ClaudeProvider` (SDK chính thức `anthropic-java`, model mặc định `claude-opus-5-5`, có bật dự phòng phía máy chủ khi bị bộ lọc an toàn từ chối). Thêm hãng AI mới = thêm 1 lớp.
- `AiToolRegistry`: AI **không tự bịa số liệu** mà gọi "công cụ" đọc dữ liệu thật: `get_menu`, `get_shop_info`, `get_my_orders` (mọi người), `get_sales_summary`, `get_active_orders` (chỉ nhân viên/quản trị – kiểm tra quyền 2 lớp).
- `OfflineAssistant`: **chế độ cơ bản** trả lời theo từ khóa khi chưa có khóa API hoặc AI đang lỗi → demo không cần Internet.
- Lịch sử chat lưu trong bảng `chat_messages`.

Bật AI thật: *Admin → Tài khoản → Cài đặt quán & AI* → chọn dịch vụ, nhập khóa API (lưu trong bảng `app_settings`, không bao giờ trả lại cho app). Hoặc đặt biến môi trường trước khi chạy: `set GEMINI_API_KEY=...` (hoặc `set AI_PROVIDER=claude` + `set ANTHROPIC_API_KEY=...`).

---

## 5. Kiến trúc & tái sử dụng code

### Backend – phân lớp
```
Controller (web)  →  Service  →  Repository  →  Entity  ⇄  SQLite
     DTO + @Valid      nghiệp vụ    Spring Data JPA   @Entity    schema.sql
```
| Thành phần tái sử dụng | Dùng lại ở |
|---|---|
| `common/CrudService<E, REQ, RES>` (Template Method): khung thêm/đọc/sửa/xóa + móc `validate`, `beforeDelete`, `findAll(filters)` | `CategoryService`, `ProductService`, `TableService` (mỗi lớp ~50 dòng) |
| `common/CrudController<REQ, RES>`: sinh sẵn 5 API REST chuẩn | `CategoryController`, `ProductController`, `TableController` (chỉ khai báo đường dẫn) |
| `common/BaseEntity`: `id`, `created_at`, `updated_at` tự điền | mọi Entity có 3 cột này |
| `common/GlobalExceptionHandler` + `ApiException`: mọi lỗi trả cùng 1 dạng JSON | toàn bộ API |
| `common/LocalDateTimeConverter`: ngày giờ ⇄ TEXT trong SQLite | tự áp dụng mọi cột ngày giờ |
| `OrderStatus.canMoveTo()`: luật chuyển trạng thái đặt ở 1 chỗ | API nhân viên, khách tự hủy |
| `NotificationService`: lưu hộp thư + đẩy FCM | đơn mới, đổi trạng thái, khách hủy |

### Frontend – các lớp
| Thư mục | Vai trò | Ví dụ tái sử dụng |
|---|---|---|
| `core/` | hạ tầng không phụ thuộc nghiệp vụ | `HttpClient` (gắn JWT, đổi lỗi), `Router` (route + phân quyền), `createStore` (Observer) |
| `data/` | truy cập dữ liệu (Repository) | `createCrudRepository('categories')` cho mọi tài nguyên CRUD; `localDb` (Adapter: SQLite ↔ localStorage) |
| `ui/` | component dùng chung | `formSheet()` form tự sinh từ cấu hình trường; `asyncView()` tải/rỗng/lỗi/thử lại; **`crudPage()`** dựng cả màn quản lý từ 1 đoạn cấu hình (dùng cho Món, Danh mục, Bàn) |
| `pages/` | màn hình | mỗi file export `render(ctx)` |

---

## 6. Kiểm thử

| Lệnh | Kiểm tra |
|---|---|
| `cd backend && run.cmd test` | 6 bài kiểm thử tích hợp (JUnit + MockMvc trên SQLite thật): bảo mật, luồng đơn hàng đầy đủ, luật trạng thái, ràng buộc dữ liệu, CRUD, chat |
| `cd frontend && node tests/e2e.mjs http://localhost:8090` | 40 bước kiểm thử giao diện trên Chrome ẩn (cỡ màn hình điện thoại), chụp ảnh từng màn vào `tests/ket-qua/` – dùng làm hình minh họa báo cáo |

---

## 7. Công nghệ

| Phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.5 (Web, Data JPA, Security, Validation), Hibernate + SQLite dialect, sqlite-jdbc, JJWT, Firebase Admin SDK 9, anthropic-java, springdoc-openapi (Swagger), Lombok |
| CSDL | SQLite (máy chủ: `aicf-mobile.db`; điện thoại: `aicf_local`) |
| Frontend | HTML5, CSS (design token, chế độ tối tự động), JavaScript ES Modules, Service Worker, Web App Manifest, Firebase JS SDK 10 |
| Di động | Capacitor 8 (Android), @capacitor-community/sqlite, @capacitor/push-notifications |

Tài liệu chi tiết: [docs/flows.md](docs/flows.md) (sơ đồ luồng hoạt động) · [docs/database.md](docs/database.md) · [docs/api.md](docs/api.md)
