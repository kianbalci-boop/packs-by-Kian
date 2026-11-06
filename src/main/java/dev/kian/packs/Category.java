package dev.kian.packs;

import java.util.function.Predicate;

/** preview = Pfad der Textur im Pack, die als Vorschau-Icon gezeigt wird. */
public record Category(String id, String name, Predicate<String> filter, String preview) {
    public static Category of(String id, String name, String regex, String preview) {
        return new Category(id, name, p -> p.matches(regex), preview);
    }
}
