package dev.emctable;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The transmutation table screen. Everything is painted with filled rectangles and text, apart
 * from the player inventory, which uses the vanilla strip so it looks normal.
 */
public class EmcTableScreen extends AbstractContainerScreen<EmcTableMenu> {

    private static final int PANEL_W = 230;
    private static final int PANEL_H = 242;

    private static final Identifier INVENTORY_STRIP =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int STRIP_X = (PANEL_W - 176) / 2;
    private static final int STRIP_Y = 146;
    private static final int STRIP_V = 126;
    private static final int STRIP_H = 96;

    private static final int PANEL = 0xFF23202E;
    private static final int PANEL_LIGHT = 0xFF302B40;
    private static final int BORDER = 0xFF121016;
    private static final int EDGE_HI = 0xFF4E4668;

    private static final int LIST_X = 52;
    private static final int LIST_Y = 34;
    private static final int LIST_W = 170;
    private static final int ROW_H = 14;
    private static final int VISIBLE_ROWS = 6;
    private static final int LIST_H = VISIBLE_ROWS * ROW_H;

    private static final int ROW_BG = 0xFF1D1A26;
    private static final int ROW_BG_ALT = 0xFF221E2C;
    private static final int ROW_HOVER = 0xFF3C3560;
    private static final int ROW_POOR = 0xFF2A1C1C;

    private static final int TEXT = 0xFFE8E4F2;
    private static final int TEXT_DIM = 0xFF968EAE;
    private static final int TEXT_EMC = 0xFF9BE7FF;
    private static final int TEXT_GOLD = 0xFFE6C34A;
    private static final int TEXT_RED = 0xFFE07F7F;

    private static final int SLOT_BG = 0xFF15131C;
    private static final int SLOT_EDGE = 0xFF0A090E;

    private int scroll = 0;
    private int hoveredRow = -1;

