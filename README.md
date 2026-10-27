# ReBook – Website quản lý chuỗi cửa hàng sách cũ

Đồ án cuối kỳ môn **Lập trình Web (WEPR330479)** – HCMUTE

**GitHub:** https://github.com/tranthinhhhh/rebook
**Đề tài 14:** Xây dựng website quản lý chuỗi cửa hàng sách cũ bằng Spring Boot + Thymeleaf + Bootstrap + JPA + MySQL + JWT + WebSocket + Cloudinary.

| Thành viên | MSSV | Phụ trách |
|---|---|---|
| Trần Tiến Thịnh | 22110236 | Guest, User, Shipper, chức năng chung (đăng ký/quên mật khẩu OTP, đăng nhập JWT) |
| Phan Thanh Thiện | 22110234 | Vendor, Manager, Admin, WebSocket chat & thông báo |

## Công nghệ
- Java 21, Spring Boot 3.3.5, Maven
- Thymeleaf + Thymeleaf Layout Dialect + Spring Security extras, Bootstrap 5.3, Bootstrap Icons, Chart.js
- Spring Data JPA (Hibernate) + MySQL 8
- Spring Security, BCrypt, JWT (jjwt 0.12) lưu trong cookie HttpOnly; REST API dùng `Authorization: Bearer`
- WebSocket STOMP + SockJS (chat khách – shop, thông báo đơn hàng realtime)
- JavaMailSender (Gmail SMTP) gửi OTP; Cloudinary lưu ảnh/video; VNPay sandbox

## Cấu trúc 3 tầng
```
src/main/java/vn/hcmute/rebook
├── controller/web   # Tầng trình bày: Controller trả về view Thymeleaf
├── controller/api   # REST API (JSON) dùng JWT
├── service          # Tầng nghiệp vụ: interface
├── service/impl     # Tầng nghiệp vụ: cài đặt
├── repository       # Tầng truy cập dữ liệu: Spring Data JPA
├── entity           # Thực thể JPA (+ enums)
├── dto              # Form, DTO, validation
├── security         # JWT, UserDetails, filter
├── config           # Security, WebSocket, MVC, dữ liệu mẫu
├── exception        # Xử lý lỗi tập trung
└── util             # Tiện ích định dạng
src/main/resources/templates   # layouts, fragments, auth, home, product, user, vendor, admin, shipper, error
```

## Hướng dẫn cài đặt và chạy

### 1. Yêu cầu
- JDK 21+ (Maven không bắt buộc vì đã có Maven Wrapper `mvnw`)
- MySQL 8.x

### 2. Tạo database
```sql
CREATE DATABASE rebook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'rebook'@'localhost' IDENTIFIED BY 'rebook123';
GRANT ALL ON rebook.* TO 'rebook'@'localhost';
```
Có 2 cách nạp dữ liệu:
- **Cách 1 (khuyên dùng):** để database trống, khi chạy lần đầu ứng dụng tự tạo bảng và dữ liệu mẫu (`DataSeeder`).
- **Cách 2:** import file `database/rebook.sql`: `mysql -u rebook -p rebook < database/rebook.sql`

Nếu dùng tài khoản MySQL khác, sửa `spring.datasource.*` trong `src/main/resources/application.properties`.

### 3. Cấu hình dịch vụ ngoài (không bắt buộc)
Đặt biến môi trường trước khi chạy (để trống thì ứng dụng vẫn chạy được):

| Biến | Ý nghĩa | Khi để trống |
|---|---|---|
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail và App Password để gửi OTP | Mã OTP được in ra console |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Lưu ảnh/video lên Cloudinary | Lưu vào thư mục `uploads/` |
| `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` | Mã website VNPay sandbox (đăng ký tại sandbox.vnpayment.vn/devreg) | Ẩn lựa chọn VNPay, chỉ dùng COD |

### 4. Chạy
```bash
git clone https://github.com/tranthinhhhh/rebook.git
cd rebook
```
Không cần cài Maven, dùng Maven Wrapper có sẵn trong project:
```bash
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```
Hoặc mở thư mục `rebook` bằng IntelliJ IDEA rồi chạy `RebookApplication`.
Mở http://localhost:8080

### 5. Tài khoản demo (mật khẩu chung: `123456a`)
| Vai trò | Email |
|---|---|
| Admin | admin@rebook.vn |
| Manager | manager@rebook.vn |
| Vendor | vendor1@rebook.vn … vendor4@rebook.vn |
| Shipper | shipper1@rebook.vn, shipper2@rebook.vn |
| User | user1@rebook.vn … user6@rebook.vn |
| User có shop chờ duyệt | vendor5@rebook.vn |

Mã giảm giá mẫu: `REBOOK10`, `FREESHIP` (toàn sàn), `THUDUC20` (ReBook Thủ Đức), `Q1SHIP50` (ReBook Quận 1).

## REST API
| Method | URL | Mô tả |
|---|---|---|
| POST | `/api/auth/login` | Body `{"email","password"}` → trả về JWT |
| GET | `/api/auth/me` | Thông tin người dùng (cần JWT) |
| GET | `/api/books?q=&categoryId=&sort=&page=` | Tìm kiếm sách |
| GET | `/api/books/{id}` | Chi tiết sách |
| GET/POST | `/api/cart` | Xem / thêm giỏ hàng (cần JWT) |
| GET | `/api/orders` | Đơn hàng của tôi (cần JWT) |
