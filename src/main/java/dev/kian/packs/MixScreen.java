package dev.kian.packs;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Packs-Mixer: links Kategorien, rechts ein Raster mit allen Packs als Karten.
 * Klick auf eine Karte waehlt das Pack fuer die Kategorie, "Anwenden" baut den Mix.
 */
public class MixScreen extends Screen {
    private static final int ACCENT = 0xFF5EEAD4;
    private static final int[] DOTS = {
        0xFFC084FC, 0xFFFB923C, 0xFF60A5FA, 0xFFF87171, 0xFFFACC15,
        0xFF4ADE80, 0xFFF472B6, 0xFF22D3EE, 0xFFA3A3A3
    };

    private static final int CARD_W = 68, CARD_H = 78, GAP = 6;
    private static final int HDR = 26, FOOT = 18, PAD = 8, PREVIEW = 36;

    private final Screen parent;
    private final List<String> packs = new ArrayList<>();
    private final Map<String, Integer> idx = new HashMap<>();
    private Map<String, Integer> applied = new HashMap<>();
    private final PreviewCache previews = new PreviewCache();
    private final Config cfg = Config.load();

    private int selCat = 0;
    private int scroll = 0;
    private String status = "";

    // Layout
    private int px, py, pw, ph, sideW, gridX, gridY, gridW, gridH, cols, visRows, gridOff;

    public MixScreen(Screen parent) {
        super(Text.literal("Packs by Kian"));
        this.parent = parent;
    }

    private Category cat() { return Categories.ALL.get(selCat); }

    private boolean dirty() { return !applied.equals(idx); }

    private int maxScroll() {
        int totalRows = (packs.size() + cols - 1) / cols;
        return Math.max(0, totalRows - visRows);
    }

    private int cardX(int c) { return gridX + gridOff + c * (CARD_W + GAP); }
    private int cardY(int r) { return gridY + r * (CARD_H + GAP); }

    private void computeLayout() {
        pw = Math.min(width - 16, 460);
        ph = Math.min(height - 56, 280);
        px = (width - pw) / 2;
        py = (height - (ph + 26)) / 2;
        sideW = Math.min(112, pw / 3);
        gridX = px + sideW + PAD;
        gridY = py + HDR + PAD;
        gridW = pw - sideW - PAD * 2;
        gridH = ph - HDR - FOOT - PAD * 2;
        cols = Math.max(1, (gridW + GAP) / (CARD_W + GAP));
        visRows = Math.max(1, (gridH + GAP) / (CARD_H + GAP));
        gridOff = Math.max(0, (gridW - (cols * (CARD_W + GAP) - GAP)) / 2);
    }

