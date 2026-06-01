# Mobile Development - Lab 6 (OpenCV Document Filter)

Ứng dụng Android (Kotlin) thực hiện bài tập thực hành của môn học **Mobile Development (Lab 6)**. Dự án được thiết kế với giao diện Dark Mode hiện đại, hiệu ứng bo góc mượt mà và gam màu HSL thiết kế chuẩn Premium để xử lý ảnh tài liệu, loại bỏ hoàn toàn bóng đổ (Shadow Removal) bằng thư viện xử lý ảnh hàng đầu **OpenCV**.

## 🔗 GitHub Repository
*   **Repository URL:** [https://github.com/keydii1/Mobile-Lab-6](https://github.com/keydii1/Mobile-Lab-6)
*   **Tác giả:** Ho Hoang Son (hoson2k5@gmail.com)

---

## 🛠️ Công nghệ sử dụng (Tech Stack)
*   **Ngôn ngữ:** Kotlin
*   **UI/UX:** XML Layouts, Material Design 3, Gradient Backgrounds, Custom UI Cards, Progress indicators
*   **Xử lý ảnh:** **OpenCV SDK 4.9.0** (Tích hợp hiện đại thông qua Maven Central)
*   **Quản lý luồng:** Java SingleThreadExecutor & Android Handler (Xử lý tác vụ nặng ở luồng nền tránh đơ UI, trả kết quả về UI Thread an toàn)

---

## 📂 Các chức năng nổi bật trong ứng dụng

Dự án tích hợp đầy đủ các chức năng xử lý ảnh tài liệu thông minh thông qua màn hình điều hướng Dashboard trung tâm (`MainActivity`):

### 1. Tích hợp OpenCV SDK hiện đại
*   Thay vì phải cấu hình module thủ công phức tạp và lỗi thời bằng tệp zip SDK tải ngoài, dự án được tích hợp **OpenCV 4.9.0** trực tiếp thông qua **Maven Central** bằng cách khai báo:
    ```kotlin
    implementation("org.opencv:opencv:4.9.0")
    ```
*   Khởi tạo động OpenCV tại thời điểm khởi chạy ứng dụng bằng `OpenCVLoader.initDebug()`, cung cấp thông báo tức thì về trạng thái tải thư viện.

### 2. Thuật toán khử bóng đổ tài liệu chuyên sâu (`ShadowRemovalFilter.kt`)
Triển khai thuật toán xử lý ảnh số dựa trên gợi ý từ bài học (Slide 7-8) bằng ngôn ngữ **Kotlin** tối ưu:
1.  **Chuyển đổi không gian màu:** Chuyển đổi từ `RGB` sang `HSV` để tách biệt thông tin độ sáng (Value - V) khỏi màu sắc (Hue, Saturation).
2.  **Ước lượng nền (Background Illumination):** 
    *   Sử dụng phép nở ảnh `Imgproc.dilate` với nhân 7x7 nhằm ước lượng độ sáng nền và loại bỏ các chi tiết chữ đen nhỏ.
    *   Áp dụng bộ lọc mờ trung vị `Imgproc.medianBlur` với kích thước hạt 21 để làm mịn dải bóng đổ, tạo ra bản đồ nền sáng bị lỗi.
3.  **Khử bóng (Difference & Invert):**
    *   Tính toán sự khác biệt tuyệt đối `Core.absdiff` giữa kênh V gốc và kênh V nền mờ để xác định bóng.
    *   Đảo ngược ảnh bằng `Core.bitwise_not` để chuyển vùng chữ về màu đen gốc trên nền sáng.
4.  **Chuẩn hóa tương phản:** Áp dụng `Core.normalize` kéo giãn độ tương phản về dải chuẩn [0, 255] nhằm tối ưu hóa độ trắng của nền giấy và độ sắc nét của chữ.
5.  **Tái cấu trúc:** Ghép (merge) kênh V đã làm sạch với kênh H & S ban đầu, sau đó chuyển ngược về không gian màu `RGB` và xuất lại định dạng `Bitmap` của Android.
6.  **Tối ưu hiệu năng:** Toàn bộ tiến trình native Mat của OpenCV đều được gọi hàm `.release()` giải phóng bộ nhớ C++ ngay khi xử lý xong nhằm loại bỏ triệt để nguy cơ rò rỉ bộ nhớ (native memory leak).

### 3. Trải nghiệm người dùng thông minh (Wow Factors)
*   **Trình sinh tài liệu mẫu thông minh (`btnLoadSample`):** Tự động vẽ và tạo ra một trang tài liệu văn bản giả lập cực nét bằng canvas, sau đó phủ lên một lớp gradient đổ bóng chéo vô cùng chân thực (như bóng đổ của điện thoại khi chụp tài liệu). Giúp người dùng test ngay tính năng khử bóng tức thì mà không cần chuẩn bị sẵn ảnh.
*   **Chọn ảnh từ Thư viện (`btnSelectImage`):** Cho phép người dùng tự chụp hoặc chọn bất kỳ bức ảnh cuốn sách, tài liệu nào từ máy điện thoại thông qua `ActivityResultContracts` hiện đại.
*   **Màn hình so sánh trực quan:** Thiết kế thẻ ảnh Gốc (Original Card) và ảnh Đã lọc (Processed Card) cạnh nhau, giúp người dùng so sánh ngay lập tức hiệu quả khử bóng tuyệt vời của thuật toán.
*   **Đồng hồ chờ trực quan:** Khi nhấn chạy bộ lọc, một ProgressBar vòng tròn sẽ tự động xoay và vô hiệu hóa nút bấm tạm thời để thể hiện trạng thái xử lý bất đồng bộ mượt mà.

---

## 🚀 Hướng dẫn cài đặt & Chạy ứng dụng

### Bước 1: Mở dự án trong Android Studio
1.  Khởi động **Android Studio** (Phiên bản khuyến nghị: Hedgehog 2023.3.1 hoặc mới hơn).
2.  Chọn **File** -> **Open** và tìm đến thư mục chứa dự án: `/Users/hohoangson/Downloads/Lab6`.
3.  Đợi Gradle đồng bộ và tải thư viện OpenCV tự động thông qua Maven Central.

### Bước 2: Build và Chạy máy ảo / Thiết bị thật
1.  Đảm bảo máy ảo hoặc thiết bị chạy hệ điều hành Android API 26 trở lên (Android 8.0+).
2.  Nhấn nút **Run** (Biểu tượng tam giác xanh lá) trên thanh công cụ của Android Studio để bắt đầu biên dịch và cài đặt.

---

## 📝 Bản quyền bài tập
*   **Giảng viên hướng dẫn:** Trần Vinh Khiêm
*   **Đội ngũ phát triển tài liệu:** S3T - Smart Software System Team (01/03/2022)
