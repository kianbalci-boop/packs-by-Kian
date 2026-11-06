package dev.kian.packs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Mixer {
    public static final String MIX_NAME = "PacksByKian_Mix";
    public static final String MIX_ID = "file/" + MIX_NAME;

    private Mixer() {}

    public static List<String> listPacks(Path dir) {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.equals(MIX_NAME)) return;
                if (Files.isDirectory(p) || n.toLowerCase().endsWith(".zip")) out.add(n);
            });
        } catch (IOException ignored) {}
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static void build(Path packsDir, Map<String, String> selection) throws IOException {
        Path out = packsDir.resolve(MIX_NAME);
        deleteRecursive(out);
        Files.createDirectories(out);
        Files.writeString(out.resolve("pack.mcmeta"),
            "{\"pack\":{\"pack_format\":75,\"description\":\"Mixed with Packs by Kian\"}}");

        for (Category cat : Categories.ALL) {
            String packName = selection.get(cat.id());
            if (packName == null) continue;
            Path src = packsDir.resolve(packName);
            if (Files.isDirectory(src)) copyFromDir(src, cat, out);
            else if (Files.isRegularFile(src)) copyFromZip(src, cat, out);
        }
    }

    private static void copyFromDir(Path root, Category cat, Path out) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (!Files.isRegularFile(p)) continue;
                String rel = root.relativize(p).toString().replace('\\', '/');
                if (cat.filter().test(rel)) {
                    Path target = out.resolve(rel);
                    Files.createDirectories(target.getParent());
                    Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void copyFromZip(Path zip, Category cat, Path out) throws IOException {
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            // Falls das Pack einen Unterordner hat: Prefix anhand von pack.mcmeta finden
            String prefix = "";
            int best = Integer.MAX_VALUE;
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                String n = e.nextElement().getName();
                if (n.endsWith("pack.mcmeta")) {
                    int depth = n.split("/").length;
                    if (depth < best) { best = depth; prefix = n.substring(0, n.length() - "pack.mcmeta".length()); }
                }
            }
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                if (ze.isDirectory() || !ze.getName().startsWith(prefix)) continue;
                String rel = ze.getName().substring(prefix.length());
                if (cat.filter().test(rel)) {
                    Path target = out.resolve(rel);
                    Files.createDirectories(target.getParent());
                    try (InputStream in = zf.getInputStream(ze)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    /** Liest eine einzelne Datei aus einem Pack (zip oder Ordner). Gibt null zurueck, wenn nicht vorhanden. */
    public static byte[] read(Path pack, String rel) {
        try {
            if (Files.isDirectory(pack)) {
                Path f = pack.resolve(rel);
                return Files.isRegularFile(f) ? Files.readAllBytes(f) : null;
            }
            try (ZipFile zf = new ZipFile(pack.toFile())) {
                String prefix = "";
                int best = Integer.MAX_VALUE;
                for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                    String n = e.nextElement().getName();
                    if (n.endsWith("pack.mcmeta")) {
                        int d = n.split("/").length;
                        if (d < best) { best = d; prefix = n.substring(0, n.length() - "pack.mcmeta".length()); }
                    }
                }
                ZipEntry ze = zf.getEntry(prefix + rel);
                if (ze == null) return null;
                try (InputStream in = zf.getInputStream(ze)) { return in.readAllBytes(); }
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void enableAndReload(MinecraftClient client) {
        ResourcePackManager mgr = client.getResourcePackManager();
        mgr.scanPacks();
        List<String> enabled = new ArrayList<>(mgr.getEnabledIds());
        enabled.remove(MIX_ID);
        enabled.add(MIX_ID); // ganz oben = hoechste Prioritaet
        mgr.setEnabledProfiles(enabled);
        client.options.resourcePacks.clear();
        client.options.resourcePacks.addAll(mgr.getEnabledIds());
        client.options.write();
        client.reloadResources();
    }

    private static void deleteRecursive(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (Stream<Path> s = Files.walk(p)) {
            for (Path x : (Iterable<Path>) s.sorted(Comparator.reverseOrder())::iterator) Files.delete(x);
        }
    }
}
