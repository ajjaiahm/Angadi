# ANGADI

Angadi is a Kotlin and Jetpack Compose grocery shopping app starter built from the original Android Studio project. It keeps the repository's Gradle wrapper, version catalog, plugins, SDK settings, and application id, and adds a modular shopping experience on top.

## What is included

- Home storefront, category browsing, product search, and price sorting.
- Grocery catalog refresh from DummyJSON with an offline curated catalog when the network is unavailable.
- Product details, favorites, and a quantity-aware basket.
- SQLite persistence for basket contents, saved products, and demo order history.
- Address collection and demo checkout; it does not charge a payment method or contact a delivery service.
- Angadi AI chat with an offline app-use guide and a server-side Gemini proxy for general answers when configured.
- Product images from the sample catalog, local emoji artwork for offline products, loading fallbacks, and smooth screen transitions.
- Google Sans Flex throughout the Compose interface, with a Light / Dark / System appearance setting.
- A custom Angadi adaptive launcher icon, including themed-icon artwork.

## Open and run in Android Studio

1. Open this folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on an Android device or emulator. The project uses the Gradle and Android versions already configured in this repository.
3. Internet access is used for the sample catalog and product images. Without it, the app still opens with the local catalog and the basket and saved items remain available.

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. You do not copy an APK into the source folders: download the supplied debug APK to an Android phone's **Downloads** folder and tap it to install, or build again from Android Studio.

The debug build is also available in the repository at [`artifacts/ANGADI-debug.apk`](https://github.com/ajjaiahm/Angadi/raw/refs/heads/main/artifacts/ANGADI-debug.apk). Download it to an Android phone's **Downloads** folder and tap it to install.

To change the look, open **You → Appearance** and choose **System**, **Light**, or **Dark**. The app icon artwork lives in `app/src/main/res/drawable/ic_launcher_foreground.xml` and its adaptive launcher entries are in `app/src/main/res/mipmap-anydpi-v26/`.

The Google Sans Flex font file is at `app/src/main/res/font/google_sans_flex.ttf`; its font family and weight mappings are in `app/src/main/java/com/angadi/app/ui/theme/Type.kt`. The font's OFL license and trademark notice are bundled in the project root's `licenses/` folder.

## Gemini assistant setup

The Android app never contains a Gemini API key. Create or copy an actual key from [Google AI Studio's API Keys page](https://aistudio.google.com/app/apikey)—the app needs the key value, not the webpage URL. Run the small proxy in `backend/` on a trusted machine or deploy it over HTTPS:

1. Install Node.js 20 or newer.
2. In the `backend` folder, copy `.env.example` to `.env` and replace `put-your-private-key-here` on the `GEMINI_API_KEY=` line with the key value. Keep `.env` private and out of Git.
3. Start the proxy with `npm start`.
4. In the app, open **You → API & AI**, enter the server URL, save it, and tap **Test connection**. For an Android emulator on the same computer, use `http://10.0.2.2:3000`; for a device or hosted server, use a reachable HTTPS URL.

The proxy calls Google's Gemini Interactions API from the server and keeps the key out of the APK. Angadi AI is configured as a general chatbot for everyday questions and how-to help, and receives a small summary of the current sample catalogue and basket for relevant shopping answers. Set `GEMINI_MODEL` to another model available to your account if needed. A real deployment should add authentication, HTTPS, abuse monitoring, and per-user rate limits.

## Data and production integration

`data/remote` contains the sample catalog and assistant HTTP clients; `data/local` owns the SQLite database; `data/repository` connects data sources; `domain/model` holds product models; and `presentation` contains app state and Compose screens. The catalog currently uses DummyJSON's placeholder grocery data. Its displayed prices are illustrative demo values, not merchant prices or a live exchange-rate conversion. Replace that feed with your merchant's product, price, inventory, and fulfillment API before launch.

The basket, favorites, and order history use a local SQLite database on this device. For a multi-device store, connect a trusted backend and database. Checkout is a demo flow only; connect a payment provider and delivery integration on the server before taking real orders. No passwords, API keys, or customer data are included in this project.

## Project layout

```text
app/src/main/java/com/angadi/app/
  data/local/        SQLite persistence
  data/remote/       Catalog and Gemini proxy clients
  data/repository/   Data access boundary
  domain/model/      Product and offline catalog models
  presentation/      App state and Compose screens
  ui/theme/          Angadi colors and Material theme
backend/             Private-key Gemini proxy
gradle/              Original version catalog and wrapper configuration
```