    public EmcTableScreen(EmcTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_W, PANEL_H);
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelX = STRIP_X + 8;
        this.inventoryLabelY = STRIP_Y + 4;
    }

    private List<Payloads.Known> known() {
        return this.menu.clientKnown();
    }

    private int maxScroll() {
        return Math.max(0, known().size() - VISIBLE_ROWS);
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, EDGE_HI);
        g.fill(x, y, x + 1, y + h, EDGE_HI);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    private static void slotBox(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        g.fill(x, y, x + 16, y + 16, SLOT_BG);
    }

    /** Draws text at 3/4 size, for hints that would otherwise overflow. */
    private void smallText(GuiGraphicsExtractor g, Component text, int x, int y, int colour) {
        float scale = 0.75F;
        g.pose().pushMatrix();
        g.pose().scale(scale, scale);
        g.text(this.font, text, Math.round(x / scale), Math.round(y / scale), colour, false);
        g.pose().popMatrix();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        scroll = Math.min(scroll, maxScroll());
        int left = this.leftPos;
        int top = this.topPos;

        panel(g, left, top, PANEL_W, STRIP_Y, PANEL);
        g.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_STRIP, left + STRIP_X, top + STRIP_Y,
                0.0F, (float) STRIP_V, 176, STRIP_H, 256, 256);

        // input panel on the left
        panel(g, left + 8, top + 76, 36, 62, PANEL_LIGHT);
        slotBox(g, left + EmcTableMenu.INPUT_SLOT_X, top + EmcTableMenu.INPUT_SLOT_Y);

        // the list of learnt items
        g.fill(left + LIST_X - 1, top + LIST_Y - 1, left + LIST_X + LIST_W + 1, top + LIST_Y + LIST_H + 1, BORDER);

        List<Payloads.Known> entries = known();
        long balance = this.menu.clientBalance();
        hoveredRow = -1;
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scroll + i;
            int rowX = left + LIST_X;
            int rowY = top + LIST_Y + i * ROW_H;
            boolean hovering = mouseX >= rowX && mouseX < rowX + LIST_W
                    && mouseY >= rowY && mouseY < rowY + ROW_H;

            if (index >= entries.size()) {
                g.fill(rowX, rowY, rowX + LIST_W, rowY + ROW_H, i % 2 == 0 ? ROW_BG : ROW_BG_ALT);
                continue;
            }
            if (hovering) {
                hoveredRow = index;
            }
            Payloads.Known entry = entries.get(index);
            boolean affordable = balance >= entry.value();
            int background = hovering ? ROW_HOVER
                    : affordable ? (i % 2 == 0 ? ROW_BG : ROW_BG_ALT) : ROW_POOR;
            g.fill(rowX, rowY, rowX + LIST_W, rowY + ROW_H, background);

            g.text(this.font, Component.literal(entry.name()), rowX + 4, rowY + 3,
                    affordable ? TEXT : TEXT_RED, false);
            String cost = String.valueOf(entry.value());
            int costWidth = this.font.width(cost);
            g.text(this.font, Component.literal(cost), rowX + LIST_W - costWidth - 4, rowY + 3,
                    affordable ? TEXT_EMC : TEXT_RED, false);
        }

        if (maxScroll() > 0) {
            int trackX = left + LIST_X + LIST_W + 2;
            g.fill(trackX, top + LIST_Y, trackX + 4, top + LIST_Y + LIST_H, BORDER);
            int knobH = Math.max(12, LIST_H * VISIBLE_ROWS / Math.max(1, entries.size()));
            int knobY = top + LIST_Y + (LIST_H - knobH) * scroll / maxScroll();
            g.fill(trackX, knobY, trackX + 4, knobY + knobH, EDGE_HI);
        }

        // player inventory slots come from the vanilla strip, so nothing to draw for them
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_GOLD, false);

        String balance = this.menu.clientBalance() + " EMC";
        int width = this.font.width(balance);
        g.text(this.font, Component.literal(balance), PANEL_W - width - 10, this.titleLabelY, TEXT_EMC, false);

        smallText(g, Component.literal("Feed items"), 11, 80, TEXT_DIM);
        smallText(g, Component.literal("to learn"), 11, 89, TEXT_DIM);
        smallText(g, Component.literal(known().size() + " known"), LIST_X, LIST_Y - 10, TEXT_DIM);

        int detailY = LIST_Y + LIST_H + 5;
        if (hoveredRow >= 0 && hoveredRow < known().size()) {
            Payloads.Known entry = known().get(hoveredRow);
            long balanceNow = this.menu.clientBalance();
            long affordable = entry.value() > 0 ? balanceNow / entry.value() : 0;
            smallText(g, Component.literal(entry.name() + "  -  " + entry.value() + " EMC each"),
                    LIST_X, detailY, TEXT);
            smallText(g, Component.literal("You can afford " + affordable),
                    LIST_X, detailY + 9, affordable > 0 ? TEXT_EMC : TEXT_RED);
            smallText(g, Component.literal("Left-click: one  |  Shift-click: a stack"),
                    LIST_X, detailY + 18, TEXT_DIM);
        } else {
            smallText(g, Component.literal("Put items in the slot to bank their EMC and learn them."),
                    LIST_X, detailY, TEXT_DIM);
            smallText(g, Component.literal("Hover a learnt item to withdraw it."),
                    LIST_X, detailY + 9, TEXT_DIM);
        }

        g.text(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int left = this.leftPos;
        int top = this.topPos;

        if (mouseX >= left + LIST_X && mouseX < left + LIST_X + LIST_W
                && mouseY >= top + LIST_Y && mouseY < top + LIST_Y + LIST_H) {
            int row = (int) ((mouseY - (top + LIST_Y)) / ROW_H) + scroll;
            if (row >= 0 && row < known().size()) {
                Payloads.Known entry = known().get(row);
                int count = event.hasShiftDown() ? 64 : 1;
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new Payloads.Withdraw(entry.id(), count));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
