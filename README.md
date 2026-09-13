# TGW Stock

Android restaurant stock control & purchasing app. Kotlin + Jetpack Compose + Room,
Material 3, fully offline.

## Building the app

This app was written in an environment with no Android SDK and no access to Google's
Maven repository (`dl.google.com` is blocked by network policy), so it has **not been
compiled or turned into an APK here** - the source has been reviewed carefully by hand
and by a second automated pass, but has not gone through a real Kotlin/Android compiler.
To build it:

1. Open the project root in Android Studio (Koala/2024.1 or newer recommended).
2. Let Gradle sync - it will download the Android SDK platform (34), build tools, and
   all AndroidX/Compose/Room/CameraX/ML Kit dependencies from Google's and Maven
   Central's repositories.
3. Run on a device/emulator with API 24+ (Android 7.0), or `Build > Build Bundle(s) /
   APK(s) > Build APK(s)` for a release/debug APK.
4. If Gradle reports version-resolution errors, they are most likely genuine dependency
   version mismatches (the versions pinned in `app/build.gradle.kts` were current as of
   authoring but Google occasionally pulls old artifacts) - bump the offending artifact
   to its next patch version.

The Gradle wrapper (`gradlew` / `gradlew.bat`) is committed, pointing at Gradle 8.9.

## Architecture

- **`data/local`** - Room database (`AppDatabase`), one `@Entity` + `@Dao` pair per
  table (`products`, `categories`, `suppliers`, `supplier_products`, `supplier_prices`,
  `recipes`, `recipe_items`, `stock_batches`, `stock_movements`, `purchase_orders`,
  `purchase_order_items`, `stock_counts`, `stock_count_items`, `waste_records`). Money
  is stored as integer minor units (cents) on every entity, never as `Float`/`Double`,
  to avoid rounding errors.
- **`data/repository`** - one interface + one `Room*` implementation per domain area
  (products, suppliers, prices, recipes, stock movements, purchasing, stock counts,
  waste, reports, dashboard, maintenance). **ViewModels and UI only depend on these
  interfaces**, never on Room types directly - swapping the backing store for Supabase
  later means writing `Supabase*` implementations of the same interfaces and changing
  what `di/AppContainer.kt` constructs; nothing above that layer needs to change.
- **`domain`** - pure Kotlin, no Android/Room dependencies: `Money` (cent-based
  arithmetic), `StockUnit`/`UnitConverter` (kg↔g, L↔ml conversions), `StockCalculations`
  (stock value, weighted-average cost, recipe cost/food-cost %, portions producible,
  cheapest-supplier price-per-unit, purchase quantity/cost, price-change %, waste cost).
- **`di/AppContainer.kt`** - hand-rolled dependency graph (no Hilt/Dagger) exposed from
  `TgwStockApp`. Screens obtain a ViewModel via the `tgwViewModel { it.someRepository }`
  helper in `ui/common/ViewModelUtils.kt`.
- **`data/seed/SampleDataSeeder`** - populates categories/products/suppliers/prices/
  recipes/opening stock/one waste record/one draft PO on first launch, entirely through
  the same repositories the UI uses (so it's a realistic exercise of the write path).
  Every row it creates has `isSample = true`; **Settings → Clear Sample Data** deletes
  everything flagged that way in one transaction, child tables first.
- **`ui/`** - one package per feature (`dashboard`, `stock`, `movements`, `recipes`,
  `suppliers`, `purchase`, `waste`, `reports`, `settings`, `barcode`, `nav`, `common`,
  `theme`). Each screen has a `ViewModel` holding `StateFlow`s and a stateless
  `@Composable` screen function. Simple single-table lists are observed reactively via
  Room `Flow`s; screens that need data joined across tables (recipe cost, purchase
  list, reports, product detail) call a repository "compute" `suspend fun` and store
  the result in a `MutableStateFlow`, refreshed on screen entry and after mutations.

## Feature coverage

Dashboard counts/alerts/quick actions, full stock CRUD with search/filter and a
single-page product detail (used-in recipes, FEFO batches, movements, price history),
manual Stock In/Out with reason codes, Stock Count with system-vs-counted difference
and one-tap adjustment commit, recipes with live cost/food-cost/portions-available,
suppliers with multi-supplier price history per product and an automatic
cheapest-supplier call-out, an auto-generated purchase list grouped by supplier with
one-tap PO creation, full Purchase Order lifecycle (Draft → Ordered → Partially
Delivered/Delivered → stock received into real batches) or cancellation, a dedicated
Waste screen with weekly/monthly cost totals, a Reports screen with 8 metrics and
Today/7-day/30-day/custom filters, camera barcode scanning (CameraX + ML Kit) that
opens a matched product or offers to link/create one, single-file SQLite backup and
restore (Storage Access Framework), CSV export of every major table, and a manual
light/dark/system theme override on top of automatic dark mode support.

## What to check first when you open it in Android Studio

- `app/build.gradle.kts` for the dependency versions.
- `data/local/AppDatabase.kt` for the full table list.
- `ui/nav/NavGraph.kt` for how every screen is wired together.
