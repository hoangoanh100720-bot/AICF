# REST API

- Địa chỉ gốc: `http://localhost:8080`. Tài liệu tương tác (Swagger UI): `/swagger-ui.html`, đặc tả OpenAPI: `/v3/api-docs`.
- Dữ liệu vào/ra: JSON (UTF-8). Ngày giờ dạng ISO `2026-10-15T09:30:00`.
- Xác thực: gửi header `Authorization: Bearer <token>` (token lấy từ `/api/auth/login`, `/register` hoặc `/firebase`, hạn 72 giờ).
- Ký hiệu quyền: 🌐 mọi người · 🔑 đã đăng nhập · 👷 STAFF hoặc ADMIN · 👑 ADMIN.

## Quy ước REST

| Phương thức | Ý nghĩa | Mã thành công |
|---|---|---|
| `GET` | đọc | 200 |
| `POST` | tạo mới / thực hiện hành động | 201 (tạo mới) hoặc 200 |
| `PUT` | thay toàn bộ | 200 |
| `PATCH` | sửa một phần (vd chỉ trạng thái) | 200 |
| `DELETE` | xóa | 204 (không có nội dung) |

Lỗi luôn cùng 1 dạng:

```json
{ "status": 409, "error": "Conflict", "message": "Danh mục còn 5 món, hãy chuyển hoặc xóa các món trước",
  "details": [], "timestamp": "2026-10-15T09:30:00" }
```

| Mã | Khi nào |
|---|---|
| 400 | dữ liệu sai (`details` liệt kê từng trường, vd `"name: không được để trống"`) |
| 401 | chưa đăng nhập / token sai / hết hạn / sai mật khẩu |
| 403 | không đủ quyền |
| 404 | không tìm thấy (cả khi xem đơn của người khác – không tiết lộ đơn có tồn tại) |
| 409 | xung đột: trùng tên, sai luồng trạng thái, xóa dữ liệu đang được dùng |
| 503 | tính năng cần Firebase nhưng máy chủ chưa cấu hình |

## 1. Xác thực

| | API | Mô tả |
|---|---|---|
| 🌐 | `POST /api/auth/register` | Đăng ký khách: `{username, password, fullName, email?, phone?}` → 201 `{token, expiresAt, user}` |
| 🌐 | `POST /api/auth/login` | `{username, password}` → `{token, expiresAt, user}` |
| 🌐 | `POST /api/auth/firebase` | `{idToken}` (Firebase ID token sau khi đăng nhập Google) → `{token, expiresAt, user}` |

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d "{\"username\":\"khach\",\"password\":\"khach123\"}"
```

## 2. Người dùng

| | API | Mô tả |
|---|---|---|
| 🔑 | `GET /api/users/me` | Hồ sơ của tôi |
| 🔑 | `PUT /api/users/me` | `{fullName, email?, phone?}` |
| 🔑 | `PUT /api/users/me/password` | `{currentPassword, newPassword}` → 204 |
| 👑 | `GET /api/users` | Danh sách tài khoản |
| 👑 | `PUT /api/users/{id}` | `{role, active}` – đổi vai trò / khóa (không tự khóa chính mình) |

## 3. Thực đơn (CRUD tổng quát – cùng 5 API cho cả 3 tài nguyên)

| | API | Mô tả |
|---|---|---|
| 🌐 | `GET /api/categories` · `GET /api/categories/{id}` | Danh mục |
| 👑 | `POST` / `PUT /{id}` / `DELETE /{id}` `/api/categories` | `{name, description?, icon?, sortOrder}` |
| 🌐 | `GET /api/products?categoryId=&q=&available=true` | Món, có lọc theo danh mục / từ khóa / đang bán |
| 👑 | `POST` / `PUT /{id}` / `DELETE /{id}` `/api/products` | `{categoryId, name, description?, price, imageUrl?, available}` |
| 🌐 | `GET /api/tables` · `GET /api/tables/{id}` | Bàn |
| 👑 | `POST` / `PUT /{id}` / `DELETE /{id}` `/api/tables` | `{name, area?, seats, active}` |

## 4. Đơn hàng

| | API | Mô tả |
|---|---|---|
| 🔑 | `POST /api/orders` | Đặt đơn (ví dụ dưới) → 201 |
| 🔑 | `GET /api/orders/my` | Đơn của tôi |
| 🔑 | `GET /api/orders/{id}` | Chi tiết (chủ đơn hoặc nhân viên) |
| 🔑 | `POST /api/orders/{id}/cancel` | Khách tự hủy khi còn `PENDING`: `{reason?}` |
| 👷 | `GET /api/orders/active` | Đơn đang xử lý |
| 👷 | `GET /api/orders?date=2026-10-15` | Mọi đơn trong ngày |
| 👷 | `PATCH /api/orders/{id}/status` | `{status, cancelReason?}` – theo luật PENDING→CONFIRMED→PREPARING→READY→COMPLETED, hoặc →CANCELLED |
| 👷 | `PATCH /api/orders/{id}/paid` | Ghi nhận đã thanh toán |

```json
POST /api/orders
{
  "orderType": "DINE_IN", "tableId": 2, "paymentMethod": "CASH", "note": "Mang ra nhanh giúp mình",
  "items": [ { "productId": 1, "quantity": 2, "note": "ít đá" }, { "productId": 4, "quantity": 1 } ]
}
→ 201
{ "id": 7, "code": "DH20261015-0007", "status": "PENDING", "statusLabel": "Chờ xác nhận",
  "tableName": "Bàn 02", "totalAmount": 103000, "paid": false,
  "items": [ { "productName": "Cà phê sữa đá", "unitPrice": 29000, "quantity": 2, "lineTotal": 58000, "note": "ít đá" }, … ] }
