package com.tierspoofer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class SkinCache {
    private static final Logger LOGGER = LoggerFactory.getLogger("TierSpoofer-SkinCache");
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    // the newer lookup first, the old one if that fails (both get rate limited now and then)
    private static final String[] UUID_APIS = {
            "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
            "https://api.mojang.com/users/profiles/minecraft/"
    };
    private static final String PROFILE_API = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final Map<UUID, Identifier> skinTextureCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Identifier> capeTextureCache = new ConcurrentHashMap<>();
    private static final Map<String, UUID> usernameToUuidCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> loadingState = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> slimModel = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> retryAfter = new ConcurrentHashMap<>();
    private static final long RETRY_MS = 5 * 60 * 1000;
    private static final long NAME_RETRY_MS = 60 * 1000;
    // name -> when we may look it up again (Long.MAX_VALUE while a lookup is running)
    private static final Map<String, Long> nameRetryAt = new ConcurrentHashMap<>();

    public static Identifier getCachedSkin(UUID uuid) {
        return skinTextureCache.get(uuid);
    }

    public static Identifier getCachedCape(UUID uuid) {
        return capeTextureCache.get(uuid);
    }

    public static boolean hasCachedSkin(UUID uuid) {
        return skinTextureCache.containsKey(uuid);
    }

    public static boolean isSlim(UUID uuid) {
        return slimModel.getOrDefault(uuid, false);
    }

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    public static CompletableFuture<Void> fetchSkinByUsername(String username) {
        if (username == null || !VALID_NAME.matcher(username).matches()) {
            return CompletableFuture.completedFuture(null);
        }
        UUID cachedUuid = usernameToUuidCache.get(username.toLowerCase());
        if (cachedUuid != null) {
            if (skinTextureCache.containsKey(cachedUuid)) {
                return CompletableFuture.completedFuture(null);
            }
            return fetchSkinByUUID(cachedUuid);
        }
        String key = username.toLowerCase();
        long now = System.currentTimeMillis();
        if (now < nameRetryAt.getOrDefault(key, 0L)) {
            return CompletableFuture.completedFuture(null);
        }
        nameRetryAt.put(key, Long.MAX_VALUE);
        return fetchUUID(username, 0).handle((uuid, error) -> {
            if (uuid == null) {
                LOGGER.warn("No skin for \"{}\": {}", username, error != null ? error.toString()
                        : "no Minecraft account with that name, or Mojang didn't answer (trying again in a minute)");
                // unknown name, rate limit or network error: don't ask again for a bit
                nameRetryAt.put(key, System.currentTimeMillis() + NAME_RETRY_MS);
                return null;
            }
            nameRetryAt.remove(key);
            usernameToUuidCache.put(key, uuid);
            return uuid;
        }).thenCompose(uuid -> uuid == null ? CompletableFuture.completedFuture(null) : fetchSkinByUUID(uuid));
    }

    public static CompletableFuture<Void> fetchSkinByUUID(UUID uuid) {
        if (skinTextureCache.containsKey(uuid) || loadingState.getOrDefault(uuid, false)
                || System.currentTimeMillis() < retryAfter.getOrDefault(uuid, 0L)) {
            return CompletableFuture.completedFuture(null);
        }
        loadingState.put(uuid, true);
        retryAfter.put(uuid, System.currentTimeMillis() + RETRY_MS);
        return fetchTextures(uuid).thenCompose(textures -> {
            if (textures == null) {
                loadingState.put(uuid, false);
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<Void> skinFuture = CompletableFuture.completedFuture(null);
            slimModel.put(uuid, textures.slim());
            if (textures.skinUrl() != null) {
                skinFuture = downloadAndRegister(uuid, textures.skinUrl(), "skin", skinTextureCache);
            }
            CompletableFuture<Void> capeFuture = CompletableFuture.completedFuture(null);
            if (textures.capeUrl() != null) {
                capeFuture = downloadAndRegister(uuid, textures.capeUrl(), "cape", capeTextureCache);
            }
            return CompletableFuture.allOf(skinFuture, capeFuture)
                    .thenRun(() -> loadingState.put(uuid, false));
        }).exceptionally(e -> {
            LOGGER.error("Error fetching skin/cape for {}", uuid, e);
            loadingState.put(uuid, false);
            return null;
        });
    }

    private static CompletableFuture<Void> downloadAndRegister(
            UUID uuid, String url, String type, Map<UUID, Identifier> cache) {
        // Mojang hands out http:// links, some networks block plain http
        String secureUrl = url.startsWith("http://textures.minecraft.net/") ? "https://" + url.substring(7) : url;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(secureUrl)).timeout(REQUEST_TIMEOUT).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenAccept(response -> {
            if (response.statusCode() != 200) {
                LOGGER.warn("Downloading {} for {} failed: HTTP {}", type, uuid, response.statusCode());
            } else {
                byte[] data = response.body();
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
                        if ("skin".equals(type) && image.getHeight() == 32) {
                            image = upgradeLegacySkin(image);
                        }
                        Identifier id = Identifier.of("tierspoofer",
                                type + "/" + uuid.toString().replace("-", ""));
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                        mc.getTextureManager().registerTexture(id, texture);
                        cache.put(uuid, id);
                    } catch (Exception e) {
                        LOGGER.error("Failed to register {} texture for {}", type, uuid, e);
                    }
                });
            }
        });
    }

    private static CompletableFuture<TextureUrls> fetchTextures(UUID uuid) {
        String url = PROFILE_API + uuid.toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(REQUEST_TIMEOUT).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                LOGGER.warn("Skin profile for {} failed: HTTP {}", uuid, response.statusCode());
            } else {
                try {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray props = json.getAsJsonArray("properties");
                    for (JsonElement prop : props) {
                        if (!"textures".equals(prop.getAsJsonObject().get("name").getAsString())) continue;
                        String val = prop.getAsJsonObject().get("value").getAsString();
                        String decoded = new String(Base64.getDecoder().decode(val));
                        JsonObject textures = JsonParser.parseString(decoded)
                                .getAsJsonObject().getAsJsonObject("textures");
                        String skin = textures.has("SKIN")
                                ? textures.getAsJsonObject("SKIN").get("url").getAsString() : null;
                        String cape = textures.has("CAPE")
                                ? textures.getAsJsonObject("CAPE").get("url").getAsString() : null;
                        boolean slim = false;
                        if (textures.has("SKIN") && textures.getAsJsonObject("SKIN").has("metadata")) {
                            JsonObject meta = textures.getAsJsonObject("SKIN").getAsJsonObject("metadata");
                            slim = meta.has("model") && "slim".equals(meta.get("model").getAsString());
                        }
                        return new TextureUrls(skin, cape, slim);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse textures for {}", uuid, e);
                }
            }
            return null;
        });
    }

    private static CompletableFuture<UUID> fetchUUID(String username, int api) {
        if (api >= UUID_APIS.length) return CompletableFuture.completedFuture(null);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(UUID_APIS[api] + username))
                .timeout(REQUEST_TIMEOUT).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).handle((response, error) -> {
            if (error == null && response.statusCode() == 200) {
                try {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    return parseUUID(json.get("id").getAsString());
                } catch (Exception ignored) {
                }
            }
            return null;
        }).thenCompose(uuid -> uuid != null ? CompletableFuture.completedFuture(uuid) : fetchUUID(username, api + 1));
    }

    private static UUID parseUUID(String id) {
        if (id.length() == 32) {
            return UUID.fromString(id.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"));
        }
        return UUID.fromString(id);
    }

    public static UUID getUuidForUsername(String username) {
        return usernameToUuidCache.get(username.toLowerCase());
    }

    public static void prefetchSkin(String username) {
        if (username != null && !username.isEmpty()) {
            fetchSkinByUsername(username);
        }
    }

    private static NativeImage upgradeLegacySkin(NativeImage legacy) {
        NativeImage image = new NativeImage(64, 64, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                image.setColorArgb(x, y, legacy.getColorArgb(x, y));
            }
        }
        legacy.close();
        // copy right leg/arm into the empty left ones, mirrored
        mirrorLimb(image, 0, 16, 16, 48);
        mirrorLimb(image, 40, 16, 32, 48);
        return image;
    }

    private static void mirrorLimb(NativeImage img, int sx, int sy, int dx, int dy) {
        mirrorRect(img, sx + 4, sy, dx + 4, dy, 4, 4);
        mirrorRect(img, sx + 8, sy, dx + 8, dy, 4, 4);
        mirrorRect(img, sx, sy + 4, dx + 8, dy + 4, 4, 12);
        mirrorRect(img, sx + 4, sy + 4, dx + 4, dy + 4, 4, 12);
        mirrorRect(img, sx + 8, sy + 4, dx, dy + 4, 4, 12);
        mirrorRect(img, sx + 12, sy + 4, dx + 12, dy + 4, 4, 12);
    }

    private static void mirrorRect(NativeImage img, int sx, int sy, int dx, int dy, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setColorArgb(dx + (w - 1 - x), dy + y, img.getColorArgb(sx + x, sy + y));
            }
        }
    }

    private record TextureUrls(String skinUrl, String capeUrl, boolean slim) {}
}
