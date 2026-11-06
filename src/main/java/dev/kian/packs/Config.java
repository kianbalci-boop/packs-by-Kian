package dev.kian.packs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("packsbykian.json"); }

    /** Kategorie-ID -> Pack-Dateiname */
    public Map<String, String> selection = new HashMap<>();

    public static Config load() {
        try {
            if (Files.exists(file())) {
                Config c = GSON.fromJson(Files.readString(file()), Config.class);
                if (c != null && c.selection != null) return c;
            }
        } catch (Exception ignored) {}
        return new Config();
    }

    public void save() {
        try { Files.writeString(file(), GSON.toJson(this)); } catch (Exception ignored) {}
    }
}
