# BLOCKED — unified group project

## Recruitment feedback verification — 2026-11-02

The release base checked here has no `run.mjs` or `summarize.mjs`. The preserved NVThang checkout does have both: `run.mjs --check` verified 187 IDs including three Recruitment suites of 25, all explicitly SKIP. Its execution adapters handle Playwright PIM and Cypress Leave. `--member=23130299` selects inventory but has no Katalon execution adapter; it must not be treated as a Recruitment run. Its skip text still mentions missing Profile/plan, which are already supplied on the Recruitment release. This is a branch-integration gap, not missing source handoff. `summarize.mjs` reads group-run JSON; it does not import official Katalon HTML/JUnit and currently has only Pass/Fail/Skipped totals.

Keep **SKIPPED_BY_GROUP_DECISION / GROUP_INTEGRATION_PENDING**. No shared runner code or other member modules were copied or changed. Review [Recruitment checklist](../docs/automation/recruit-checklist.md), [report index](../Test%20Report%20-%20Group%2001/Individual/23130299/index.html) and [verification report](../docs/automation/ASM8_RECRUITMENT_TEAM_LEADER_FIX_REPORT.md).

Before an opt-in Katalon adapter, the lead must approve demo writes, fixtures/cleanup, exact active numeric identities, manual login and licensed KRE availability. Studio's package includes a CLI executable on the inspected machine; license readiness and CLI browser execution are NOT VERIFIED. Preserve ERROR separately from FAILED when designing aggregation. For the operator's future standalone/suite smoke, follow the canonical Recruitment README and SMOKE_PLAN; create distinct FIRST_RUN/RETEST outputs. Do not reuse the three UNCLASSIFIED local runs as a shared run or replay uncertain Candidate81 Save. No adapter is enabled by this handoff.

The original staging note below describes this release branch, not every branch in the repository.

This folder is a deliverable staging marker, not a completed unified automation project. Canonical Recruitment project: `../automation/23130299/KatalonStudio/OrangeHRM_RECRUIT_VAC_KatalonStudio.prj`.

Repository contains separate member projects: Recruitment Katalon, PIM/ADM Playwright, LEAVE Cypress. No existing common Katalon project or agreed all-member runner exists in the checked branch. TIME source is not present on this branch. Copying these into one folder would not integrate them. Preserve canonical layout and obtain the group's integration design/missing source. Recruitment stays SKIPPED by lead; source compilation does not prove shared execution. No duplicate `.prj` was created here.
