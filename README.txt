Packs by Kian - Fabric Mod fuer Minecraft 1.21.11

BAUEN
1. JDK 21 installieren.
2. Auf https://fabricmc.net/develop das Template fuer 1.21.11 ansehen und die Versionen
   in gradle.properties (yarn, loader, fabric api, loom) ggf. anpassen.
3. Gradle-Wrapper erzeugen (einmalig: "gradle wrapper"), dann "./gradlew build".
4. Die fertige .jar liegt in build/libs/ -> in den mods-Ordner kopieren.

BENUTZEN
- Packs in den resourcepacks-Ordner legen (zip oder Ordner).
- Im Spiel Taste K -> links Kategorie waehlen, rechts Pack-Karte anklicken -> Anwenden.
- Neue Kategorien: Categories.java (Regex auf Dateipfade im Pack).
