# 05: Automated E2E & 6-Pillar Regression Verification Suite

**What to build:** Build an automated end-to-end and regression test suite verifying the 6 pillars from `STITCH-SCREENS-SPEC.md`: zero-mock mathematical invariants, bidirectional state synchronization between curves and active month inspection, Warm Botanical tokens and safe area layout, edge cases (no prepayments vs immediate payoff), and CSV round-trip verification.

**Blocked by:** 02: KMP Compose Hero Savings Card & Executive 2x2 KPI Grid, 03: KMP Compose Dual-Curve Trajectory Canvas & Extinction Zone, 04: KMP Compose Key Milestones Schedule & CSV Export

**Status:** closed

- [x] All 6 pillars verified through automated suites (`npm test` and `gradle jvmTest` / `gradle testDebugUnitTest`).
- [x] Invariant tests verify cash conservation, balance monotonic decrease, and payoff extinction.
- [x] Visual tokens, font variant tabular-nums, and touch target sizes verified.
- [x] All test runs pass in CI without warnings or flaky behavior.
