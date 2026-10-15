# Architecture Direction 1: Deep Amortization Accounting Module

This document details the architectural deepening of the amortization accounting engine within Finsim. It establishes the interface, implementation, seams, adapters, and domain guarantees according to the codebase design principles.

---

## 1. Context and Problem Statement

In the original codebase, loan amortization was implemented inside `simulateLoan` as a 250-line procedural loop with mutable local state (`balance`, `referenceBalance`, `interest`, `insurance`, `overdue`) and inline closures (`allocate`, `debt`).

This created structural friction:
- **Shallow design**: High cognitive load was forced upon any caller wanting to simulate or inspect loan behavior. Timeline orchestration (calendar dates, event array filtering, event recurrence expansion) was tightly coupled with granular financial accounting (waterfall priority allocation, overdue capital accrual, rounding half-up, and regional prepayment fees).
- **Test surface leakage**: To test a subtle payment imputation case (e.g. allocating $7 against unpaid insurance and interest), tests were forced to construct an entire `Scenario` object with string dates, UUIDs, arrays of events, and run up to 1,200 iterations. The accounting logic lacked an independent test surface.
- **Scattered domain rules**: Rules from `docs/financial-conventions.md` (priority waterfall: insurance $\rightarrow$ interest $\rightarrow$ capital, arrears vs debt distinction, maturity residual absorption, and regional prepayment rules such as Argentina profile) were entangled directly inside procedural loop increments.

---

## 2. Module Classification & Seam Placement

### Dependency Category: In-Process
The amortization accounting module belongs to the **in-process** dependency category:
- Pure deterministic computation with in-memory state transitions.
- Zero I/O, zero network calls, zero platform dependencies.
- Tested directly across its own interface without test doubles for computation.

### Seam Definition
The primary **seam** is placed between **Timeline Orchestration** (`simulateLoan`, calendar schedules, batch runners) and **Amortization Accounting** (`AmortizationAccount`).
- Callers across this seam supply only the monthly inputs (scheduled status, offered catchup amounts, offered extra amounts, and active insurance rules).
- The module hides all mutable state registers, calculation orders, priority waterfalls, overdue arithmetic, and invariant validations behind this seam.

