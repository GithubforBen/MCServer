## Wish

"Also update the package name programmatically from de.schnorrenbergers to com.ben-schnorr (dont do all of this via normal write operations to save tokens)."

## Decisions

| # | Question | Answer | Source |
| --- | --- | --- | --- |
| Q1 | Which name? A hyphen is not allowed in a Java package | `com.benschnorr` | user |
| Q2 | When? | After the four feature PRs (buyback, skin, lab, signing) | user |
| Q3 | Which packages? | Both `de.schnorrenbergers` and `de.hems` | user |
| Q4 | Where does `de.hems` go? | Into `com.benschnorr` directly (`de.hems.paper.X` becomes `com.benschnorr.paper.X`); no sub-package collides | user |
| Q5 | Maven groupId? | Also `com.benschnorr` | user |
| Q6 | Archived changes? | Keep their old paths; they record history | delegated |
| Q7 | Rollout? | Network events travel as Java-serialized classes, so launcher and all servers switch together, as a normal update restart does | delegated |

## Out of scope

- Renaming modules, artifactIds, plugin names or Velocity plugin ids.
- Fixing the `featrues` typo in package names.

## Open questions

## Confirmation

confirmed_by: human:Githubforben
confirmed_at: 2026-11-06T19:19:28+01:00
