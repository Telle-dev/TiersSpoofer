// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer.test;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FakeTierTagger {
    public static volatile boolean enabled;

    private FakeTierTagger() {
    }

    public static Text prepend(Text original) {
        if (!enabled || original == null) return original;
        return Text.literal("").append(Text.literal("HT3").styled(s -> s.withColor(0xFF0000)))
                .append(Text.literal(" | ").formatted(Formatting.GRAY)).append(original);
    }
}
