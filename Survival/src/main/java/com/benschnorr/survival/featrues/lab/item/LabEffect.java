package com.benschnorr.survival.featrues.lab.item;

/** An effect cut into Kokain. The declaration order is the stored order, so equal sets are equal data. */
public enum LabEffect {
    NIGHT_VISION("Nachtsicht"),
    JUMP_BOOST("Sprungkraft"),
    FIRE_RESISTANCE("Feuerresistenz"),
    REGENERATION("Regeneration"),
    STRENGTH("Stärke");

    private final String label;

    LabEffect(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
