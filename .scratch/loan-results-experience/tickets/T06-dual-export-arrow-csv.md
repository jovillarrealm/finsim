# T06 — Dual Data Export Engine: Apache Arrow IPC (.arrow) & Formatted CSV/TSV

Status: closed
Depends On: T05
Blocks: T07

## Description
Implement the dual export module for the amortization schedule:
1. Pure Kotlin CSV/TSV exporter:
   - Exports the complete schedule with Spanish column headers.
   - Formatted with UTF-8 BOM (`\uFEFF`) and clean delimiter for instant opening in Microsoft Excel and Google Sheets without encoding/delimiter conflicts.
2. Apache Arrow IPC (`.arrow`) exporter:
   - Generates native columnar Apache Arrow IPC stream/file with schema:
     `month` (Int32), `date` (Utf8), and numeric columns as Float64 (or Decimal128).
   - In Web (Wasm), implements the bridge via JavaScript to construct and download the `.arrow` blob.
   - In Android, integrates through the platform export contract.
3. Expose both export buttons in `MonthlyTable.kt`:
   - "Exportar CSV / Excel"
   - "Exportar Apache Arrow (.arrow)"

## Acceptance Criteria
- Clicking "Exportar CSV" downloads a valid UTF-8 BOM delimited file that opens in Excel.
- Clicking "Exportar Apache Arrow" downloads a valid `.arrow` file consumable by Python/Pandas/Polars/DuckDB (`pyarrow.ipc.open_file` or `pyarrow.ipc.open_stream`).
