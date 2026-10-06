# Google Play store listing (draft)

## Basics

| Field | Value |
|---|---|
| App name (max 30) | Spend Like a Millionaire |
| Short description (max 80) | Spend a billionaire's fortune on anything, from a candy bar to a space station. |
| Category | Games > Simulation (alternative: Apps > Entertainment) |
| Contact | GitHub issues page of this repo (or your own email) |
| Privacy policy URL | https://michaeo04.github.io/spend-like-a-millionaire/privacy-policy (enable GitHub Pages first, see release checklist) |
| Default language | English (United States); add Vietnamese (vi) translation |

## Full description (English, max 4000)

How big is a billionaire fortune, really? Pick a famous billionaire or celebrity, receive their entire net
worth, and try to spend it.

- Choose whose fortune to spend from a list of public billionaires and celebrities.
- 300+ things to buy, from a $1.80 candy bar to private jets, islands, football clubs and space stations.
- Sort by price, filter by category, search by name.
- Watch your remaining balance and the percentage of the fortune you have spent.
- Share a receipt of your shopping spree.
- Pick your language (English, Tiếng Việt) and currency.
- Works fully offline.

This app is a parody made for entertainment. It is not affiliated with, endorsed by, or sponsored
by any person, company, team or band mentioned. Net worths and prices are rough public estimates.

## Mô tả đầy đủ (Tiếng Việt)

Tài sản của một tỷ phú thật sự lớn đến mức nào? Hãy chọn một tỷ phú nổi tiếng, nhận toàn bộ tài
sản của họ và thử tiêu hết xem sao.

- Chọn tài sản của ai để tiêu trong danh sách các tỷ phú công khai.
- Hơn 300 món để mua, từ thanh kẹo 1,5 USD đến máy bay riêng, hòn đảo, câu lạc bộ bóng đá và trạm vũ trụ.
- Sắp xếp theo giá, lọc theo nhóm, tìm theo tên.
- Theo dõi số tiền còn lại và phần trăm tài sản đã tiêu.
- Chia sẻ hóa đơn cho cuộc mua sắm của bạn.
- Chọn ngôn ngữ (English, Tiếng Việt) và đơn vị tiền tệ.
- Hoạt động hoàn toàn ngoại tuyến.

Ứng dụng mang tính giải trí, châm biếm. Ứng dụng không liên kết, không được bảo trợ hay xác nhận
bởi bất kỳ cá nhân, công ty, đội bóng hay nhóm nhạc nào được nhắc đến. Tài sản và giá cả chỉ là
ước tính từ nguồn công khai.

## Graphic assets to prepare

| Asset | Spec |
|---|---|
| App icon | 512 x 512 PNG (the in-app icon is a gold coin; export a 512 version) |
| Feature graphic | 1024 x 500 PNG/JPG, no real people or logos |
| Phone screenshots | at least 2, 16:9 or 9:16, take from the emulator (Shop, Cart, Onboarding, dark mode) |

## Content rating (IARC questionnaire) hints

No violence, no sexual content, no gambling, no user-generated content, no in-app purchases, no
location, no ads in v1. Expect an "Everyone" rating. Re-answer the questionnaire when AdMob is added.

## Data safety form hints

- Data collected: crash logs and diagnostics, app interactions (Analytics), device or other IDs
  (app-instance ID) - all via Firebase, used for analytics and app functionality; not sold.
- Data is encrypted in transit; users cannot request deletion of anonymous analytics (state that).
- No data shared for advertising in v1. Update when AdMob is added (advertising ID, ad interactions).

## Policy risk reminders

- No person's name or brand in the app title, short description or tags. In-app photos of people and
  branded products are open-licensed (see release-checklist sections 3b and 3c for the risks); keep
  them out of store graphics.
- Keep the parody disclaimer in the description and in Settings.
- Named valuations (clubs, stadiums, groups) are shown with "≈ estimate".
