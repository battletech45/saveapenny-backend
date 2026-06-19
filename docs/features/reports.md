# Reports

## Overview

Reports transform transaction history into financial summaries. All reports are date-range based and user-scoped. Four report types are available: monthly summary, category spending, cash flow, and net worth.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/reports/monthly-summary?from=&to=` | Income/expense grouped by month |
| GET | `/api/v1/reports/monthly-summary/export?from=&to=` | CSV download of monthly summary |
| GET | `/api/v1/reports/category-spending?from=&to=` | Spending totals by category |
| GET | `/api/v1/reports/cash-flow?from=&to=` | Daily income/expense |
| GET | `/api/v1/reports/net-worth?snapshotDate=` | Net worth on a given date |

## Monthly Summary

Returns income, expense, and net totals grouped by month within the date range. Suitable for budget reviews and trend analysis.

### CSV Export

The `export` endpoint returns the same data as a downloadable CSV file:

```bash
curl -O -J "http://localhost:8080/api/v1/reports/monthly-summary/export?from=2026-01-01&to=2026-12-31" \
  -H "Authorization: Bearer <accessToken>"
```

## Category Spending

Returns total spending grouped by category:

```json
[
  {
    "categoryId": "<uuid>",
    "categoryName": "Groceries",
    "total": 450.00,
    "transactionCount": 12
  }
]
```

Useful for identifying spending concentration and overspending categories.

## Cash Flow

Returns daily income and expense totals within the date range. Daily data points help identify:

- Spending patterns and low-cash periods
- Income timing (e.g., monthly salary vs. weekly expenses)
- Short-term liquidity needs

## Net Worth

Returns total assets minus total liabilities as of `snapshotDate`:

| Component | Calculation |
|-----------|-------------|
| **Assets** | Sum of all account balances with type `CASH`, `BANK`, `SAVINGS`, or `INVESTMENT` |
| **Liabilities** | Sum of all account balances with type `CREDIT` (credit balances represent debt) |
| **Net Worth** | Assets − Liabilities |

```json
{
  "snapshotDate": "2026-06-10",
  "totalAssets": 15000.00,
  "totalLiabilities": 2000.00,
  "netWorth": 13000.00
}
```

### Snapshot Persistence

- Results are persisted on first access per (user, date)
- A daily scheduled job pre-computes snapshots for all active users
- Historical queries return stable, previously-captured values
- Snapshots are deterministic — re-querying the same date returns the same result

## Best Practices

| Use Case | Recommended Report | Date Range |
|----------|-------------------|------------|
| Budget review | Monthly summary | Current month or quarter |
| Overspending identification | Category spending | 3-6 months |
| Liquidity planning | Cash flow | 30-90 days |
| Wealth tracking | Net worth | Monthly snapshots over 1+ years |
| Data export | Monthly summary CSV | Year-to-date |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `INVALID_REPORT_DATE_RANGE` | 400 | Date range parameters are invalid or `from` > `to` |
| `INVALID_NET_WORTH_SNAPSHOT_DATE` | 400 | Snapshot date is in the future |

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Net worth persisted as snapshots | Stable historical values; not recalculated on each query |
| Daily pre-computation of snapshots | Ensures all dates have values ready; reduces query-time computation |
| CSV export separate from JSON endpoint | Clean separation of concerns; export endpoint sets correct `Content-Type` |

## Referenced Files

| File | Purpose |
|------|---------|
| `src/main/java/com/saveapenny/report/controller/ReportController.java` | REST endpoints |
| `src/main/java/com/saveapenny/report/service/impl/ReportServiceImpl.java` | Report generation logic |
| `src/main/java/com/saveapenny/report/scheduler/NetWorthSnapshotScheduler.java` | Snapshot persistence and pre-computation |
