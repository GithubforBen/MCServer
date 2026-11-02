## Delta

```yaml
ops:
  - op: create
    path: capabilities/shop-buyback.md
    type: Capability
    summary: Warum Händler ankaufen können und warum das aus der Teamkasse statt aus neuen Bits kommt.
  - op: create
    path: decisions/shop-buyback-paid-by-team.md
    type: Decision
    summary: Rückkauf zahlt die Teamkasse über den bestätigten Weg, auch das eigene Team darf verkaufen.
  - op: update
    path: domain/bits.md
    type: Term
    summary: Bestätigte Abbuchungen haben eine Frist, danach verwirft der Launcher sie.
  - op: update
    path: architecture/survival.md
    type: Component
    summary: Shops kaufen jetzt optional an.
empty_reason: ""
```

## Notes

Concept text is German; identifiers stay English. The decision records the accepted team account risk and the rejected unconfirmed payment.
