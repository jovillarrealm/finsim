# ADR 0001: Canonical KMP `:shared` Financial Engine Seam

## Status
Accepted

## Date
2026-10-12

## Context
During the evolution of Finsim from a single-target browser prototype into a multiplatform system supporting Android and Web (via Compose Multiplatform WasmJs and JS), a dual-stack implementation occurred:
1. The TypeScript calculation module in `src/engine/` (`loan.ts`, `rates.ts`, `amortizationAccounting.ts`, and `src/domain/scenario.ts`).
2. The Kotlin Multiplatform module in `kmp/shared/src/commonMain/kotlin/com/finsim/` (`Loan.kt`, `Rates.kt`, `AmortizationAccounting.kt`, `Scenario.kt`, `EventDraftBatch.kt`, `ingestion/`, `workspace/`).

Divergence has already materialized across these two modules:
- The Kotlin Multiplatform `:shared` module has incorporated essential domain capabilities specified in `docs/financial-conventions.md`:
  - Argentine prepayment regulatory rules (`argentinaProfile`, `ArgentinaPrepaymentAdapter`) enforcing legal thresholds and commission/VAT penalties.
  - Hybrid on-device statement ingestion (Tier 1 regex extraction and Tier 2 VLM structured vision).
  - Multiplatform structured logging via Touchlab Kermit.
  - Bidirectional parity verification and financial invariant test suites across currencies and edge cases.
  - Batch event drafting (`EventDraftBatch`).
- The legacy TypeScript module in `src/` lacks these capabilities.
- Maintaining two parallel calculation engines duplicates maintenance cost for every change in `docs/financial-conventions.md`. When mathematical formulas, rounding strategies, or regulatory profiles evolve, having dual modules harms locality and risks silent drift between targets.

## Decision
We formally establish Kotlin Multiplatform `:shared` (`kmp/shared/src/commonMain/kotlin/com/finsim/`) as the single canonical source of truth and the sole canonical module for financial calculation across all targets:
- **Android Target**: Consumed as compiled JVM bytecode / DEX via Compose Multiplatform.
- **Web Target**: Consumed as compiled WebAssembly (`wasmJs`) and JavaScript (`js`) via Compose Multiplatform.
- **Legacy TypeScript Engine**: Formally marked as `@deprecated` and frozen. `src/engine/` and `src/domain/` are retained strictly for backward baseline regression tests and web prototype comparisons. No new features, bug fixes, or regulatory extensions will be added to the TypeScript module.

The canonical seam for financial simulation is the interface exposed by `:shared`:
- Amortization calculation: `com.finsim.engine.LoanKt.simulateLoan(Scenario): LoanResult`
- Rate conversions: `com.finsim.engine.RatesKt.toMonthlyRate(Rate): Decimal` and `convertRate(Rate, TargetRateKind, Frequency): String`
- In-process amortization ledger: `com.finsim.engine.AmortizationAccount` with pluggable adapters (`StandardPrepaymentAdapter`, `ArgentinaPrepaymentAdapter`)
- Statement reconciliation: `com.finsim.reconciliation.reconcileStatement(Scenario, List<StatementRow>)`
- Scenario persistence: `com.finsim.persistence.ScenarioPersistence`
- Document ingestion pipeline: `com.finsim.ingestion.HybridDocumentOrchestrator`
- Workspace state orchestration: `com.finsim.workspace.WorkspaceStore`

Any client callers across any target (mobile or web) interact with this canonical seam rather than developing target-specific calculation algorithms.

## Consequences

### Depth
The `:shared` module is a deep module: behind a concise, minimal interface (`simulateLoan`, `toMonthlyRate`, `convertRate`, `AmortizationAccount`), it encapsulates extensive mathematical and regulatory complexity:
- 40-digit arbitrary-precision arithmetic without floating-point cancellation.
- Strict cascade payment imputation: insurance fees first, accrued interest second, overdue capital third, and regular scheduled capital fourth.
- Argentine prepayment fee calculation and statutory quarter-term threshold verification.
- In-process delinquency tracking without duplicate ledger states.
Callers provide input scenarios and receive verified schedules without having to know or manage internal calculation mechanics.

### Leverage
Callers on all platforms receive substantial leverage from this depth:
- A single, deeply tested Kotlin implementation powers Android DEX and Web WasmJs/JS.
- Complex domain logic (regulatory rules, ingestion pipelines, ledger imputation) is authored once and leveraged across all target platforms.
- Eliminates the need to reproduce complex multi-currency financial logic across separate languages.

### Locality
Maintainers gain strict locality:
- All domain rules, rounding conventions, insurance stopping rules, and regulatory adapters reside in a single location (`kmp/shared/src/commonMain/kotlin/com/finsim/`).
- Whenever financial conventions or regulatory requirements are updated, the change is implemented in exactly one place. Fixes and improvements propagate uniformly across all targets, eliminating divergent behavior.

### Test Surface
- The canonical interface of `:shared` constitutes the primary test surface.
- The multiplatform test suite in `kmp/shared/src/commonTest/` (`:shared:jvmTest`) validates all financial invariants (principal conservation, zero balance at payoff, rate conversion tolerances < 1e-12, parity corpuses).
- Legacy Vitest test suites (`src/**/*.test.ts`) are preserved in CI to guarantee that the legacy web prototype does not suffer unexpected regressions, but new test suites and parity assertions are authored in `:shared`.
