# Finsim Glossary

This document establishes the canonical terminology for the Finsim codebase across both financial domain concepts and architectural design principles.

---

## 1. Domain Terminology

### Amortization Schedule (Cronograma de amortización)
A chronological monthly table detailing the systematic repayment of a loan over time. Each row captures the month index, opening balance, interest charged, insurance charged, scheduled payment, extra payments, applied distribution across obligations, and closing balance, terminating when the principal reaches zero or the maximum operational limit (1,200 months) is attained.

### Allocation / Imputation (Imputación / Prelación de pagos)
The strict priority sequence governing how incoming cash is applied across distinct obligations during monthly settlement. In Finsim, incoming payments are imputed in strict cascade order:
1. Pending insurance fees from prior missed months.
2. Current insurance fees for the active month.
3. Pending interest accumulated from prior missed months.
4. Current monthly interest.
5. Overdue capital from prior missed months.
6. Current regular scheduled principal.

Any remaining surplus from prepayments is imputed directly to reduce outstanding principal.

### Arrears (Atrasos / Mora)
The aggregate accumulation of past due amounts resulting from missed or incomplete monthly payments. Arrears encompass unpaid insurance, unpaid interest charges, and overdue capital that must be settled in full before subsequent payments can amortize regular scheduled principal.

### Overdue Capital (Capital vencido)
The portion of scheduled principal that was billed in prior months but remained unpaid due to missed installments or partial payments. It remains outstanding as a distinct past obligation and takes priority over future scheduled capital during payment imputation.

### Prepayment Profile (Perfil de precancelación)
A rule set determining the financial and contractual terms under which prepayments or early loan payoffs are processed. For example, the Argentine Prepayment Profile (`argentinaProfile`) specifies statutory exemption thresholds (free of penalties after 25% of original term or 180 days) and calculates mandatory commissions and VAT penalties for prepayments executed prior to the threshold.

### Scenario (Escenario)
The self-contained, versioned input specification required to simulate a loan. Encapsulates principal, start date, term in months, interest rate (monthly, annual effective, or nominal), known or computed base payment, currency (COP, ARS, USD, EUR), insurance items, loan events (extra payments, recurring payments, missed payments, catchup offers), and regional profiles such as the Argentine prepayment profile.

### Reconciliation Row (Fila de conciliación / Fila de extracto)
A structured record representing a single period extracted from an actual bank loan statement. Contains month index, opening balance, interest charged, insurance charged, principal amortized, and closing balance, compared against simulated schedule rows to identify and quantify signed discrepancies.

### Regulatory Framework (Marco regulatorio)
The statutory jurisdiction and banking supervisory rules governing loan contracts in a target jurisdiction. Dictates maximum loan tenors, grace period treatments, consumer prepayment rights, commission caps, tax treatment (such as VAT on financial fees), and mandatory disclosures.

### Tier 1 Text Extraction (Extracción de texto Tier 1)
The fast, zero-dependency on-device text parsing stage in document ingestion. Extracts raw text streams from electronic PDFs using regex heuristics and density scanning to recognize loan terms, statement tables, dates, and currency values locally without invoking heavy neural models.

### Tier 2 VLM Vision (Visión VLM Tier 2)
The deep on-device vision-language model inference stage in document ingestion. Invoked when Tier 1 confidence is insufficient or when documents are scanned images. Rasterizes document pages and evaluates them against a structured JSON grammar to reliably extract complex statement layouts, tabular grids, and bank notices.

---

## 2. Architecture Terminology

The codebase adheres strictly to deep-module design terminology:

### Module
Anything with an interface and an implementation. Deliberately scale-agnostic: a function, class, package, or tier-spanning slice (such as the `:shared` financial engine).  
_Terms to avoid_: unit, component, service.

### Interface
Everything a caller must know to use the module correctly: the type signatures, but also invariants, ordering constraints, error modes, required configuration, and precision/performance characteristics.  
_Terms to avoid_: API, signature (these are too narrow, as they refer only to type-level syntax).

### Implementation
What is inside a module; its internal body of code. The implementation is private to the module and hidden behind the interface. Distinct from an adapter.

### Depth
Leverage at the interface. The amount of behavior a caller (or test) can exercise per unit of interface they have to learn. A module is **deep** when substantial behavior sits behind a concise interface, and **shallow** when the interface is nearly as complex as the implementation.

### Seam
A place where you can alter behavior without editing in that place; the specific location where a module's interface lives. Where to locate the seam is a primary design decision, distinct from what goes behind it.  
_Terms to avoid_: boundary.

### Adapter
A concrete thing that satisfies an interface at a seam. Describes the role a participant fills, rather than its internal mechanics (for example, `ArgentinaPrepaymentAdapter` satisfying `PrepaymentRule`).

### Leverage
What callers gain from module depth. Callers receive substantial capability per unit of interface learned. One deep implementation pays back across multiple caller targets and tests.

### Locality
What maintainers gain from module depth. Changes, bug fixes, domain rules, and verification concentrate in one central module rather than spreading across callers. Fix once, fixed everywhere.
