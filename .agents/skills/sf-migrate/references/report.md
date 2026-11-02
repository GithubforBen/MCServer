# Migration report template

Write `.specfold/migration/report.md` after validation passes.

```markdown
# Migration report: <repository name>

## Result

- Branch: specfold/migration (from <default branch> at <base commit>)
- Kit: <kit_version> from <ref, or the template folder>
- Concepts written: <n> (<n> Decision, <n> Convention, ...), all draft
- Seeded from code: <n> of the cap <cap>
- Checks: specfold validate, specfold index --check and openspec validate --strict pass
- Always loaded: <n> tokens (estimate from specfold stats); budgets exceeded: <none, or each with its tokens and limit>

## Needs human verification

<!-- Every concept written, one line each; a human runs `specfold verify <path>` after reading it. -->
- knowledge/decisions/x.md (from docs/adr/0001-x.md)

## Install conflicts

none   <!-- or each conflict and what the user decided -->

## Migration candidates

<!-- Specs of other tools that stay as they are for now. -->
none

## Rationale comments in code

none   <!-- or path:line, one per comment -->

## Left out

<!-- Sources marked ignore or keep, and seed candidates beyond the cap. -->

## Open questions

none
```
