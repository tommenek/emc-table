package dev.emctable;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Client side EMC values: shows each item's value in its tooltip, with a hotkey to switch that
 * on and off. The on/off choice is remembered in config/emctable.properties.
 */
public final class EmcTooltips {

    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("emctable.properties");

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(EmcTableMod.id("emctable"));
    private static KeyMapping toggleKey;

    /** As last sent by the server. */
    private static Map<String, Long> values = Map.of();
    private static boolean enabled = true;

    private EmcTooltips() {
    }

    public static void init() {
        load();
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.emctable.toggle_tooltips", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

        // in-game, with no screen open
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                toggle();
            }
        });
        // inside inventories, where tooltips are actually seen; key mappings don't fire there on their own
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenKeyboardEvents.afterKeyPress(screen).register((current, event) -> {
                    if (!(current.getFocused() instanceof EditBox) && toggleKey.matches(event)) {
                        toggle();
                    }
                }));

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (enabled) {
                addLines(stack, lines);
            }
        });
    }

    public static void setValues(Map<String, Long> newValues) {
        values = newValues;
    }

    public static long valueOf(ItemStack stack) {
        return stack.isEmpty() ? 0 : values.getOrDefault(EmcValues.key(stack.getItem()), 0L);
    }

    public static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static void addLines(ItemStack stack, List<Component> lines) {
        long each = valueOf(stack);
        if (each <= 0) {
            return;
        }
        lines.add(Component.literal("EMC: " + format(each)).withStyle(ChatFormatting.YELLOW));
        if (stack.getCount() > 1) {
            lines.add(Component.literal("Stack EMC: " + format(each * stack.getCount()))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static void toggle() {
        enabled = !enabled;
        save();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal("EMC tooltips " + (enabled ? "on" : "off"))
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

    private static void load() {
        if (!Files.exists(CONFIG)) {
            return;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG)) {
            props.load(reader);
            enabled = Boolean.parseBoolean(props.getProperty("show_tooltips", "true"));
        } catch (IOException e) {
            EmcTableMod.LOGGER.warn("EMC Table: could not read {}", CONFIG, e);
        }
    }

    private static void save() {
        Properties props = new Properties();
        props.setProperty("show_tooltips", String.valueOf(enabled));
        try (Writer writer = Files.newBufferedWriter(CONFIG)) {
            props.store(writer, "EMC Table client settings");
        } catch (IOException e) {
            EmcTableMod.LOGGER.warn("EMC Table: could not save {}", CONFIG, e);
        }
    }
}