An internal seam is placed at **Regional Prepayment Profiles**:
- The module defines the `PrepaymentRule` interface.
- Two concrete adapters satisfy this interface: `StandardPrepaymentAdapter` and `ArgentinaPrepaymentAdapter`.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Timeline Orchestrator                           │
│       (simulateLoan / Batch Runners / Sensitivity Engines)             │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
                     SEAM: AmortizationTerms & processMonth()
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                    MODULE: AmortizationAccount                         │
│                                                                        │
│  - Monthly Charge Calculation (Interest & Insurance)                   │
│  - Scheduled Capital Due & Overdue Capital Accrual                     │
│  - Maturity Residual Absorption via Reference Balance                 │
│  - Imputation Waterfall (Insurance -> Interest -> Capital)             │
│  - Invariant Assertions & State Conservation                           │
│                                                                        │
│                      INTERNAL SEAM: PrepaymentRule                     │
│                       ┌─────────────────────────┐                      │
│                       │  PrepaymentRule (Port)  │                      │
│                       └───────────┬─────────────┘                      │
│                                   │                                    │
│             ┌─────────────────────┴─────────────────────┐              │
│             │                                           │              │
│   ┌─────────▼─────────────────┐       ┌─────────────────▼───────────┐  │
│   │ StandardPrepaymentAdapter │       │ ArgentinaPrepaymentAdapter  │  │
│   │ (0% fee, 0% VAT)          │       │ (25% term threshold, fee/VAT│  │
│   └───────────────────────────┘       └─────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Interface Specification

The module interface is deliberately minimal, exposing only contract terms, step transition inputs, and immutable settlement snapshots.

### 3.1 Types

#### Terms Configuration
```kotlin
data class AmortizationTerms(
    val principal: Decimal,
    val monthlyRate: Decimal,
    val termMonths: Int,
    val basePayment: Decimal? = null,
    val isPaymentExplicit: Boolean = false,
    val prepaymentRule: PrepaymentRule = StandardPrepaymentAdapter()
)
```

#### Step Input
```kotlin
data class MonthPaymentInput(
    val month: Int,
    val isMissed: Boolean = false,
    val catchupOffer: Decimal = Decimal.ZERO,
    val extraOffer: Decimal = Decimal.ZERO,
    val activeInsurance: List<Insurance> = emptyList()
)
```

#### Step Settlement Snapshot
```kotlin
data class MonthSettlement(
    val month: Int,
    val openingBalance: String,
    val interestCharged: String,
    val insuranceCharged: String,
    val scheduled: Allocation,
    val catchup: Allocation,
    val extra: Allocation,
    val closingBalance: String,
    val overduePrincipal: String,
    val pendingInterest: String,
    val pendingInsurance: String,
    val arrears: String,
    val debt: String,
    val cashPaid: String,
    val commissionPaid: String,
    val vatPaid: String,
    val isPaidOff: Boolean
)
```

#### Allocation Imputation
```kotlin
data class Allocation(
    val offered: String,
    val applied: String,
    val insurance: String,
    val interest: String,
    val principal: String,
    val unapplied: String
)
```

#### Regional Prepayment Seam
```kotlin
data class PrepaymentFee(
    val commission: Decimal,
    val vat: Decimal
)

interface PrepaymentRule {
    fun evaluatePrepayment(month: Int, prepaidCapital: Decimal): PrepaymentFee
}
```

### 3.2 Methods & Operations

```kotlin
class AmortizationAccount {
    companion object {
        fun open(terms: AmortizationTerms): AmortizationAccount
        fun calculateAnnuityBase(principal: Decimal, rate: Decimal, termMonths: Int): Decimal
    }

    val basePayment: Decimal
    val currentBalance: Decimal
    val pendingInterest: Decimal
    val pendingInsurance: Decimal
    val overduePrincipal: Decimal

    fun totalDebt(): Decimal
    fun arrears(): Decimal
    fun isPaidOff(): Boolean
    fun processMonth(input: MonthPaymentInput): MonthSettlement
}
```

### 3.3 Domain Invariants

The module automatically asserts and preserves the following domain invariants upon each monthly transition:

1. **Conservation of Principal**:
   $$\text{openingBalance} = \text{closingBalance} + \text{scheduled.principal} + \text{catchup.principal} + \text{extra.principal}$$
2. **Conservation of Interest**:
   $$\text{pendingInterest}_{\text{before}} + \text{interestCharged} = \text{pendingInterest}_{\text{after}} + \sum \text{paidInterest}$$
3. **Conservation of Insurance**:
   $$\text{pendingInsurance}_{\text{before}} + \text{insuranceCharged} = \text{pendingInsurance}_{\text{after}} + \sum \text{paidInsurance}$$
4. **Conservation of Cash**:
   $$\text{cashPaid} = \text{scheduled.applied} + \text{catchup.applied} + \text{extra.applied} + \text{commissionPaid} + \text{vatPaid}$$
5. **Overdue Capital Bounds**:
   $$0 \le \text{overduePrincipal} \le \text{closingBalance}$$
6. **Total Debt Identity**:
   $$\text{debt} = \text{closingBalance} + \text{pendingInterest} + \text{pendingInsurance}$$
7. **Arrears Identity**:
   $$\text{arrears} = \text{overduePrincipal} + \text{pendingInterest} + \text{pendingInsurance}$$
   *(Note: $\text{arrears} \le \text{debt}$ always holds because $\text{overduePrincipal} \le \text{closingBalance}$. Arrears are never added to total debt).*
8. **Non-negativity**: All monetary balances and charges are strictly non-negative.

### 3.4 Ordering Constraints

1. **Sequential Monotonicity**: Month index `input.month` must strictly equal `lastProcessedMonth + 1`. Calling a non-consecutive month throws `IllegalArgumentException`.
2. **Monthly Phase Order**:
   - Phase 1: Charge accrual on opening balance (interest and active insurance).
   - Phase 2: Overdue capital accrual for scheduled capital due.
   - Phase 3: Reference schedule progression.
   - Phase 4: Scheduled offer determination (including maturity residual check).
   - Phase 5: Waterfall imputation for scheduled payment tranche.
   - Phase 6: Waterfall imputation for catchup payment tranche.
   - Phase 7: Waterfall imputation for extraordinary prepayment tranche.
   - Phase 8: Prepayment rule fee evaluation on extra prepaid capital.
   - Phase 9: Closing balance snapshot and invariant verification.

### 3.5 Error Modes

- **Invalid Terms at Initialization**: Throws `IllegalArgumentException` if `principal <= 0`, `termMonths <= 0`, `monthlyRate < 0`, or `basePayment <= 0`.
- **Invalid Ordering**: Throws `IllegalArgumentException` if `input.month != lastProcessedMonth + 1`.
- **Invariant Failure**: Throws `IllegalStateException` if negative balances or overdue bound violations are detected at the closing step.

---

## 4. Implementation Hidden Behind the Seam

The implementation encapsulates all internal mechanics that callers do not need to know:

1. **Mutable State Registers**:
   The module internally encapsulates 5 mutable registers:
   `balance`, `interest`, `insurance`, `overdue`, `referenceBalance`.
   These are inaccessible directly from outside the module.
2. **Payment Allocation Waterfall**:
   When an amount is offered to `allocate(offered)`:
   - Category 1 (Insurance): `min(offered, pendingInsurance)`
   - Category 2 (Interest): `min(offered - paidInsurance, pendingInterest)`
   - Category 3 (Principal): `min(offered - paidInsurance - paidInterest, balance)`
   - Overdue reduction: Capital payments first reduce `overduePrincipal` to zero before reducing un-due balance.
3. **Independent Half-Up Rounding**:
   - Ordinary interest is rounded half-up to 2 decimals on opening balance.
   - Each active insurance item is rounded half-up to 2 decimals independently before summing.
   - No hidden unrounded fractional cents accumulate as shadow debt.
4. **Maturity Residual Absorption**:
   - The virtual `referenceBalance` tracks unconstrained theoretical amortization.
   - In month `termMonths`, if the loan was current at the start of charges and payment was not user-fixed, the scheduled offer absorbs the rounding residual:
     $$\text{scheduledOffer} = \text{totalDebt}$$
5. **Regional Prepayment Rules**:
   - In Argentina profile, checks whether $\text{month} < \lceil(\text{originalTermMonths} + 3) / 4\rceil$.
   - Calculates contract commission rate on prepaid capital, and VAT rate on the commission, without polluting standard loans.

---

## 5. Dependency Strategy & Adapters

In accordance with codebase design rules, adapters are introduced only where variance exists across a seam (*"One adapter means a hypothetical seam. Two adapters means a real one"*).

### Adapters at the Prepayment Seam:

1. **`StandardPrepaymentAdapter`**:
   - Used for general loans (COP, USD, EUR, standard ARS).
   - Returns zero commission and zero VAT for all months and amounts.
2. **`ArgentinaPrepaymentAdapter`**:
   - Encapsulates Argentina's regulatory prepayment rules:
     - 25% original term threshold: $\lfloor(\text{originalTerm} + 3) / 4\rfloor$.
     - Before threshold: $\text{commission} = \text{round}(\text{prepaidCapital} \times \text{commissionRate})$, $\text{VAT} = \text{round}(\text{commission} \times \text{vatRate})$.
     - After threshold: Zero fee and zero VAT.

---

## 6. Usage Examples

### 6.1 Orchestration in `simulateLoan`

`simulateLoan` collapses from a 250-line state machine into a lightweight timeline loop:

```kotlin
fun simulateLoan(scenario: Scenario): LoanResult {
    scenario.validate()
    val rate = toMonthlyRate(scenario.rate)

    val prepaymentRule = if (scenario.argentinaProfile != null) {
        val arg = scenario.argentinaProfile
        ArgentinaPrepaymentAdapter(
            originalTermMonths = arg.originalTermMonths ?: scenario.termMonths,
            commissionRate = Decimal.parse(arg.commissionRate),
            vatRate = Decimal.parse(arg.vatRate)
        )
    } else {
        StandardPrepaymentAdapter()
    }

    val account = AmortizationAccount.open(
        AmortizationTerms(
            principal = Decimal.parse(scenario.principal),
            monthlyRate = rate,
            termMonths = scenario.termMonths,
            basePayment = scenario.payment?.let { Decimal.parse(it) },
            isPaymentExplicit = scenario.payment != null,
            prepaymentRule = prepaymentRule
        )
    )

    val resultRows = mutableListOf<MonthRow>()
    for (month in 1..MAX_MONTHS) {
        // Timeline concerns: filter events, check post-payoff status
        val activeInsurance = scenario.insurance.filter { ... }
        if (payoffMonth != null && activeInsurance.isEmpty() && account.pendingInsurance.isZero) {
            continue
        }

        // Single call to the deep module across the seam
        val settlement = account.processMonth(
            MonthPaymentInput(
                month = month,
                isMissed = hasMissed,
                catchupOffer = catchupOffer,
                extraOffer = extraOffer,
                activeInsurance = activeInsurance
            )
        )

        resultRows.add(settlement.toMonthRow(dateStr))
        // Accumulate totals and check payoff
    }
    return LoanResult(...)
}
```

### 6.2 Testing Directly at the Deep Module Interface

Unit tests no longer require `Scenario` fixtures, date string formats, or 1200-iteration runs:

```kotlin
@Test
fun testWaterfallPriorityInsuranceThenInterestThenPrincipal() {
    val account = AmortizationAccount.open(
        AmortizationTerms(
            principal = Decimal.parse("1000.00"),
            monthlyRate = Decimal.parse("0.01"),
            basePayment = Decimal.parse("100.00"),
            termMonths = 12,
            isPaymentExplicit = true
        )
    )

    // Month 1: Missed
    account.processMonth(
        MonthPaymentInput(
            month = 1,
            isMissed = true,
            activeInsurance = listOf(Insurance(id = "life", name = "Vida", kind = "fixed", value = "5.00"))
        )
    )

    // Month 2: Partial catchup of 7.00 must allocate 7.00 entirely to insurance
    val m2 = account.processMonth(
        MonthPaymentInput(
            month = 2,
            isMissed = true,
            catchupOffer = Decimal.parse("7.00"),
            activeInsurance = listOf(Insurance(id = "life", name = "Vida", kind = "fixed", value = "5.00"))
        )
    )

    assertEquals("7.00", m2.catchup.insurance)
    assertEquals("0.00", m2.catchup.interest)
    assertEquals("0.00", m2.catchup.principal)
    assertEquals("3.00", m2.pendingInsurance)
    assertEquals("20.00", m2.pendingInterest)
}
```

---

## 7. Trade-offs and Depth Analysis

### High Leverage Areas
- **Interface-to-Behavior Ratio**: The interface consists of two primary types (`AmortizationTerms`, `MonthPaymentInput`) and one transition method (`processMonth`). Behind this sit over 15 financial rules (imputation priority, overdue accumulation, reference amortization, rounding, regional fee policies, and domain invariant assertions).
- **Locality**: Every change to financial conventions is localized in `AmortizationAccount`. Changes to calendar formatting, scenario storage, or UI charts cannot break amortization math.
- **Testability**: The module interface is the direct test surface. Tests verify observable outcomes (settlement snapshots) rather than internal variables.
- **Cross-Platform Parity**: The identical deep module interface is implemented in Kotlin Multiplatform and TypeScript, ensuring 100% calculation parity across platforms.

### Thin Leverage Areas (Deliberate Non-Inclusions)
To keep the module deep rather than bloated, certain peripheral concerns were intentionally left outside the seam:
- **Calendar Date Formatting**: Converting month numbers to `YYYY-MM` strings is an external timeline formatting concern. Including date parsing would pollute the interface with calendar logic.
- **Recurring Event Expansion**: Calculating `(month - start) % every == 0` is an event schedule expansion concern, kept in the timeline loop.
- **Post-Payoff Unapplied Event Auditing**: Storing records of unapplied events after loan payoff is an audit/reporting concern, kept in the scenario result collector.
