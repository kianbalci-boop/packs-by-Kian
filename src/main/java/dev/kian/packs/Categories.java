package dev.kian.packs;

import java.util.List;

/** Hier kannst du einfach neue Kategorien hinzufuegen (Regex auf den Pfad im Pack + Vorschau-Textur). */
public final class Categories {
    private static final String MC = "assets/minecraft/";

    public static final List<Category> ALL = List.of(
        Category.of("glint", "Glint", MC + "textures/misc/enchanted_glint.*",
            MC + "textures/misc/enchanted_glint_item.png"),
        Category.of("end_crystal", "End Crystal",
            MC + "(textures/entity/end_crystal/.*|textures/item/end_crystal\\..*|items/end_crystal\\.json|models/item/end_crystal\\.json)",
            MC + "textures/item/end_crystal.png"),
        Category.of("ender_pearl", "Ender Pearl",
            MC + "(textures/item/ender_pearl\\..*|items/ender_pearl\\.json|models/item/ender_pearl\\.json)",
            MC + "textures/item/ender_pearl.png"),
        Category.of("swords", "Schwerter",
            MC + "(textures/item|models/item|items)/.*sword.*",
            MC + "textures/item/diamond_sword.png"),
        Category.of("totem", "Totem",
            MC + "(textures/item/totem_of_undying\\..*|items/totem_of_undying\\.json|models/item/totem_of_undying\\.json)",
            MC + "textures/item/totem_of_undying.png"),
        Category.of("fire", "Feuer", MC + "textures/block/(soul_)?fire_[01].*",
            MC + "textures/block/fire_0.png"),
        Category.of("particles", "Partikel", MC + "textures/particle/.*",
            MC + "textures/particle/flame.png"),
        Category.of("gui", "GUI", MC + "textures/gui/.*",
            MC + "textures/gui/sprites/hud/hotbar.png"),
        Category.of("sky", "Sonne/Mond/Himmel", MC + "textures/environment/.*",
            MC + "textures/environment/sun.png")
    );

    private Categories() {}
}
