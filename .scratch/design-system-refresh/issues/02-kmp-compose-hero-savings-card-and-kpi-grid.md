# 02: KMP Compose Hero Savings Card & Executive 2x2 KPI Grid

**What to build:** In Compose Multiplatform (`LoanSummary.kt`), render the executive Hero Savings Card in `#F0FDF4` with dynamic savings calculation (percentage of interest saved, time gained, status chip `Libre en Mes X/Y`) and the secondary 2x2 KPI grid showing strikethrough original amounts and saved insurance count.

**Blocked by:** 01: KMP Compose Mobile Shell & Floating Navigation Bar

**Status:** closed

- [x] Hero Card rendered with `#F0FDF4` background, percentage savings badge, net savings tabular-nums amount, time gained and payoff month.
- [x] 2x2 secondary KPI grid displays monthly payment, interest with strikethrough pacted value, total insurance with saved payments badge, and total cash outflow.
- [x] All metrics derive deterministically from the 40-digit loan simulation without mock values.
