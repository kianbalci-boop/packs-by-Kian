package dev.kian.packs;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PacksByKianClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("packsbykian", "main"));
        KeyBinding open = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.packsbykian.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, cat));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.wasPressed()) {
                client.setScreen(new MixScreen(client.currentScreen));
            }
        });
    }
}
