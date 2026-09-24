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
import java.util.HashMap;
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

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();

    public record RealTier(String tier, String gamemode) {
    }

    private record Rankings(Map<String, RealTier> byMode, RealTier highest) {
    }

    private static final class Entry {
        volatile Rankings result;
        volatile boolean loading = true;
        volatile long expiresAt = Long.MAX_VALUE;
    }

    private RealTierCache() {
    }

    public static RealTier get(UUID uuid, TierList list, String gamemode) {
        if (uuid == null || list == null || uuid.version() != 4) {
            // Offline-mode / NPC UUIDs (version 3/2) can never be ranked.
            return null;
        }
        String key = list.id + ":" + uuid;
        long now = System.currentTimeMillis();
        Entry entry = CACHE.get(key);
        if (entry == null || (!entry.loading && now > entry.expiresAt)) {
            Entry fresh = new Entry();
            if (entry != null) fresh.result = entry.result; // keep showing stale data while refreshing
            CACHE.put(key, fresh);
            fetch(fresh, uuid, list);
            entry = fresh;
        }
        return pick(entry.result, list, gamemode);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static RealTier pick(Rankings rankings, TierList list, String gamemode) {
        if (rankings == null) return null;
        if (gamemode == null || gamemode.isEmpty() || gamemode.equalsIgnoreCase("highest")) {
            return rankings.highest();
        }
        TierList.Mode mode = list.getMode(gamemode);
        return mode == null ? null : rankings.byMode().get(mode.key());
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
                    // Player isn't on this tier list.
                    entry.result = null;
                    entry.expiresAt = now + TTL_MS;
                    return;
                }
                if (code != 200) {
                    // 429 rate limit / 5xx: back off and try again later.
                    entry.expiresAt = now + RETRY_MS;
                    return;
                }
                entry.result = parse(response.body(), list);
                entry.expiresAt = now + TTL_MS;
            } catch (Exception e) {
                TierSpoofer.LOGGER.debug("[TierSpoofer] Failed to parse {} profile for {}", list.displayName, uuid, e);
                entry.expiresAt = now + RETRY_MS;
            } finally {
                entry.loading = false;
            }
        });
    }

    private static Rankings parse(String body, TierList list) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject()) return null;
        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("rankings") || !obj.get("rankings").isJsonObject()) return null;
        JsonObject rankings = obj.getAsJsonObject("rankings");

        Map<String, RealTier> all = new HashMap<>();
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
            all.put(e.getKey(), rt);

            // Lower tier number = better; HT beats LT; active beats retired.
            int score = -(tier * 4 + pos * 2 + (retired ? 1 : 0));
            if (score > bestScore) {
                bestScore = score;
                best = rt;
            }
        }
        return best == null ? null : new Rankings(Map.copyOf(all), best);
    }
}
