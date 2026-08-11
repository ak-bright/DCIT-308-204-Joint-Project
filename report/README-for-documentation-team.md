# Note for the Documentation & Report Team (2 people)

Hi — this is a plain-language guide to what Bright (via Claude Code) has already
finished versus what still needs *your* judgement. Please read this before you
start formatting the report into Word.

## What this report is

A complete **first draft** of all 12 required sections. Each section is a separate
Markdown file in [`sections/`](sections/), and there is a single combined file,
[`final-report-draft.md`](final-report-draft.md), that stitches them together for
convenience. Everything is written to be readable by a non-programmer while staying
technically accurate.

## ✅ Already final — please do NOT change the substance

These are generated from the real code, real test runs, and real measurements. You
may reformat, restyle, and fix typos, but **do not alter the numbers or technical
claims** (if something looks wrong, ask Bright rather than editing it):

- **Section 6 & 7** — the algorithms, pseudocode, Big-O, trace tables, proofs and
  the two counterexamples. These match the code exactly.
- **Section 8 & `performance/interpretation.md`** — the performance tables are
  **real measured numbers** from `bash build.sh bench`. Do not invent or "round for
  neatness" in a way that changes them. The charts are in `performance/graphs/`
  (SVG — they open in any browser; you can paste them into Word, or re-export PNGs
  with `performance/plot.py` if you prefer).
- **Section 9** — the schema, sample records and the algorithm-run log are pulled
  from the actual SQLite database.
- **The code, tests and schema** in `src/`, `database/schema.sql` — please don't
  edit these at all.

## ✏️ Placeholders you MUST fill in

1. **The 15 team member names.** They appear as blanks in:
   - Section 1 (cover page table)
   - Section 11 (contribution-statement table)
   - The root `README.md` team table
   Bright is confirmed as Group Leader; the other 14 names are yours to insert.
2. **Two-sentence contribution statement per member** — Section 11.1. Collect one
   from each of the 15 people and paste it in.
3. **Submission date and academic year** — Section 1.
4. **Optional screenshots** — Section 9 invites a console or DB-browser screenshot;
   the exact data those would show is already in the tables, so screenshots are a
   nice-to-have, not required.

## ⚠️ The one big data caveat — PLACEHOLDER dataset

The dataset currently in `database/seed-data/` is **auto-generated placeholder
data**, not the real hospital information. Every CSV starts with a
`# PLACEHOLDER …` banner line so it's obvious. It exists so the system runs and the
report has concrete numbers while the real data is being finalised.

**When the Data Collection + Database Teams deliver the real CSVs:**
- Replace the four files in `database/seed-data/` (keep the exact same column
  headers — see section 3).
- Delete the old `database/hospital.db` so it rebuilds from the new data, and
  re-run the app once (`bash build.sh run`) and the benchmark if you want fresh
  numbers.
- Then update section 3's row counts and the section 9 sample records if they
  changed. The *structure* of the report won't change — only the specific numbers.

Nothing else in the report depends on the placeholder data being "real"; the
data-structure/algorithm/performance content stands on its own.

## How to check things yourself (no coding needed)

Open a terminal in the project folder and run:
- `bash build.sh test` — should print **60 tests successful, 0 failed**.
- `bash build.sh run` — launches the menu so you can try the features for
  screenshots.

If any of that fails, contact Bright — it should not, as of this draft.
