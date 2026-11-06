## Delta

```yaml
ops:
  - op: create
    path: capabilities/lab-brewing.md
    type: Capability
    summary: Warum es eine lange, automatisierbare Produktionskette im Braustand gibt und wie sie Survival unberührt lässt.
  - op: create
    path: capabilities/lab-upgrades.md
    type: Capability
    summary: Warum Vanilla-Blöcke am Stand als Upgrades wirken statt eigener Blöcke oder Items.
  - op: create
    path: capabilities/lab-cutting.md
    type: Capability
    summary: Warum Kokain nach der Herstellung gestreckt und mit Effekten versehen werden kann.
  - op: create
    path: capabilities/lab-consumption.md
    type: Capability
    summary: Warum Konsum über Vanilla gehen darf, aber nicht im Kampf.
  - op: create
    path: decisions/lab-is-an-addon.md
    type: Decision
    summary: Das Labor bleibt ein vanilla-naher Zusatz auf Survival; Razzien und neue Bits wurden verworfen.
  - op: create
    path: decisions/lab-brewing-engine.md
    type: Decision
    summary: Zuckerrohr braut mit eigenem Tick auf leeren Slots, alles andere läuft auf dem Vanilla-Brauen.
  - op: create
    path: domain/purity.md
    type: Term
    summary: Die fünf Reinheitsstufen von Paste und Kokain.
  - op: update
    path: architecture/survival.md
    type: Component
    summary: Survival enthält jetzt das Labor.
empty_reason: ""
```

## Notes

Concept text is German; identifiers stay English. If the fallback to glass bottles is taken, `decisions/lab-brewing-engine.md` records why.
