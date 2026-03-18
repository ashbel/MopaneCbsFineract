# Local Maven repository for Pentaho Reporting (Fineract)

Apache Fineract (legacy Mifos) depends on Pentaho 3.9.x JARs that are **not** on Maven Central. Public mirrors (Hitachi Artifactory, cobuys.com) no longer work.

**One-time setup** (from repository root, same folder as `gradlew`):

```powershell
powershell -ExecutionPolicy Bypass -File fineract-provider/scripts/bootstrap-pentaho-m2.ps1
```

Gradle reads dependencies from `fineract-provider/pentaho-m2-local/` (see `build.gradle`).

This downloads from SourceForge (~70 MB) and Pentaho Report Designer CE (~178 MB on first run), then lays out JARs + minimal POMs here so Gradle can resolve:

- `pentaho-reporting-engine:*`
- `pentaho-report-designer:*`
- `pentaho-library:*`

After this, `gradlew build` / `gradlew war` should resolve Pentaho dependencies.

Optional:

- `MOPANE_PRD_ZIP` — path to an existing `prd-ce-3.9.1-GA.zip` to skip downloading PRD.
- `-SkipPrdDownload` — fail if wizard/scripting JARs are missing (use when you will supply `MOPANE_PRD_ZIP` later).
