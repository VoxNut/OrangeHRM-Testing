# Recruitment offline handoff tools

`team_feedback.py audit` captured the release before repair; `team-feedback/audit.json` is that fixed snapshot, not a baseline to regenerate to hide drift. `build` derives new case/anomaly indexes from the official reports; `validate` checks source/report preservation, suite structure, GUID/FQCN uniqueness, status agreement, PNG hashes and relative links. Existing historical summaries/manifests are read-only.

From repository root with Python 3:

```powershell
python maintenance/asm8-23130299/team_feedback.py validate
python maintenance/asm8-23130299/offline_feedback.py --katalon-home '<your-installed-Katalon-Studio-11.6.0-directory>' --output '<new-dedicated-validation-directory>'
```

The offline compiler uses the real Studio jars, a generated GlobalVariable class from the actual Profile and renamed temporary copies of scripts to avoid duplicate generated script class names. Production source is unchanged. It compiles Antlr2 and Parrot, then exercises production methods with browser boundaries guarded. It never calls Katalon CLI or executes a test case/browser/API.

`checks/check_latest_portability.groovy` and `checks/check_latest_flow.groovy` are exact copies of the original Downloads harnesses, SHA recorded in `team-feedback/offline/results.json`. The FLOW harness itself calls portability; the wrapper gives it a separate fresh sandbox to preserve the no-overwrite assertion. Use a new `--output` for another verification; existing logs are never replaced. Optional `--reference <reference-project>` checks the shipped harnesses against their originals.

`team-feedback/offline/` is the final focused verification, including the FLOW canary. `offline-initial/` retains the initial focused verification before adding that canary, also PASS. Neither changes the archived full legacy regression: it remains PARTIAL with its original failures/unavailable captures.

`reference-evidence.json` records source-only historical observations and hashes without copying those screenshots/reports into the submission. The linked anomaly index explicitly labels reference-only records and unmatched evidence. Runtime browser profiles, cookies and session context are excluded.
