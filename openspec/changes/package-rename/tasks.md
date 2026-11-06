# Tasks

## 1. Rename

```yaml
files:
  - pom.xml
  - "*/pom.xml"
  - "*/src/**"
  - install.sh
  - admin-passwort.sh
  - testing/probes/**
depends: []
scenarios: []
agent: implementer
```

- [x] 1.1 Move every package under `de.schnorrenbergers` and `de.hems` to `com.benschnorr` with `git mv`, and rewrite all references by script, including plugin mains, the launcher main class, scripts, probes and the Maven groupId
  - Done when: `./mvnw -B -q install -DskipTests` passes and no `de.hems` or `de.schnorrenbergers` is left outside `openspec/changes/archive/`
- [x] 1.2 The existing check classes still pass under the new names
  - Done when: `LabBrewCheck`, `LabChainCheck`, `SigningRulesCheck`, `BuybackRulesCheck`, `SkinNameCheck` and `InfoTextCheck` exit 0

## 2. Docs

```yaml
files:
  - README.md
  - testing/**
  - Bedwars/PLAN.md
  - openspec/specs/**
depends: [1]
scenarios: []
agent: implementer
```

- [x] 2.1 README, `testing/`, `Bedwars/PLAN.md` and the current specs name the new packages and paths
  - Done when: `openspec validate --all --strict` passes and no old package name is left in these files

## 3. In-game check

```yaml
files:
  - testing/package-rename/**
depends: [2]
scenarios: []
agent: implementer
```

- [x] 3.1 The orchestrator starts the local network: launcher, proxy, Lobby and Survival start with all plugins enabled, a client joins, warps to Survival and opens a shop
  - Done when: the result is recorded in `testing/package-rename/results-2026-11-06.md`

## 4. Knowledge

```yaml
files: [knowledge/**]
depends: [3]
scenarios: []
agent: knowledge-curator
```

- [ ] 4.1 Write the concepts listed in knowledge.md, and update every concept that names an old package or path
  - Done when: `specfold validate` exits 0

## Workflow follow-up

- Archive the change, then run `specfold sync package-rename` and commit the result in the same PR.
