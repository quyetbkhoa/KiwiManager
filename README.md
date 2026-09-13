# 🥝 Kiwi Manager

> **Trung tâm quản trị và kho ứng dụng tập trung cho hệ sinh thái Wear OS & Android**  
> Lấy cảm hứng từ triết lý của Vanced / ReVanced Manager và Morphe.

<p align="center">
  <img src="app/src/main/res/drawable/ic_kiwi_mascot.xml" width="128" height="128" alt="Kiwi Mascot" />
</p>

---

## 🌟 Tính Năng Nổi Bật

- **📦 Kho ứng dụng tập trung (Centralized App Hub):** Liệt kê, theo dõi và cập nhật toàn bộ ứng dụng trong hệ sinh thái (cả bản Mobile Phone Companion và Wear OS Watch).
- **🔄 Quản lý cập nhật kép (Dual-Platform Updater):**
  - **Điện thoại:** Tải về và cài đặt trực tiếp qua `PackageInstaller` của Android.
  - **Đồng hồ:** Tích hợp bộ máy **Wireless ADB** (`dev.mobile:dadb`), kết nối không dây qua Wi-Fi / Hotspot và tự động thực thi `pm install -r -d -t -g`.
- **✨ Tự cập nhật chính mình (Self-Update):** Kiwi Manager xuất hiện trong chính kho ứng dụng, cho phép tự phát hiện bản phát hành mới và cài đè mượt mà.
- **🎨 Hệ thống 3 Theme Material Design 3:**
  - ☀️ **Light Theme:** Sáng thanh lịch với sắc xanh kiwi tươi mát.
  - 🌙 **Dark Theme:** Tối tiêu chuẩn dịu mắt.
  - 🖤 **Pure OLED Black:** Đen tuyệt đối (`#000000`), tiết kiệm pin tối đa cho màn hình AMOLED và đồng bộ phong cách với Wear OS.
- **⚡ Làm mới kho thủ công (Manual Refresh):** Nút cập nhật tức thì trên TopAppBar cùng hỗ trợ chế độ kép:
  - *Dynamic Mode:* Tự động quét và phân tích GitHub Releases mới nhất qua API.
  - *Static Mode:* Sử dụng `catalog.json` offline-first.

---

## 📱 Hệ Sinh Thái Ứng Dụng Hỗ Trợ

| Ứng dụng | Nền tảng | Package Name | Mô tả |
| :--- | :---: | :--- | :--- |
| **Kiwi Manager** | Phone | `com.kiwi.manager` | Ứng dụng quản trị trung tâm và kho cập nhật |
| **Gemini for Wear OS** | Phone + Watch | `com.oppowatch.gemini` | Trợ lý AI Gemini cho Wear OS 2+ |
| **Open Navigation Wear** | Phone + Watch | `com.opennavigation.wear` | Điều hướng Google Maps trên đồng hồ |
| **Custom Vibration** | Watch | `com.oppowatch.haptics` | Tùy biến rung nâng cao cho Wear OS |

---

## 🛠️ Kiến Trúc Công Nghệ

- **Ngôn ngữ:** 100% Kotlin hiện đại
- **Giao diện:** Jetpack Compose + Material Design 3 (M3)
- **Kiến trúc:** Modern Android Architecture (Clean Architecture + Unidirectional Data Flow)
  - `ui/`: Compose Screens, ViewModels, Themes, Components
  - `domain/`: Models (`AppInfo`, `AdbDevice`, `InstallResult`), Business Logic
  - `data/`: Repositories (`CatalogRepository`, `AdbRepository`, `DownloadRepository`), DataStore Preferences
- **Kết nối ADB:** `dev.mobile:dadb:1.2.6` (thuần JVM/Android ADB client)
- **Đóng gói & Tối ưu:** R8 / ProGuard Minified (~1.6 MB Release APK)

---

## 🚀 Hướng Dẫn Cài Đặt Lên Đồng Hồ (Wireless ADB)

1. **Kết nối mạng:** Kết nối điện thoại và đồng hồ vào cùng một mạng Wi-Fi (hoặc phát Hotspot từ điện thoại rồi kết nối đồng hồ vào).
2. **Bật gỡ lỗi:** Trên đồng hồ, vào *Cài đặt > Tùy chọn nhà phát triển > Bật Gỡ lỗi ADB và Gỡ lỗi qua Wi-Fi*.
3. **Kết nối:** Mở Kiwi Manager, chọn tab **ADB**, nhập IP và Port (mặc định 5555) hiển thị trên đồng hồ rồi bấm **Kết nối**.
4. **Cài đặt:** Vào trang chi tiết ứng dụng, bấm **Cập nhật / Cài đặt Wear** — Kiwi Manager sẽ tự động đẩy APK sang `/data/local/tmp/` và thực thi cài đặt đè `pm install -r -d -t -g`.

---

## 📜 Quy Chuẩn Đặt Phiên Bản (SemVer)

- **versionName:** `vMAJOR.MINOR.PATCH` (Ví dụ: `1.0.0`, `1.3.4`)
- **versionCode:** $\text{MAJOR} \times 10000 + \text{MINOR} \times 100 + \text{PATCH}$ (Ví dụ: `10000`, `10304`)
- **Release Asset Naming:**
  - `KiwiManager-v{version}.apk`
  - `{AppName}-Phone-v{version}.apk`
  - `{AppName}-Watch-v{version}.apk`

---

## 📄 Bản quyền & Tài liệu

Xem tài liệu đặc tả chi tiết tại [REQUIREMENTS.md](REQUIREMENTS.md).
