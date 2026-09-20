# Financial simulator: agreed product brief

Status: first loan implementation available locally. Technical verification and remaining acceptance are tracked in [tracking](tracking.md).

## Purpose

A Spanish-first educational tool for the owner and their inner circle to understand financial decisions through editable scenarios. Start with Colombian loans; add investment comparisons later. Users prescribe events rather than assign probabilities to uncertain futures.

## Platform

- TypeScript, React, and Vite; a browser application with static GitHub Pages hosting and zero hosting cost.
- Local scenario saving and file export/import; no account or cross-device synchronization required.
- Responsive desktop and phone use. Native packaging is deferred.
- Calculation modules remain independent of React within the same project. See [stack research](research/stack-maintainability.md) for supporting recommendations; supporting libraries remain implementation choices.

## First release: loan exploration

Model fixed-rate loans in COP, ARS, USD or EUR (one currency per scenario, no currency conversion) with monthly payments. Accept either an original loan or a current balance with remaining conditions. Expose assumptions so users can reconcile the schedule with a real loan.

Show two scenarios at a time: the original schedule and a modified schedule using the reduce-term prepayment mode. Defer reduced-installment comparisons and additional simultaneous scenarios. When missed payments are prescribed, the modified schedule may extend rather than shorten the term.

Use aligned stacked monthly bars for principal, interest, and insurance. Keep axes and the time scale consistent so payments eliminated by earlier payoff are visible. Summaries show payoff date, payment amounts, total interest, insurance, cash paid, and signed differences from the original. Extra principal payments must be visible and included in cash paid; label them distinctly from scheduled principal.

Provide an expandable monthly table with opening balance, scheduled payment components, extra principal, any overdue charges, and closing balance. Differences can represent additional cost as well as savings.

### Direct editing

The user requested "click where to edit." Interpret this as selecting a month directly on the chart to open nearby event controls, and selecting displayed input values to edit them in place. Keep these controls keyboard-accessible and available through the monthly table as well. This interpretation should be checked in the first interface preview.

Offer a clearly labeled example loan for immediate experimentation and a route to enter a personal loan. Use Spanish labels, Colombian number/currency formatting, and short explanations of financial terminology.

### Events and insurance

- Single extra principal payments.
- Recurring extra principal payments.
- Missed payments followed by explicitly scheduled catch-up payments.
- In the initial monthly model, extra principal is applied after the scheduled payment; disclose this timing.
- Separate insurance charges may be fixed monthly amounts or percentages of outstanding principal. Each specifies whether it ends at payoff.
- Missed payments use a visibly illustrative policy, not a claim to reproduce every lender. Rounding, insurance timing, payment allocation, arrears, and unpaid insurance follow [financial conventions](financial-conventions.md). Their implementation must be verified before this feature is released.

### Reconciliation and rate conversion

Allow manual entry of selected statement rows and show differences against simulated values. Explain mismatches instead of silently fitting the calculation. PDF processing is not required; a documented, versioned import schema can later accept externally extracted data.

Include a standalone converter for effective annual, effective monthly, and nominal annual rates with an explicit compounding frequency. Explain equivalence with a worked example. Defer advance-payment rate conventions.

## Next milestone: investment comparisons

Compare hypothetical Colombian CDT and ETF strategies using the same starting capital, contribution schedule, and horizon. Display results in COP.

- Enter CDT rate and maturity, ETF return assumptions, contributions, withdrawals, and applicable costs.
- Explicitly decide whether CDT proceeds remain as cash or are reinvested at an entered rate.
- For foreign-currency ETFs, show an editable exchange-rate assumption.
- Clearly distinguish assumed ETF performance from known inputs. Support prescribed changes over time.
- Compare ending value, contributions, costs, and access to funds.
- Defer historical replay, live product lookup, and loan-prepayment-versus-investment comparisons.

## Acceptance criteria

1. Enter a loan and inspect its monthly interest, principal, insurance, and balance.
2. Select a month and add or change an extra payment; both charts and totals update consistently.
3. Reconcile several manually entered statement rows, with differences visible.
4. Explore missed and catch-up payments under a documented illustrative policy.
5. Inspect time and cost differences, including increases, without hiding the extra money contributed.
6. Convert supported rate conventions with independently checked examples.
7. Save, export, import, and reload a scenario while preserving its inputs and events.
8. A member of the intended audience can explain what changed without coaching.

Financial verification should cover independently checked schedules, principal conservation, final payment handling, no-event equivalence, rate conversion tolerances, insurance termination, and delinquency examples. Decimal arithmetic still requires explicit precision and rounding rules.

## Remaining verification before implementation/release

- The user authorized implementation of the consolidated brief; audience acceptance remains to be observed.
- Check direct editing and chart readability in the first interface preview.
- Verify the implementation against the specified [financial conventions and reference examples](financial-conventions.md), particularly rounding, insurance calculation timing, and illustrative delinquency behavior. These are simulator rules, not a universal lender policy.
- Reconcile against a real example before describing any loan model as matching that lender.
