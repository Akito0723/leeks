# AGENTS.md

## Project Summary
- IntelliJ IDEA plugin that shows fund, stock, and crypto quotes inside the IDE.
- UI is split into three tabs (Fund/Stock/Coin) with refresh timers, sortable tables, and optional "hidden" mode (pinyin headers + neutral colors).
- Data sources:
  - Funds: TianTian Fund (HTTP JSONP)
  - Stocks: Tencent or Sina endpoints (configurable)
  - Crypto: Yahoo Finance (requires proxy)
- Scheduled refresh via Quartz with configurable cron expressions.

## Key Entry Points
- Tool windows/UI: `FundWindow`, `StockWindow`, `CoinWindow`
- Settings panel: `SettingsWindow`
- Refresh scheduling: `quartz/QuartzManager`, `quartz/HandlerJob`
- Data handlers:
  - Funds: `handler/TianTianFundHandler`
  - Stocks: `handler/TencentStockHandler`, `handler/SinaStockHandler`
  - Crypto: `handler/YahooCoinHandler`

## Data Flow (High Level)
1. Settings values stored via `PropertiesComponent` (fund/stock/coin codes, proxy, cron).
2. `*Window.apply()` reads settings, builds tables, and triggers `refresh()`.
3. `refresh()` schedules Quartz jobs and calls `HandlerJob` to fetch data.
4. Handlers request data via `HttpClientPool`, parse response, update table models.

## Code Conventions & Behaviors
- Table headers are configurable; their state is persisted in `PropertiesComponent`.
- “Hidden mode” uses `PinYinUtils` and neutral colors to reduce visual emphasis.
- Table models (`*RefreshHandler`) are `DefaultTableModel` subclasses and are not editable.
- Refresh intervals use cron expressions; multiple expressions separated by `;`.

## Build & Packaging
- JDK 8, Maven.
- `mvn package` outputs `target/leeks-<version>.zip`.
- Uses `src/assembly/plugin.xml` for plugin assembly.

## File Structure Pointers
- `src/main/java/*Window.java` — main UI and tool window integration.
- `src/main/java/handler` — fetch/parse/update per data source.
- `src/main/java/bean` — model objects for parsed data.
- `src/main/java/utils` — HTTP, pinyin conversion, popups, logging.
- `src/main/java/quartz` — scheduling helpers.
- `src/main/resources/META-INF/plugin.xml` — IntelliJ plugin metadata.

## Notes & Caveats
- Crypto quotes require proxy configuration (Yahoo endpoints).
- Network timeouts are short (2s), and handler errors are usually swallowed/logged.
- UI operations run on Swing EDT where needed (e.g., refresh time labels).
