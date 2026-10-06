# Trạng thái dự án - Spend Like a Millionaire

**Cập nhật lần cuối: 2026-10-06 · Phiên bản: v1.0.0 (tag `v1.0.0`) · Trạng thái: code xong, chờ publish**

> Mở file này đầu tiên mỗi khi quay lại dự án. Mọi việc còn lại đều nằm ở mục 2.

## 1. Đã xong (không cần làm lại)

- App Android offline (Kotlin + Jetpack Compose), một module `app`, không có backend.
- Onboarding 4 bước (chào, ngôn ngữ EN/VI, tiền tệ, chọn người), shop dạng lưới ảnh, giỏ hàng, % đã tiêu, chia sẻ hóa đơn, màn Settings và ghi công ảnh.
- 32 người (17 tỷ phú, 15 người nổi tiếng, 30 người có ảnh chân dung), 312 món đồ (267 món có ảnh thật từ Wikimedia Commons, còn lại dùng emoji; tên và brand thật).
- Firebase là **tùy chọn**: không có `google-services.json` app vẫn build và chạy đầy đủ. Có file thì tự bật Crashlytics, Analytics, Remote Config.
- Chất lượng: 135 unit test, 1 test E2E (emulator), lint sạch, CI GitHub Actions xanh, bản release R8 đã chạy thử.
- Đã review độc lập 3 lần và sửa hết lỗi. Nhánh `main` là bản cuối; các nhánh khác đã xóa.
- Tài liệu phát hành đã viết sẵn: `docs/release-checklist.md`, `docs/store-listing.md`, `docs/privacy-policy.md`.

## 2. Việc còn lại để publish (làm theo thứ tự)

Việc có dấu **(nhờ Claude)** thì chỉ cần nói với Claude ở phiên mới, ví dụ "wire signing config".

### A. Tài khoản (làm song song, vì chờ lâu)
- [ ] **Google Play Console**: đăng ký (25 USD một lần), xác minh danh tính, bật xác thực 2 bước. Tài khoản cá nhân tạo sau 13/11/2023 phải chạy closed test **12 tester liên tục 14 ngày** trước khi lên production (tổ chức thì không). Có thể phải xác minh thiết bị Android thật.
- [ ] **Tìm 12 tester** (bạn bè, nhóm...) và lập danh sách email Google của họ. Đây là khâu chậm nhất.
- [ ] **Firebase**: tạo project, thêm app Android với package `com.michaeo04.spendlikeamillionaire`, tải `google-services.json` vào `app/` (đã gitignore, không commit). Bật Crashlytics, Analytics, Remote Config; tạo tham số `net_worth_overrides` mặc định `{}`.

### B. Khóa ký
- [ ] Tạo upload keystore **ngoài repo** và backup 2 nơi (mất là không cập nhật app được):
  `keytool -genkeypair -v -keystore D:\secrets\upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
- [ ] Tạo `keystore.properties` ở thư mục gốc (đã gitignore): `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
- [ ] Thêm `signingConfigs.release` đọc file trên vào `app/build.gradle.kts` **(nhờ Claude)**.
- [ ] Bật **Play App Signing** khi tải bản đầu tiên lên Play Console.

### C. Chính sách riêng tư và tài nguyên store
- [ ] GitHub repo > Settings > Pages > Deploy from branch `main`, thư mục `/docs`. Kiểm tra mở được `https://michaeo04.github.io/spend-like-a-millionaire/privacy-policy` (link này đang được dùng trong Settings của app).
- [ ] Icon 512x512, feature graphic 1024x500, ít nhất 2 ảnh chụp màn hình **(nhờ Claude tạo)**. Dùng màn hình có món chung chung, tránh logo thương hiệu và mặt người nổi tiếng.
- [ ] Điền thông tin store từ `docs/store-listing.md`.

### D. Build và nộp
- [ ] `./gradlew bundleRelease` ra AAB ở `app/build/outputs/bundle/release/`. Cài thử trên điện thoại thật.
- [ ] Play Console, mục **App content**: link chính sách riêng tư, khai không có quảng cáo (v1), không đăng nhập, bảng câu hỏi độ tuổi, **Data safety** (dữ liệu Firebase: nhật ký crash, sự kiện sử dụng, mã định danh thiết bị).
- [ ] Tải AAB lên **Internal testing** kiểm tra nhanh, rồi **Closed testing** mời 12 tester và giữ 14 ngày.
- [ ] Nộp đơn xin production, chờ duyệt, phát hành dần (20% rồi 100%), theo dõi Crashlytics.

