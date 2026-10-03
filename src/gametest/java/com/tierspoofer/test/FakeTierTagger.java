package com.tierspoofer.test;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Adds a tag the way TierTagger does ("<icon>HT3 | name", prepended in a ModifyReturnValue). */
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
