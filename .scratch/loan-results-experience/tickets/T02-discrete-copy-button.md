# T02 — Discrete Copy Button Component (Hover Clipboard & Check Feedback)

Status: closed
Depends On: T01
Blocks: T04, T05

## Description
Refactor `CopyResult` and `CopyMoney` in `CopyResult.kt`:
- At rest, display only the clean value without the noisy `· Copiar` text suffix.
- On mouse pointer hover (or tap on touch), reveal a subtle clipboard vector icon alongside the number.
- When clicked, copy the unformatted number to clipboard and transform the icon into a green checkmark (**✓**) for 1.5 seconds.
- Crucially, preserve `contentDescription = "Copiar $label"` on the Compose semantics node so screen readers and automated test locators (`getByRole('button', { name: 'Copiar ...' })`) continue to match effortlessly.

## Acceptance Criteria
- [x] No visible `· Copiar` text cluttering the UI at rest.
- [x] Hover reveals clipboard icon; clicking triggers copy and shows checkmark feedback.
- [x] Accessibility tests and Playwright button locators pass cleanly.
