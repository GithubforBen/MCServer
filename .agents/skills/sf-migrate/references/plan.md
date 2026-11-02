# Migration plan template

Write `.specfold/migration/plan.md` with these sections. Keep it short: the user approves it in one sitting.

```markdown
# Migration plan: <repository name>

## Repository

- Default branch: <from migrate status>
- Stack: <detected, or corrected>
- Layout: <single package or monorepo>
- CI: <system, or none>
- Source globs: <globs install writes to specfold.yaml>
- Agent tools: agents, claude   <!-- or agents only -->

## Capability domains

<!-- 2-6 areas the product is made of, each one line. They become knowledge folders or capability cards later. -->
- <domain>: <one line>

## Mapping

| Source | Becomes | Target | Note |
| --- | --- | --- | --- |
| docs/adr/0001-x.md | Decision | knowledge/decisions/x.md | accepted |
| AGENTS.md | keep | managed block | team rules stay below the Specfold block |
| .cursor/rules/style.mdc | Convention | knowledge/conventions/style.md | |
| README.md | keep | | entry point for humans |
| openspec/specs/login/ | Capability | knowledge/capabilities/login.md | migration candidate |

## Seed from code

- Cap: 30 concepts
- From: <source globs, or graphify-out/ when present>

## Gate

- This PR is the adoption PR: G4 is skipped, every other rule runs.
- From the next PR on, source changes need an OpenSpec change (G4).

## Open questions

none
```

**Becomes** is one of: a concept type (`Decision`, `Convention`, `Term`, `Business Rule`, `Component`, `Interface`, `Data Flow`, `Runbook`, `Reference`, `Capability`), `AGENTS.md section`, `keep` (stays as it is) or `ignore` (not knowledge, such as a changelog). Every inventory source gets exactly one row; group many files of one folder into one row only when they all get the same treatment.

After the user approves, append:

```markdown
## Approval

approved_by: human:<login the user stated>
approved_at: <ISO 8601 with offset, from `date`>
```
