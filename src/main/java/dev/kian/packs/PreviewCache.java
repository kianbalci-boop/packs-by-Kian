package dev.kian.packs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Laedt Vorschau-Texturen aus den Packs und registriert sie als GUI-Texturen. */
public class PreviewCache {
    public record Entry(Identifier id, int w, int h) {}

    private static final Entry MISSING = new Entry(null, 0, 0);
    private final Map<String, Entry> cache = new HashMap<>();
    private int counter = 0;

    public Entry get(Path packsDir, String packName, String texturePath) {
        String key = packName + "|" + texturePath;
        Entry e = cache.get(key);
        if (e != null) return e == MISSING ? null : e;

        Entry loaded = MISSING;
        try {
            byte[] data = Mixer.read(packsDir.resolve(packName), texturePath);
            if (data != null) {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(data));
                Identifier id = Identifier.of("packsbykian", "preview/" + (counter++));
                MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(id, new NativeImageBackedTexture("packsbykian_preview", img));
                loaded = new Entry(id, img.getWidth(), img.getHeight());
            }
        } catch (Exception ignored) {}
        cache.put(key, loaded);
        return loaded == MISSING ? null : loaded;
    }

    public void clear() {
        for (Entry e : cache.values())
            if (e != MISSING) MinecraftClient.getInstance().getTextureManager().destroyTexture(e.id());
        cache.clear();
    }
}
