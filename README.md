# ANGADI

Angadi is a Kotlin and Jetpack Compose grocery shopping app. It browses a sample catalogue, keeps a persistent basket, takes demo orders with saved addresses, delivery slots and coupons, tracks order progress, and includes a Gemini-powered assistant served through a small private proxy.

> **Demo build.** The catalogue is DummyJSON sample data, payments are not processed, and delivery slots and order tracking are simulated. Everything is stored on the device. See [Known limitations](#known-limitations).

## Features

| Area | What it does |
| --- | --- |
| Browse | Home storefront, categories, offers strip, deals, top-rated shelf, recently viewed products, offline seed catalogue plus a cached copy of the last successful catalogue refresh. |
| Search & discovery | Search with suggestions, recent searches (clearable), no-result state, category / price range / rating / in-stock filters and sorting by relevance, price (both ways) and rating. Filters and sorting combine. |
| Product details | Image, description, price, rating, quantity picker, add to basket, save, and on-device reviews. |
| Reviews | 1–5 star rating + text, rating distribution and average, edit and delete. Reviews are local and **not** verified purchases. |
| Basket | Quantity steppers (limited by stock and a per-item cap), remove, delivery fee, free-delivery progress, totals. |
| Addresses | Add / edit / delete, Home / Work / Other labels, default address, full validation (name, 10-digit mobile, PIN, …). |
| Delivery slots | Four days of time windows with availability. Availability is **simulated**, deterministic and labelled as such. |
| Coupons & pricing | Percent and fixed coupons with minimum basket value, maximum discount, expiry and one-time use. Delivery fee and free-delivery threshold. Totals are recomputed from stored data when an order is placed. |
| Orders | Persistent history, order details with item snapshots (name, image, quantity, price paid), address, payment method, slot, discounts and a six-step status timeline. Cancel before packing. |
| Reorder | Adds a past order's items to the basket at **current** prices, respecting stock and the per-item cap, with a summary of anything unavailable. |
| Profile | Editable name, email, phone and photo (local only), shortcuts to orders and addresses, theme (System / Light / Dark), viewing-history size. |
| Angadi AI | Offline app guide, or live Gemini answers through the proxy in `backend/`. |

## Screens and navigation

Bottom tabs: **Home**, **Explore**, **Saved**, **Basket**, **You**. Other destinations are pushed on top with Navigation Compose:

```text
Home ─ Explore ─ Saved ─ Basket ─▶ Checkout ─▶ Order details
                                      │             ▲
You ─▶ Edit profile                   ▼             │
    ├▶ Your orders ───────────────────────────────────┘
    ├▶ Saved addresses ─▶ Address editor
    └▶ API & AI settings
Assistant (from the ✦ button on any tab)
```

The product sheet (with reviews) opens from any product card as a modal bottom sheet.

## Architecture

MVVM with a unidirectional data flow and a small clean-architecture split:

```text
app/src/main/java/com/angadi/app/
  AngadiApplication.kt   Application + AppContainer (manual dependency injection)
  MainActivity.kt
  domain/
    model/               Product, Order, Address, Coupon, Review, UserProfile, … (plain Kotlin)
    logic/               PricingEngine, OrderPlanner, ProductSearch, DeliverySlotPlanner,
                         AddressValidator, ReorderPlanner (pure, unit-tested)
  data/
    local/               Room database, entities, DAOs, migration 1→2
    remote/              DummyJSON catalogue and Gemini proxy HTTP clients
    repository/          Shop, Order, Address, Review, Profile, Settings, Assistant repositories
  presentation/
    viewmodel/           One ViewModel per feature, StateFlow state, ViewModel factory
    navigation/          Routes, NavHost, bottom bar, app scaffold
    components/          Reusable composables (cards, headers, chips, …)
    screens/             home, explore, saved, basket, checkout, orders, addresses,
                         profile, assistant, product
  ui/theme/              Angadi colours, typography (Google Sans Flex), Material 3 theme
backend/                 Gemini proxy (Node.js)
```

* **State:** ViewModels expose `StateFlow`; screens collect with `collectAsStateWithLifecycle`. Room DAOs return `Flow`, so the UI updates when data changes.
* **Trusted calculations:** `PricingEngine` and `OrderPlanner` are pure Kotlin. `OrderRepository.placeOrder` reads the basket from the database, looks prices up in the catalogue, re-validates stock, address, slot and coupon, recomputes every figure and saves the order in a single transaction. UI-computed totals are previews only. This is the strongest guarantee available without a server; a real deployment must repeat these checks on a backend.
* **Dependency injection:** `AppContainer` provides lazily created singletons and `AppViewModelFactory` builds the ViewModels. Hilt was deliberately not added: it needs an annotation-processing step, brings little for this size of app, and AGP 9's built-in Kotlin support makes the extra Gradle plugin a compatibility risk.
* **Threading:** Kotlin coroutines everywhere; Room and the network clients run off the main thread.

## Technology stack

Kotlin 2.2, Jetpack Compose (Material 3), Navigation Compose, Lifecycle ViewModel, Coroutines / Flow, Room 2.8 (KSP), Android Gradle Plugin 9.3, minSdk 26 / targetSdk 37, Node.js 20+ for the proxy.

## Database schema

Room database `angadi.db`, version 2 (schema exported to `app/schemas/`).

| Table | Purpose / key columns |
| --- | --- |
| `cart_items` | `product_id` (PK), `quantity` |
| `favorites` | `product_id` (PK) |
| `cached_products` | Last successful catalogue refresh (offline basket support) |
| `orders` | `id`, `created_at`, `status`, `status_updated_at`, `subtotal`, `delivery_fee`, `discount`, `total`, `coupon_code`, `item_count`, `payment_method`, recipient / phone / address snapshot, `slot_label`, `slot_start` |
| `order_items` | `order_id` → `orders` (cascade), `product_id`, `title`, `unit`, `unit_price`, `quantity`, `thumbnail`, `emoji`, `category` – a snapshot, so catalogue changes never alter old orders |
| `addresses` | Label, recipient, phone, house, street, area, city, state, PIN, `is_default` |
| `coupon_usage` | One-time coupon redemptions (released if the order is cancelled) |
| `reviews` | One review per product on this device (unique `product_id`) |
| `recent_views` | `product_id` (PK), `viewed_at` |
| `search_history` | `query` (PK), `searched_at` |
| `profile` | Single row: name, email, phone, photo path |

Theme, assistant server URL and viewing-history size remain in `SharedPreferences` (`angadi_preferences`, unchanged keys).

### Migration from version 1

Version 1 was a hand-written `SQLiteOpenHelper` schema (`cart`, `favorites`, `orders(total, item_count, created_at)`). `MIGRATION_1_2` copies every row into the new tables before dropping the old ones: baskets and saved items are kept unchanged and past orders keep their ID, total, item count and date (shown as delivered; they have no line items because v1 never stored them). `MigrationTest` builds a genuine v1 database and verifies this.

## Backend (Gemini proxy)

The Android app never contains a Gemini key. `backend/` is a dependency-free Node.js server that holds the key and forwards chat requests.

```bash
cd backend
cp .env.example .env     # then put your key from https://aistudio.google.com/app/apikey on the GEMINI_API_KEY line
npm start                # http://localhost:3000
npm test                 # unit tests (node:test)
```

| Variable | Default | Meaning |
| --- | --- | --- |
| `GEMINI_API_KEY` | – | Your private key. Never commit `.env`. |
| `GEMINI_MODEL` | `gemini-3.8-flash` | Model available to your account. |
| `PORT` / `HOST` | `3000` / `0.0.0.0` | Listen address. |
| `ALLOWED_ORIGIN` | unset | CORS is **off** by default (the Android app doesn't need it). Set to a single origin for a web client. |
| `RATE_LIMIT_PER_MINUTE` | `20` | Per-client limit on `/api/assistant`. |
| `TRUST_PROXY` | `false` | Honour `X-Forwarded-For` only behind a proxy you control. |

Endpoints: `GET /health` (reports whether a key is configured, never the key) and `POST /api/assistant`.

Hardening in place: strict request validation (JSON content type, size cap, role whitelist, message count/length limits, last message must be from the shopper), bounded-memory rate limiter, upstream error text is logged server-side but never returned to clients, no request bodies or secrets in logs, security headers, request / header timeouts and graceful shutdown. There is **no authentication**: anyone who can reach the URL can spend your Gemini quota. Put it behind HTTPS and an authenticating gateway (or add per-user auth) before exposing it publicly.

In the app, open **You → API & AI**, enter the server URL (`http://10.0.2.2:3000` from an Android emulator), save it and tap **Test connection**.

## Build, run and test

Requirements: Android Studio (or JDK 17 + Android SDK 37) and, for the proxy, Node.js 20+.

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew test               # JVM unit tests
./gradlew lint
./gradlew connectedDebugAndroidTest   # database, migration and UI tests; needs an emulator/device
```

Build outputs and APKs are not committed; build the debug APK locally and install it from Android Studio or `adb install`.

Tests:

* **Unit (JVM):** pricing, coupons, cart rules, order planning, search / filter / sort, reorder, address validation, delivery slots, order status rules, rating summary.
* **Instrumented – database:** order and order-item persistence, price snapshots, coupon one-time use and release, status rules, reorder, addresses (default handling), reviews, recent views, search history, profile persistence, catalogue cache, and the v1→v2 migration preserving cart, favourites and orders.
* **Instrumented – UI:** tab navigation, search and filters, product details, reviews, basket editing, checkout → order → history → details, cancel and reorder, address management, profile editing and the personalised greeting.

## Security considerations

* No secrets are in the app or repository; `.env`, keystores, `local.properties`, APKs and build folders are git-ignored.
* Pricing and order validation are centralised in testable pure functions and re-run at placement time (see Architecture).
* Profile photos are copied into app-private storage; profile, address and order data never leave the device.
* The proxy is documented above, including its lack of authentication.
* `android:allowBackup` is still enabled (inherited from the project template); review `backup_rules.xml` / `data_extraction_rules.xml` before shipping if address data should not be backed up.

## Known limitations

* **Catalogue:** DummyJSON placeholder data with illustrative demo prices (USD × 85). Popularity and "newest" sorting are not offered because the source has no sales or creation-date data; "Top rated" uses the catalogue rating.
* **Payments:** none. Checkout offers pay-on-delivery options and records a demo order only.
* **Delivery slots and tracking:** simulated. There is no logistics integration; order status only moves when you press the clearly labelled demo button on the order screen.
* **Accounts:** none. Profile, reviews and orders are local to the device, so reviews cannot be tied to verified purchases and there is no logout.
* **Coupons:** bundled with the app and enforced on-device; a malicious user with a modified build could bypass them. A real store must validate coupons and prices server-side.
* **Reorder** uses the catalogue loaded in the app; items missing from it (for example after an offline start) are reported as unavailable.

## Licences

Google Sans Flex is bundled under the SIL Open Font License; see `licenses/`.