    @Override
    protected void init() {
        if (packs.isEmpty()) {
            packs.add(null); // null = Standard (nicht mischen)
            packs.addAll(Mixer.listPacks(client.getResourcePackDir()));
            for (Category c : Categories.ALL)
                idx.put(c.id(), Math.max(0, packs.indexOf(cfg.selection.get(c.id()))));
            applied = new HashMap<>(idx);
        }
        computeLayout();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        // Kategorie-Leiste (Klickflaechen, gezeichnet wird in render)
        int rowH = sideRowH();
        for (int i = 0; i < Categories.ALL.size(); i++) {
            final int ci = i;
            addSelectableChild(ButtonWidget.builder(Text.empty(), b -> {
                selCat = ci;
                scroll = 0;
                clearAndInit();
            }).dimensions(px + 4, py + HDR + 6 + i * rowH, sideW - 8, rowH - 2).build());
        }

        // Karten (nur sichtbare Zeilen)
        Category cat = cat();
        for (int r = 0; r < visRows; r++) {
            for (int c = 0; c < cols; c++) {
                final int index = (scroll + r) * cols + c;
                if (index >= packs.size()) break;
                addSelectableChild(ButtonWidget.builder(Text.empty(), b -> { idx.put(cat.id(), index); status = ""; })
                    .dimensions(cardX(c), cardY(r), CARD_W, CARD_H).build());
            }
        }

        int by = py + ph + 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("Anwenden"), b -> apply())
            .dimensions(px + pw - 206, by, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), b -> finish())
            .dimensions(px + pw - 102, by, 102, 20).build());
    }

    private int sideRowH() {
        int avail = ph - HDR - 12;
        return Math.max(14, Math.min(22, avail / Math.max(1, Categories.ALL.size())));
    }

    private boolean apply() {
        cfg.selection.clear();
        for (Category c : Categories.ALL) {
            String p = packs.get(idx.get(c.id()));
            if (p != null) cfg.selection.put(c.id(), p);
        }
        cfg.save();
        try {
            Mixer.build(client.getResourcePackDir(), cfg.selection);
            Mixer.enableAndReload(client);
            applied = new HashMap<>(idx);
            status = "Mix angewendet!";
            return true;
        } catch (Exception e) {
            status = "Fehler: " + e.getMessage();
            return false;
        }
    }

    private void finish() {
        if (dirty() && !apply()) return;
        close();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx >= gridX && mx <= gridX + gridW && my >= gridY && my <= gridY + gridH && maxScroll() > 0) {
            int ns = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(v)));
            if (ns != scroll) {
                scroll = ns;
                clearAndInit();
            }
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    // ---------- Zeichnen ----------

    private static void border(DrawContext ctx, int x1, int y1, int x2, int y2, int t, int color) {
        ctx.fill(x1, y1, x2, y1 + t, color);
        ctx.fill(x1, y2 - t, x2, y2, color);
        ctx.fill(x1, y1, x1 + t, y2, color);
        ctx.fill(x2 - t, y1, x2, y2, color);
    }

    private static String cleanName(String p) {
        if (p == null) return "Standard";
        return p.toLowerCase().endsWith(".zip") ? p.substring(0, p.length() - 4) : p;
    }

    private void drawPreview(DrawContext ctx, Category cat, String pack, int x, int y, int size) {
        ctx.fill(x, y, x + size, y + size, 0xFF0B0B10);
        if (pack == null) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("-"), x + size / 2, y + size / 2 - 4, 0xFF777788);
            return;
        }
        PreviewCache.Entry e = previews.get(client.getResourcePackDir(), pack, cat.preview());
        if (e == null) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("?"), x + size / 2, y + size / 2 - 4, 0xFFFF6B6B);
            return;
        }
        // Animierte Texturen: nur erster Frame (quadratisch)
        int srcW = e.w();
        int srcH = (e.h() > e.w() && e.w() > 0) ? e.w() : e.h();
        int inner = size - 6;
        float scale = Math.min((float) inner / srcW, (float) inner / srcH);
        int dw = Math.max(1, Math.round(srcW * scale));
        int dh = Math.max(1, Math.round(srcH * scale));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id(),
            x + (size - dw) / 2, y + (size - dh) / 2, 0f, 0f, dw, dh, srcW, srcH, e.w(), e.h());
    }

    private void drawCard(DrawContext ctx, Category cat, int index, int x, int y, boolean hover, boolean active) {
        int bg = active ? 0xFF16282E : (hover ? 0xFF252534 : 0xFF1B1B26);
        ctx.fill(x, y, x + CARD_W, y + CARD_H, bg);
        border(ctx, x, y, x + CARD_W, y + CARD_H, active ? 2 : 1,
            active ? ACCENT : (hover ? 0xFF4A4A60 : 0xFF2E2E3A));

        String pack = packs.get(index);
        drawPreview(ctx, cat, pack, x + (CARD_W - PREVIEW) / 2, y + 7, PREVIEW);

        String name = textRenderer.trimToWidth(cleanName(pack), CARD_W - 8);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name), x + CARD_W / 2, y + 7 + PREVIEW + 7,
            active ? 0xFFFFFFFF : 0xFFC8C8D4);

        if (active) {
            int pillW = 36, pillH = 11;
            int pxl = x + (CARD_W - pillW) / 2, pyl = y + CARD_H - pillH - 6;
            ctx.fill(pxl, pyl, pxl + pillW, pyl + pillH, ACCENT);
            ctx.drawText(textRenderer, Text.literal("AKTIV"), pxl + (pillW - textRenderer.getWidth("AKTIV")) / 2,
                pyl + 2, 0xFF0B0B10, false);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // Hintergrund + Buttons unten

        Category cat = cat();

        // Panel
        ctx.fill(px, py, px + pw, py + ph, 0xF0101018);
        border(ctx, px, py, px + pw, py + ph, 1, 0xFF2A2A38);

        // Kopfzeile
        ctx.fill(px + 1, py + 1, px + pw - 1, py + HDR, 0xFF181824);
        ctx.fill(px + 1, py + HDR, px + pw - 1, py + HDR + 1, ACCENT);
        String title = "PACKS BY KIAN";
        ctx.drawTextWithShadow(textRenderer, Text.literal(title), px + 10, py + 9, ACCENT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("/  " + cat.name()),
            px + 10 + textRenderer.getWidth(title) + 6, py + 9, 0xFFB0B0C0);
        String count = (packs.size() - 1) + " Packs";
        ctx.drawTextWithShadow(textRenderer, Text.literal(count),
            px + pw - 10 - textRenderer.getWidth(count), py + 9, 0xFF8888A0);

        // Seitenleiste
        ctx.fill(px + 1, py + HDR + 1, px + sideW, py + ph - 1, 0xFF0C0C13);
        int rowH = sideRowH();
        for (int i = 0; i < Categories.ALL.size(); i++) {
            Category c = Categories.ALL.get(i);
            int x1 = px + 4, y1 = py + HDR + 6 + i * rowH, x2 = px + sideW - 4, y2 = y1 + rowH - 2;
            boolean sel = i == selCat;
            boolean hov = mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
            if (sel) {
                ctx.fill(x1, y1, x2, y2, 0xFF1F1F2E);
                ctx.fill(x1, y1, x1 + 2, y2, ACCENT);
            } else if (hov) {
                ctx.fill(x1, y1, x2, y2, 0xFF171722);
            }
            int cy = y1 + (rowH - 2) / 2;
            ctx.fill(x1 + 7, cy - 3, x1 + 13, cy + 3, DOTS[i % DOTS.length]);
            String nm = textRenderer.trimToWidth(c.name(), sideW - 8 - 34);
            ctx.drawTextWithShadow(textRenderer, Text.literal(nm), x1 + 19, cy - 4, sel ? 0xFFFFFFFF : 0xFFA0A0B4);
            Integer chosen = idx.get(c.id());
            if (chosen != null && chosen != 0)
                ctx.fill(x2 - 8, cy - 2, x2 - 4, cy + 2, ACCENT); // Punkt = aus Pack gemischt
        }

        // Karten
        String hoverName = null;
        Integer current = idx.get(cat.id());
        for (int r = 0; r < visRows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = (scroll + r) * cols + c;
                if (index >= packs.size()) break;
                int x = cardX(c), y = cardY(r);
                boolean hov = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
                if (hov) hoverName = packs.get(index) == null ? "Standard (nicht mischen)" : packs.get(index);
                drawCard(ctx, cat, index, x, y, hov, current != null && current == index);
            }
        }

        if (packs.size() == 1)
            ctx.drawTextWithShadow(textRenderer, Text.literal("Keine Packs gefunden. Lege welche in den resourcepacks-Ordner."),
                gridX, gridY + CARD_H + 8, 0xFF8888A0);

        // Scrollleiste
        if (maxScroll() > 0) {
            int bx = px + pw - 5, trackH = gridH;
            ctx.fill(bx, gridY, bx + 2, gridY + trackH, 0xFF1F1F2E);
            int thumbH = Math.max(12, trackH * visRows / ((packs.size() + cols - 1) / cols));
            int thumbY = gridY + (trackH - thumbH) * scroll / maxScroll();
            ctx.fill(bx, thumbY, bx + 2, thumbY + thumbH, ACCENT);
        }

        // Fusszeile
        int fy = py + ph - FOOT + 4;
        ctx.fill(px + sideW, py + ph - FOOT, px + pw - 1, py + ph - FOOT + 1, 0xFF22222E);
        if (!status.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(status), gridX, fy, 0xFF55FF88);
        } else if (hoverName != null) {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth(hoverName, gridW)), gridX, fy, 0xFFB0B0C0);
        } else {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(dirty() ? "Nicht angewendet - klicke auf Anwenden" : "Klicke ein Pack, um es zu waehlen"),
                gridX, fy, dirty() ? 0xFFFACC15 : 0xFF6E6E82);
        }
    }

    @Override
    public void removed() { previews.clear(); }

    @Override
    public void close() { client.setScreen(parent); }
}
