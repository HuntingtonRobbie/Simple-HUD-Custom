package com.soradgaming.simplehudenhanced.hud;

/** The HUD elements this mod draws that can be moved in the HUD editor. */
public enum HudElement {
    STATUS_TEXT("Info text"),
    SYSTEM_TIME("Clock"),
    EQUIPMENT("Equipment"),
    MOVEMENT("Movement text"),
    PAPER_DOLL("Paper doll"),
    EFFECTS("Effects");

    private final String label;

    HudElement(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
