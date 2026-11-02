# Conversion rules

Every concept is created with `SPECFOLD_MODEL=<your model id> node scripts/specfold.mjs new <type> <path> --title "<title>" --description "<one line, max 160 characters>"`, then given a body and `sources`. Paths in `sources` are relative to the concept file, so a concept in `knowledge/decisions/` points at `../../docs/adr/0001-x.md`.

```yaml
sources:
  - id: adr-0001
    resource: ../../docs/adr/0001-use-postgres.md
```

Summarize; do not copy. A concept says what was decided or agreed and why, in a few lines, and its source keeps the full text.

## By source kind

- **ADR → `Decision`.** The title is the decision, and `# Decision` and `# Why` summarize the ADR's Decision and Context. Map the ADR status to `decision:`:
  - accepted → `accepted`;
  - rejected → `rejected`;
  - superseded or deprecated → `superseded`, plus `status: deprecated`, with a link to the successor's concept in `# Related`;
  - proposed, or no status: not a decision yet. Ask the user at the plan checkpoint whether it is accepted, or whether it stays `keep`.

  The successor names a superseded decision in its text instead of linking it, because a link to a deprecated concept is an audit finding (A3); the superseded concept then shows up as unlinked (A4), which the report lists for review.
- **Agent rule file → `Convention`** (`.cursor/rules/*.mdc`, `.github/copilot-instructions.md`, `.windsurfrules`, a team's `AGENTS.md` rules). One concept per topic, not per file. The rule file stays where it is: other tools still read it.
- **Existing `## graphify` section** in `AGENTS.md` or `CLAUDE.md`: list it in the plan. After install, merge its advice into the lookup order inside the Specfold block, and remove the duplicate section only if the plan row says so.
- **`docs/` page** → the type its plan row names: architecture pages become `Component` or `Data Flow`, setup and operations pages `Runbook`, glossaries `Term`, product rules `Business Rule`.
- **README and CONTRIBUTING** usually stay (`keep`). Contribution rules that agents must follow become a `Convention`.
- **Another spec tool** (`openspec/specs/`, `.specify/`, `.kiro/specs/`): one `Capability` card per spec or feature folder, with `resource` pointing at it. No bulk conversion: list each one in the report as a migration candidate. An existing `openspec/` stays as it is; install only made `specfold` the default schema for new changes.

## Seed from code

Up to the cap in the plan (default 30), across all three kinds:

- **`Component`**: one per top-level module or package in `source_globs`, with `resource` pointing at its folder. With `graphify-out/GRAPH_REPORT.md` present, use its communities instead of guessing.
- **`Interface`**: public APIs, CLIs or events that other code or other systems use.
- **`Term`**: domain names that appear across modules (entities, states), one line each.

Prefer fewer, accurate concepts over many thin ones. Stop at the cap even if more would fit, and name what was left out in the report.

## Code comments

Long rationale comments in code (several lines explaining why) belong in knowledge, but the migration never moves them. List each with `path:line` in the report, so a later change can capture it with `sf-capture` and leave a `see knowledge:` pointer.