```

Mỗi lần đặt đơn / đổi trạng thái / hủy, máy chủ tạo thông báo (bảng `notifications`) và gửi FCM nếu đã cấu hình Firebase.

## 5. Thông báo & thiết bị

| | API | Mô tả |
|---|---|---|
| 🔑 | `GET /api/notifications?limit=30` | Hộp thư |
| 🔑 | `GET /api/notifications/unread-count` | `{unread}` |
| 🔑 | `POST /api/notifications/{id}/read` | Đánh dấu đã đọc |
| 🔑 | `POST /api/notifications/read-all` | Đánh dấu tất cả |
| 🔑 | `POST /api/devices` | Lưu mã FCM: `{token, platform: WEB\|ANDROID\|IOS}` → 204 |
| 🔑 | `DELETE /api/devices?token=…` | Gỡ mã khi đăng xuất → 204 |

## 6. Chat AI

| | API | Mô tả |
|---|---|---|
| 🔑 | `GET /api/chat/status` | `{mode: LLM\|OFFLINE, provider, suggestions[]}` |
| 🔑 | `POST /api/chat` | `{message}` → `{reply, mode, notice?, suggestions[]}` |
| 🔑 | `GET /api/chat?limit=50` | Lịch sử |
| 🔑 | `DELETE /api/chat` | Xóa lịch sử → 204 |

## 7. Thống kê & hệ thống

| | API | Mô tả |
|---|---|---|
| 👑 | `GET /api/stats/dashboard?date=` | `{revenue, orderCount, completedCount, cancelledCount, activeCount, topProducts[], last7Days[]}` |
| 🌐 | `GET /api/health` | `{status: "UP"}` – app Android dùng để kiểm tra địa chỉ máy chủ |
| 🌐 | `GET /api/shop` | `{name, address, hours, firebaseEnabled, aiMode}` |
| 👑 | `PUT /api/settings/shop` | `{name, address, hours}` |
| 👑 | `GET /api/settings/ai` | `{provider, geminiModel, geminiKeySet, claudeModel, claudeKeySet}` (không trả khóa) |
| 👑 | `PUT /api/settings/ai` | `{provider, geminiApiKey?, geminiModel?, claudeApiKey?, claudeModel?}` – khóa để trống = giữ nguyên |
