# testing/ — QA / manual testing

Plain-language manual test evidence for the console application — one entry per
menu feature, recording whether it worked and anything unexpected (no code
knowledge needed to read it).

- `manual-qa-log.md` — the completed manual QA checklist and results.

This complements the automated JUnit 5 suite under `src/test/java/`; run that with
`bash build.sh test` (or `.\build.ps1 test`).
