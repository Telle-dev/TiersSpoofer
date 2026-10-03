// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.model;

// Where a tier list's tag goes: "HT1 | name", "name | HT1" or not at all.
public enum TagSide {
    LEFT("Left"),
    RIGHT("Right"),
    OFF("Off");

    public final String label;

    TagSide(String label) {
        this.label = label;
    }

    public TagSide next() {
        TagSide[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
