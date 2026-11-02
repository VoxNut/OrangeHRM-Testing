# Gap analysis before repair — 2026-11-02

Baseline e7c89705625cb4809ae6a8e5da81fb6871ab011d; working checkout NVThang is a different history. Release has already handed over the files. No source recopy is justified.

| Area | Observed before repair | Necessary action |
| --- | --- | --- |
| Runtime paths | PortablePaths + ASM7_EVIDENCE_ROOT present; no personal runtime path found | Rerun unchanged 10-group harness in fresh temporary directory; preserve source |
| Prior portability claim | Archived log has 9 PASS then FileAlreadyExistsException, despite handoff saying PASS | Preserve old log; publish actual fresh result separately |
| Reports | All three latest official HTML/JUnit/log leaves already present | Reuse unchanged, add member-level entry point |
| Supplementary | JSON/HTML/XML and 288 original PNGs already present | Link; no duplicate evidence |
| RESULTS75 | Existing derived JSON/CSV embeds original layout paths | Preserve original; generate new official-derived JSON/Markdown with portable repository links |
| Failure analysis | Existing analysis uses ../Reports and ../evidence paths that no longer resolve in report package | Preserve archived analysis; add corrected linked analysis and anomaly index |
| Checklist | Still says CAND/FLOW unverified and old VAC only | Update to latest provenance and explicit group pending |
| Runner | Release has staging README but no run.mjs/summarize.mjs; NVThang has Node adapters and explicit Recruitment skip | Document both branch states; do not copy/rewrite shared runner |
| Standalone | Source/Profile/plan/runtime workbook present | Offline compile/preflight; browser remains NOT VERIFIED |
| Integration | Lead decision is SKIPPED; no shared Recruitment execution | Keep SKIPPED_BY_GROUP_DECISION / GROUP_INTEGRATION_PENDING |