### E. Sau khi lên production
- [ ] **AdMob (milestone M9)** **(nhờ Claude)**: tạo tài khoản và ad unit; Claude code quảng cáo, hộp thoại đồng ý UMP, và cờ điều kiện trên Remote Config. **Trước khi phát hành bản có quảng cáo** phải cập nhật chính sách riêng tư, Data safety và bảng xếp hạng độ tuổi.
- [ ] Remote Config cho điều kiện hiển thị **(nhờ Claude)**: ví dụ bật/tắt nhóm tỷ phú hay người nổi tiếng. Đã chốt là KHÔNG dùng Remote Config cho catalog.

Ước tính thời gian: khoảng 3-4 tuần kể từ lúc có tài khoản, chủ yếu do 14 ngày closed test.

## 3. Quyết định đã chốt

| Mục | Quyết định |
|---|---|
| Tên app / package | Spend Like a Millionaire / `com.michaeo04.spendlikeamillionaire` (package **không đổi được** sau khi phát hành) |
| Giấy phép repo | MIT |
| Phiên bản | `versionName 1.0.0`, `versionCode 1`; minSdk 26, compileSdk 37, targetSdk 36 |
| Dữ liệu | Offline, nằm trong app (`app/src/main/assets`). Không server, không tài khoản người dùng |
| Cập nhật không cần release | Chỉ tài sản, nguồn, tháng của người đã có, qua Remote Config (`net_worth_overrides`). Đổi giá, thêm món, thêm ảnh thì cần release |
| Remote Config sau này | Dùng cho điều kiện hiển thị (nhóm người, quảng cáo), không dùng cho catalog |
| Ảnh | Chỉ lấy từ Wikimedia Commons (CC0, public domain, CC BY, CC BY-SA), ghi công trong Settings > Image credits. Ảnh sai chủ đề thì bỏ, dùng emoji |
| Quảng cáo | AdMob làm sau cùng; hiện chỉ có `AdsGateway` rỗng |

## 4. Rủi ro đã biết

- **Thương hiệu và quyền hình ảnh cá nhân**: catalog dùng tên và ảnh sản phẩm brand thật, và có chân dung người thật. Giấy phép Commons không bao phủ quyền sử dụng hình ảnh cá nhân. Chi tiết và cách giảm rủi ro ở `docs/release-checklist.md` mục 3b, 3c. Đừng đưa tên người hay brand vào tiêu đề, mô tả hay ảnh store.
- **Giá món đồ là ước tính** không tra nguồn từng món (đã được chủ dự án duyệt).
- **Tài sản người nổi tiếng** là số liệu Forbes tại 01/03/2026 (Ronaldo 2026-10, Messi 2026-06), có thể lệch so với hiện tại.
- Ông Amancio Ortega và bà Nguyễn Thị Phương Thảo chưa có ảnh (dùng chữ cái đầu). Ông Jay-Z dùng ảnh trắng đen chất lượng vừa phải.
- Onboarding chưa lưu trạng thái khi app bị hệ thống kill giữa chừng (quay về màn chào). Chấp nhận được cho bản đầu.
- Chưa có Ambani và Masayoshi Son vì chưa lấy được số liệu thời gian thực chính xác.

## 5. Chạy và kiểm tra (Windows)

Môi trường: JDK 17, Android SDK, emulator và Android Studio nằm ở `D:\Android` (biến môi trường mức User: `ANDROID_HOME`, `GRADLE_USER_HOME`...). AVD: `Pixel_8_API_36`.

```
./gradlew testDebugUnitTest assembleDebug lintDebug     # kiểm tra chính
adb uninstall com.michaeo04.spendlikeamillionaire        # bắt buộc trước khi chạy E2E
./gradlew connectedDebugAndroidTest                      # E2E trên emulator
emulator -avd Pixel_8_API_36                             # mở máy ảo
```

Cập nhật dữ liệu (xem README mục "Photos and credits"):
```
python tools/build_catalog.py     # catalog-source.tsv  -> catalog.json
python tools/build_people.py      # people-source.tsv   -> people.json
python tools/fetch_images.py      # tải ảnh Commons (có thể chạy lại, tiếp tục từ chỗ dở)
```

## 6. Lịch sử

- PR #1 nền móng và tính năng, PR #2 sửa lỗi sau review, PR #3 ảnh, catalog brand, onboarding mới, nhân vật, review cuối.
- Tag `v1.0.0` trên `main`. Nhánh duy nhất còn lại: `main`.

## 7. Khi quay lại phiên sau

1. Đọc file này và `CLAUDE.md`.
2. Chọn việc ở mục 2, đánh dấu `[x]` khi xong rồi commit.
3. Nếu cần Claude: nói rõ việc ("wire signing config", "tạo asset store", "code AdMob + UMP + cờ Remote Config").
