# 🍿 CineTaste - Nền tảng Kết hợp Điện Ảnh & Ẩm Thực

**CineTaste** là một nền tảng dựa trên kiến trúc Vi dịch vụ (Microservices) độc đáo, mang đến trải nghiệm kết hợp hoàn hảo giữa việc thưởng thức phim ảnh và nghệ thuật ẩm thực. Bằng cách tích hợp dữ liệu phim từ TMDB và sức mạnh của Trí tuệ Nhân tạo (AI), hệ thống giúp người dùng khám phá, sáng tạo và chia sẻ các công thức nấu ăn mang đậm phong cách điện ảnh.

---

## ✨ Tính năng chính

* 👤 **Quản lý Người Dùng (User Service)**
    * Đăng ký, Đăng nhập an toàn (hỗ trợ JWT & OAuth).
    * Xây dựng Hồ sơ Khẩu vị (Flavor Profile) cá nhân hóa.
    * Hệ thống theo dõi (Follow) giữa các người dùng.
* 🍳 **Cộng đồng Ẩm Thực (Recipe Service)**
    * Đăng tải, xem và chia sẻ các công thức nấu ăn chi tiết (nguyên liệu, các bước thực hiện).
    * Đánh giá (Rating), bình luận (Comment) và tương tác thả cảm xúc (Comment Reactions) trên bài viết.
    * Lưu công thức yêu thích.
* 🎬 **Trải nghiệm Điện Ảnh (TMDB Integration)**
    * Kết nối trực tiếp với API của TMDB để lấy thông tin phim.
    * Gợi ý các món ăn/thức uống phù hợp để thưởng thức cùng bộ phim yêu thích.
* 🤖 **Trợ lý AI Thông Minh (AI Service)**
    * **Phân tích món ăn (Analyze Dish):** Nhận diện và đánh giá món ăn từ hình ảnh.
    * **Sáng tạo theo chủ đề (Create by Theme):** Tạo ra công thức nấu ăn độc quyền dựa trên bối cảnh, nhân vật hoặc không khí của một bộ phim.
    * **Nhận xét công thức (Critique Dish):** Đưa ra phản hồi và mẹo cải thiện công thức nấu ăn.
    * **Kiểm duyệt hình ảnh (Image Validator):** Tự động xác thực hình ảnh món ăn được tải lên.

---

## 🏗 Cấu trúc Kiến trúc (Architecture)

Dự án được xây dựng theo mô hình Microservices, bao gồm các thành phần sau:

* **`api-gateway` (Spring Cloud Gateway):** Cổng giao tiếp duy nhất định tuyến mọi request từ Client đến các service bên dưới, đồng thời xử lý xác thực ban đầu.
* **`user-service` (Spring Boot):** Chịu trách nhiệm về định danh (Identity), hồ sơ người dùng, xác thực JWT.
* **`recipe-service` (Spring Boot):** Quản lý core logic về bài viết công thức, bình luận, đánh giá, yêu thích và gọi sang hệ thống phim.
* **`ai_service` (Python):** Dịch vụ độc lập xử lý các tác vụ liên quan đến Machine Learning và LLMs (tích hợp API AI).

---

## 🛠 Công nghệ sử dụng

* **Ngôn ngữ lập trình:** Java 17+, Python 3.x
* **Framework Backend:** Spring Boot, Spring Cloud, Spring Data JPA, Spring Security.
* **Database Migration:** Flyway (`V1`, `V2` SQL scripts).
* **AI & Xử lý Dữ liệu:** Python (cùng các thư viện AI chuyên dụng).
* **Deployment & DevOps:** Docker, Docker Compose, Maven.

---

## 🚀 Hướng dẫn Cài đặt & Chạy dự án (Local Development)

### Yêu cầu hệ thống (Prerequisites)
* [Docker](https://www.docker.com/) và [Docker Compose](https://docs.docker.com/compose/)
* [Java 17](https://jdk.java.net/17/) (Nếu muốn chạy local không qua Docker)
* [Maven](https://maven.apache.org/) 
* [Python 3.10+](https://www.python.org/) (Dành cho AI Service)

### Các bước thiết lập

1. **Clone repository:**
   ```bash
   git clone <đường-dẫn-repo-của-bạn>
   cd cinetasteproject
