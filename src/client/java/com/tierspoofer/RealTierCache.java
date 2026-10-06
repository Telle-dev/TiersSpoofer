// TierSpoofer - Copyright (c) 2026 Tellegram (Telle-dev)
// SPDX-License-Identifier: GPL-3.0-only
// See LICENSE. Modified versions must stay GPL-3.0, keep this notice and credit the original.

package com.tierspoofer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tierspoofer.model.TierList;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public final class RealTierCache {
    private static final long TTL_MS = 10 * 60 * 1000;
    private static final long RETRY_MS = 60 * 1000;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "TierSpoofer-TierFetcher");
                t.setDaemon(true);
                return t;
            }))
            .build();

    // one map per list, so a lookup doesn't have to build a key string every frame
    private static final Map<TierList, Map<UUID, Entry>> CACHE = new EnumMap<>(TierList.class);

    static {
        for (TierList list : TierList.values()) CACHE.put(list, new ConcurrentHashMap<>());
    }

    public record RealTier(String tier, String gamemode) {
    }

    private static final class Entry {
        volatile RealTier result;
        volatile boolean loading = true;
        volatile long expiresAt = Long.MAX_VALUE;
    }

    private RealTierCache() {
    }

    // Best tier the player has on this list, or null (also while it's still loading).
    public static RealTier get(UUID uuid, TierList list) {
        if (uuid == null || list == null || uuid.version() != 4) {
            // offline/npc uuids
            return null;
        }
        Map<UUID, Entry> perList = CACHE.get(list);
        long now = System.currentTimeMillis();
        Entry entry = perList.get(uuid);
        if (entry == null || (!entry.loading && now > entry.expiresAt)) {
            Entry fresh = new Entry();
            if (entry != null) fresh.result = entry.result;
            perList.put(uuid, fresh);
            fetch(fresh, uuid, list);
            entry = fresh;
        }
        return entry.result;
    }

    private static void fetch(Entry entry, UUID uuid, TierList list) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(list.profileUrl(uuid)))
                .header("User-Agent", "TierSpoofer (Fabric mod)")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {
            long now = System.currentTimeMillis();
            try {
                if (error != null) {
                    entry.expiresAt = now + RETRY_MS;
                    return;
                }
                int code = response.statusCode();
                if (code == 404) {
                    entry.result = null;
                    entry.expiresAt = now + TTL_MS;
                    return;
                }
                if (code != 200) {
                    // rate limited or api down, try again later
                    entry.expiresAt = now + RETRY_MS;
                    return;
                }
                entry.result = parse(response.body());
                entry.expiresAt = now + TTL_MS;
            } catch (Exception e) {
                TierSpoofer.LOGGER.debug("[TierSpoofer] Failed to parse {} profile for {}", list.displayName, uuid, e);
                entry.expiresAt = now + RETRY_MS;
            } finally {
                entry.loading = false;
            }
        });
    }

    private static RealTier parse(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject()) return null;
        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("rankings") || !obj.get("rankings").isJsonObject()) return null;
        JsonObject rankings = obj.getAsJsonObject("rankings");

        RealTier best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Map.Entry<String, JsonElement> e : rankings.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject r = e.getValue().getAsJsonObject();
            if (!r.has("tier") || !r.has("pos")) continue;
            int tier = r.get("tier").getAsInt();
            int pos = r.get("pos").getAsInt();
            boolean retired = r.has("retired") && !r.get("retired").isJsonNull() && r.get("retired").getAsBoolean();

            String text = (retired ? "R" : "") + (pos == 0 ? "HT" : "LT") + tier;
            RealTier rt = new RealTier(text, e.getKey());

            // lower tier wins, then HT over LT, then active over retired
            int score = -(tier * 4 + pos * 2 + (retired ? 1 : 0));
            if (score > bestScore) {
                bestScore = score;
                best = rt;
            }
        }
        return best;
    }
}
