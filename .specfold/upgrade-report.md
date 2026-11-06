# Specfold upgrade: 0.3.1 to 0.4.0

## Conflicts (0)

None.

## Changes

| Path | Action |
| --- | --- |
| `scripts/specfold.mjs` | replace |
| `AGENTS.md` | replace |

## Kept

None.

## Migrations

None.

## Follow-up commands

| Command | Exit | Last line |
| --- | --- | --- |
| `openspec update` | 0 | Add them with `openspec config profile`. |
| `specfold agents sync` | 0 | agents sync: links are up to date, 6 kept |
| `specfold permissions sync` | 0 | permissions sync: 15 changes |
| `specfold hooks install` | 2 | specfold: core.hooksPath is already set to '/home/ben/IdeaProjects/MCServer/.specfold/hooks' and no known hook manager was found; move its hooks to hooks.extra in specfold.yaml and unset it, or add specfold to your hook manager |
| `specfold index` | 0 | 11 indexes, 0 written: 0 errors, 0 warnings |
| `specfold validate` | 0 | 51 files checked: 0 errors, 0 warnings |
| `specfold gate --base 35f593cf533ec16c9db0a012b4cb0386db776f89 --local` | 0 | gate: passed, 0 warnings |

## Upgrade notes

### 0.4.0

This release adds `permissions` to `specfold.yaml` and `specfold permissions sync`, which renders it into Claude Code's permission rules in `.claude/settings.json`. The upgrade's follow-up commands ran the sync, so the rules are already in place. With no `permissions` key, every action counts as `ask`.

After the upgrade PR is merged, tell the user:

- Claude Code now asks before `git push` (including force pushes, deleting remote branches and pushing tags) and before `gh pr create`, `gh pr merge` and `gh pr close`, including the `gh api` calls that merge or close a PR. `AGENTS.md` asks every other tool to do the same.
- To change that, they set a level per action under `permissions` in `specfold.yaml`: `allow`, `ask` or `deny`, for `push`, `force-push`, `delete-remote-branch`, `push-tags`, `open-pr`, `merge-pr` and `close-pr`. `force-push`, `delete-remote-branch` and `push-tags` must be at least as strict as `push`. Then run `node scripts/specfold.mjs permissions sync` and commit `.claude/settings.json`.

Ask the user whether to add the `permissions` block with the default `ask` levels to `specfold.yaml` so it is visible, recommending yes. The rules are best effort: a push written another way, such as `git -C . push`, isn't matched. `node scripts/specfold.mjs doctor` reports drift as check D8.
