# Stocks

## Overview

The stock module exposes authenticated, read-only endpoints backed by the Alpha Vantage API.

Supported data includes:

- quote snapshots
- daily OHLCV series
- news and sentiment
- company overview
- income statement, balance sheet, and cash flow
- technical indicators: `SMA`, `EMA`, `RSI`

## Provider

| Setting | Value |
|---------|-------|
| Provider | Alpha Vantage |
| Auth | Query parameter `apikey` |
| Base URL | `https://www.alphavantage.co` |
| Free tier limit | `5` requests/minute, `25` requests/day |

The application enforces the same minute/day quota in-memory before making provider calls.

## Enablement

Stock endpoints require both:

- `STOCK_ENABLED=true`
- `ALPHA_VANTAGE_API_KEY` configured

If the API key is blank, requests fail with `STOCK_DISABLED` even when the feature flag is enabled.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/stocks/quote?symbol=` | Current quote snapshot |
| GET | `/api/v1/stocks/daily?symbol=&outputSize=` | Daily price series |
| GET | `/api/v1/stocks/news?symbol=` | News and sentiment |
| GET | `/api/v1/stocks/overview?symbol=` | Company overview |
| GET | `/api/v1/stocks/income-statement?symbol=` | Income statements |
| GET | `/api/v1/stocks/balance-sheet?symbol=` | Balance sheets |
| GET | `/api/v1/stocks/cash-flow?symbol=` | Cash flow statements |
| GET | `/api/v1/stocks/sma?symbol=&timePeriod=&interval=&seriesType=` | Simple Moving Average |
| GET | `/api/v1/stocks/ema?symbol=&timePeriod=&interval=&seriesType=` | Exponential Moving Average |
| GET | `/api/v1/stocks/rsi?symbol=&timePeriod=&interval=&seriesType=` | Relative Strength Index |

## Validation Rules

| Parameter | Rule |
|-----------|------|
| `symbol` | Required, uppercased, max 10 chars, allowed `A-Z`, `0-9`, `.`, `-` |
| `outputSize` | `compact` or `full` |
| `interval` | `daily`, `weekly`, or `monthly` |
| `timePeriod` | Positive integer only |
| `seriesType` | `close`, `open`, `high`, or `low` |

## Error Semantics

| Code | HTTP | Meaning |
|------|------|---------|
| `INVALID_STOCK_SYMBOL` | 400 | Invalid symbol or stock query parameter |
| `STOCK_QUOTE_NOT_AVAILABLE` | 404 | Provider returned no usable data |
| `STOCK_RATE_LIMIT_EXCEEDED` | 429 | App-side stock quota exceeded |
| `STOCK_DISABLED` | 503 | Feature disabled or API key missing |
| `STOCK_PROVIDER_ERROR` | 502 | Provider returned an error/note or request failed |

## Notes

- All stock endpoints require `Authorization: Bearer <accessToken>`.
- Transport DTOs preserve provider response structure; API DTOs map to typed fields where practical.
- Technical indicator endpoints default `interval=daily` and `seriesType=close`.
- Daily series defaults `outputSize=compact`.

## Stock Holdings

Holdings allow users to track their stock purchases and view real-time profit/loss statistics. Each holding records a purchase (symbol, quantity, purchase price, date) and the system calculates current value, profit/loss, and P/L percentage against live market quotes.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/stocks/holdings` | Create a stock holding |
| GET | `/api/v1/stocks/holdings` | List holdings (paginated, with live P/L) |
| GET | `/api/v1/stocks/holdings/summary` | Portfolio summary (aggregate P/L) |
| GET | `/api/v1/stocks/holdings/{holdingId}` | Get single holding |
| PUT | `/api/v1/stocks/holdings/{holdingId}` | Update a holding |
| DELETE | `/api/v1/stocks/holdings/{holdingId}` | Delete a holding |

### Create Holding Request

```json
{
  "symbol": "IBM",
  "quantity": "7.14285714",
  "purchasePrice": "140.00",
  "currency": "USD",
  "purchaseDate": "2025-04-25",
  "notes": "First IBM position"
}
```

### Holding Response

The response includes both static holding data and live market-calculated fields:

| Field | Source | Description |
|-------|--------|-------------|
| `investedAmount` | Computed | `quantity × purchasePrice` |
| `currentPrice` | Alpha Vantage | Latest market price |
| `currentValue` | Computed | `quantity × currentPrice` |
| `profitLoss` | Computed | `currentValue − investedAmount` |
| `profitLossPercent` | Computed | `(profitLoss / investedAmount) × 100` |
| `latestTradingDay` | Alpha Vantage | Most recent trading date |

When market data is unavailable (Alpha Vantage rate-limited or return error), P/L fields are `null` and the holding data is still returned.

### Error Codes

| Code | HTTP | Meaning |
|------|------|---------|
| `STOCK_HOLDING_NOT_FOUND` | 404 | Holding not found or not owned by caller |
| `DUPLICATE_STOCK_HOLDING` | 409 | Same symbol + purchase date already exists |

### Notes

- Symbol changes are not supported on update; delete and recreate to fix symbol errors.
- Holdings use hard delete (no soft delete); delete when a position is sold.
- The summary endpoint may exhaust the Alpha Vantage rate limit (25/day) if many holdings exist. In-memory per-symbol deduplication within a single request mitigates this.

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/stock/controller/StockController.java` | Stock market data REST endpoints |
| `src/main/java/com/saveapenny/stock/service/impl/StockServiceImpl.java` | Validation, mapping, and provider classification |
| `src/main/java/com/saveapenny/stock/infrastructure/AlphaVantageClient.java` | Alpha Vantage transport client |
| `src/main/java/com/saveapenny/stock/infrastructure/RateLimitTracker.java` | App-side stock quota tracking |
| `src/main/java/com/saveapenny/stockholding/controller/StockHoldingController.java` | Holdings REST endpoints |
| `src/main/java/com/saveapenny/stockholding/service/impl/StockHoldingServiceImpl.java` | Holdings business logic and P/L calculation |
