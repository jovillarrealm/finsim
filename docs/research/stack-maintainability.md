# Stack maintainability for the financial simulator

Research date: 2026-09-13. Scope: browser-only educational dashboards, initially two loan schedules with prescribed events; later hypothetical CDT/ETF comparisons and rate conversion. This is a documentation-based architectural assessment, not a benchmark or implemented prototype.

## Recommendation

Use **TypeScript in strict mode, React, Vite, Recharts, decimal.js, and Zod**, with Vitest for financial-engine tests and a small browser acceptance suite. Keep one application and one dependency/build ecosystem. Keep calculations in ordinary framework-independent TypeScript modules in the same project; a separate module does not require a separate package, deployment, process, or language.

This is an engineering judgment for the accepted scope, not evidence that TypeScript is universally more maintainable. No present requirement demonstrates a benefit sufficient to justify the Rust/JavaScript boundary. Rust remains technically viable, including an all-Rust authored frontend. The earlier hybrid recommendation placed too much weight on hypothetical native reuse.

## Evidence and practical comparison

| Choice | Documented capability | Assessment for this project |
| --- | --- | --- |
| TypeScript + React + Vite | Vite has an official React integration and imports TypeScript; builds can deploy to GitHub Pages. | Recommended: calculations, form state, charts, and saved schemas can use one language without generated cross-language interfaces. |
| Rust + Leptos CSR + Trunk | Compiles the application to Wasm; publishing its `dist` directory is sufficient, with documented Pages deployment. | Credible alternative if writing Rust is itself a priority. Validate the chart interactions before committing. |
| Rust engine + React/TypeScript | `wasm-bindgen` defines supported boundary types and generates browser bindings. | Adds two dependency graphs, build orchestration, conversion rules, and integration tests. No established need here compensates for that work. |
| Svelte + TypeScript | Official TypeScript support and checking; SvelteKit can produce static output. | Also defensible. Compiling UI components does not give TypeScript Rust's type semantics. No evidence establishes a universal React-versus-Svelte maintenance winner. |

