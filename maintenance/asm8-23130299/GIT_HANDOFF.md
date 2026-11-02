# Recruitment ASM8 release — review branch

Branch: `feat/asm8-recruitment-23130299`. Base: `6f8bd31d5580365a925343c21515daeb7c46b11e`. Linked worktree used to preserve original NVThang branch, workbook dirty changes and 141 pre-staged deleted-worktree screenshots. No original index entries are included by accident. Use `git log -1` for the release commit SHA; this document does not attempt to embed its own commit hash.

Scope: canonical member project, root README appendix, Recruitment Individual report/evidence folders and integration-pending marker. No other member source is changed. Source selection: 208 original files plus two required Python preflight tools; README documentation updated. Compare source-comparison.json and SOURCE_PRESERVATION.json. Required Profile and preflight plan are included as exact source bytes. Historical source locks remain intact. Latest local 71P/0F/3E/1S is not a group integration result.

Offline: {"PASS": 36, "FAIL": 6, "UNAVAILABLE_HISTORICAL_CAPTURE": 2}; Antlr2/Parrot production104/generated106 PASS; latest portability PASS. Full regression PARTIAL. Original legacy failures/missing captures are preserved. No browser/demo writes, no fake reports.

Integration blockers: no existing unified group project; separate Katalon/Playwright/Cypress source; TIME not present in this branch. Listener currently global to project: scope it before adding other members' TC. Group lead must agree runner/source and approve Recruitment inclusion. Keep Recruitment SKIPPED; no automatic merge to dev/main. Follow repository PR → dev policy after review. Individual Classroom ZIP stays local and is not committed. No PPTX changes/upload.

If push is unavailable, run `git push --set-upstream origin feat/asm8-recruitment-23130299` from the release worktree after verifying clean status and remote. Never force push. Root user worktree stays NVThang; do not reset/stash/restore its dirty changes.

Scoped `.gitattributes` disables text conversion only within the member project and submitted report/evidence folders, preserving audited raw SHA on Git and fresh clones. Root .gitattributes is untouched. Some previously normalized Git blobs now differ only by original CRLF bytes; source scripts/.tc/assertions in Downloads remain untouched. All index blobs must equal staged payload byte-for-byte.