Sources: [Vite features](https://vite.dev/guide/features.html), [Vite static deployment](https://vite.dev/guide/static-deploy.html), [Leptos getting started](https://book.leptos.dev/getting_started/index.html), [Leptos CSR deployment](https://book.leptos.dev/deployment/csr.html), [wasm-bindgen supported types](https://wasm-bindgen.github.io/wasm-bindgen/reference/types.html), [Svelte TypeScript](https://svelte.dev/docs/svelte/typescript), [SvelteKit static adapter](https://svelte.dev/docs/kit/adapter-static).

“One toolchain” means one authored language and package/build workflow, not literally one executable. TypeScript needs checking as well as transformation: Vite explicitly does **not** type-check. Make `tsc --noEmit` a required build check. [Vite TypeScript behavior](https://vite.dev/guide/features.html#typescript).

## Charting is the relevant ecosystem question

Recharts documents bar, line, composed, and stacked charts, responsive sizing, tooltips, reference markers, and synchronization. These map directly to principal/interest/insurance bars, selected-month comparison, extra-payment markers, and later investment trajectories. React is recommended partly because these capabilities fit its component model directly. Shared selected-month state remains application logic. [Recharts API](https://recharts.github.io/en-US/api/).

Recharts supplies keyboard/screen-reader support, but that is not proof of an accessible application. Provide a real monthly HTML table, explicit labels, and keyboard-accessible event controls; verify the delivered interaction with keyboard and assistive technology. Selecting a bar must not be the only way to choose a month. [Recharts accessibility documentation](https://github.com/recharts/recharts/blob/main/storybook/stories/API/Accessibility.mdx).

Rust does have charting options: Chartistry provides a Leptos `Chart` component and examples. This research has not established that its current API covers the complete stacked-bar, synchronized-selection, responsive, accessible workflow. That uncertainty is a reason for a small prototype if Rust is selected, not grounds to claim Rust has no ecosystem. [Chartistry repository](https://github.com/feral-dot-io/leptos-chartistry).

Another Rust option is wrapping a JavaScript chart library. ECharts documents stacked bars, events, and accessibility descriptions/patterns. However, Leptos explicitly explains that Wasm accesses browser APIs through bindings and cautions about ownership when JavaScript libraries manipulate DOM state. That recreates some integration work the user wants to avoid. [ECharts stacked bars](https://echarts.apache.org/handbook/en/how-to/chart-types/bar/stacked-bar/), [events](https://echarts.apache.org/handbook/en/concepts/event/), [accessibility](https://echarts.apache.org/handbook/en/best-practices/aria/), [Leptos JavaScript integration](https://book.leptos.dev/web_sys.html).

## Types and money correctness

Enable strict TypeScript, use discriminated unions for event and rate variants, and represent units explicitly: money, monthly rate, annual effective rate, and month index should not be interchangeable by accident. TypeScript supports exhaustive checks using `never`; Zod validates imported data at runtime and infers static types. These tools reduce mistakes without promising Rust-equivalent guarantees. [TypeScript strict](https://www.typescriptlang.org/tsconfig/strict), [narrowing and exhaustiveness](https://www.typescriptlang.org/docs/handbook/2/narrowing.html), [Zod](https://zod.dev/).

Neither language automatically makes a loan model accurate. Use decimal.js for financial arithmetic, parse decimal input from strings, define precision centrally, and specify rounding points and modes. Convert to ordinary numbers only for plotting coordinates; retain decimal values for totals and labels. Decimal.js supports configurable precision, rounding, and fractional powers needed for rate conversion; these powers are approximations, not a guarantee of mathematically exact results. [decimal.js API](https://mikemcl.github.io/decimal.js/).

Rust's `rust_decimal` also offers decimal arithmetic, rounding strategies, string serialization, and optional mathematical functions. It has finite precision. Choosing Rust would not resolve payment ordering, insurance bases, overdue-payment allocation, or statement-rounding conventions. [rust_decimal documentation](https://docs.rs/rust_decimal/latest/rust_decimal/).

Test known schedules plus invariants: principal conservation, final-payment handling, rate-conversion tolerance, no-event equivalence, insurance stopping rules, and explicit delinquency examples. Use independently checked expected figures rather than computing expected outputs with the implementation under test. Vitest supports the Vite/TypeScript workflow. [Vitest guide](https://vitest.dev/guide/).

## Save files, hosting, and flexibility

Store versioned scenario inputs, events, assumptions, and decimal strings as JSON. Validate on import and migrate old versions deliberately. Recompute schedules instead of persisting derived chart state. Keep a small persistence adapter around localStorage and offer file export/import. localStorage persists across sessions but is origin-specific and can be cleared; it is not cross-device synchronization. [MDN localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage).

GitHub Pages can host either the TypeScript build or Leptos Wasm files. Configure the repository base path; initially use tabs or hash-based navigation to avoid direct-route deployment complications. No backend is needed for user-entered simulations. Automatic live product data and accounts would introduce separate requirements. [Vite Pages deployment](https://vite.dev/guide/static-deploy.html#github-pages), [Leptos Pages deployment](https://book.leptos.dev/deployment/csr.html#github-pages).

Keep loan and investment calculations separate, sharing only meaningful primitives such as rates and cash-flow records. Defer a generic simulation framework, global state library, monorepo, backend, and native packaging until a concrete need appears. A browser-independent TypeScript engine still allows other interfaces; Tauri supports existing static web frontends, although native packaging brings its own toolchain later. [Tauri frontend configuration](https://v2.tauri.app/start/frontend/).

Choose Leptos if Rust learning or Rust-only maintenance is an explicit project objective and a chart prototype succeeds. Choose the hybrid only after a measured computational bottleneck, an existing Rust engine worth reusing, or a concrete independent native consumer justifies its boundary. Choose Svelte if its authoring model is materially preferred. None of those conditions is currently established.
