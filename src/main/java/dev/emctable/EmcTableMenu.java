package dev.emctable;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The transmutation table menu. One real slot: anything put in it is consumed for EMC and learnt.
 * The list of learnt items is drawn by EmcTableScreen from synced data, and withdrawals come back
 * as Payloads.Withdraw packets.
 */
public class EmcTableMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOT_X = 26;
    public static final int INPUT_SLOT_Y = 100;
    public static final int INVENTORY_X = 35;
    public static final int INVENTORY_Y = 160;
    public static final int HOTBAR_Y = 218;

    private static final int INPUT_SLOT = 0;
    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final BlockPos pos;
    private final SimpleContainer container = new SimpleContainer(1);

    /** Client side only, as last sent by the server. */
    private long clientBalance;
    private List<Payloads.Known> clientKnown = List.of();

    /** Client constructor, used by the registered MenuType. */
    public EmcTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, BlockPos.ZERO);
    }

    public EmcTableMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModRegistry.EMC_TABLE_MENU, containerId);
        this.pos = pos;

        addSlot(new InputSlot(container, INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    /** Only takes items that actually have an EMC value. */
    private static class InputSlot extends Slot {
        InputSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EmcValues.hasValue(stack);
        }
    }

    // ---------- client side ----------

    public long clientBalance() {
        return clientBalance;
    }

    public List<Payloads.Known> clientKnown() {
        return clientKnown;
    }

    public void setClientData(long balance, List<Payloads.Known> known) {
        this.clientBalance = balance;
        this.clientKnown = known;
    }

    // ---------- server side ----------

    /**
     * Sends the player's balance and everything they have learnt. Must not be called from the
     * constructor: the client has no screen yet and would drop the packet.
     */
    public void syncToClient(ServerPlayer player) {
        EmcData data = EmcData.get(player.level().getServer().overworld());
        List<Payloads.Known> known = new ArrayList<>();
        for (String id : data.learned(player)) {
            long value = EmcValues.valueOf(id);
            if (value <= 0) {
                continue; // item no longer exists or lost its value
            }
            Identifier identifier = Identifier.tryParse(id);
            Item item = identifier == null ? null : BuiltInRegistries.ITEM.getValue(identifier);
            String name = item == null ? id : item.getName().getString();
            known.add(new Payloads.Known(id, name, value));
        }
        known.sort((a, b) -> {
            int byValue = Long.compare(a.value(), b.value());
            return byValue != 0 ? byValue : a.name().compareToIgnoreCase(b.name());
        });
        ServerPlayNetworking.send(player, new Payloads.TableData(data.balance(player), known));
    }

    /** Eats whatever is in the input slot: banks its EMC and learns it. */
    private void consumeInput(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = container.getItem(INPUT_SLOT);
        if (stack.isEmpty()) {
            return;
        }
        long each = EmcValues.valueOf(stack.getItem());
        if (each <= 0) {
            return;
        }

        EmcData data = EmcData.get(serverPlayer.level().getServer().overworld());
        String id = EmcValues.key(stack.getItem());
        long gained = each * stack.getCount();
        data.add(serverPlayer, gained);
        boolean isNew = data.learn(serverPlayer, id);
        container.setItem(INPUT_SLOT, ItemStack.EMPTY);

        serverPlayer.sendSystemMessage(Component.literal(
                (isNew ? "Learnt " : "Absorbed ") + stack.getCount() + "x " + stack.getHoverName().getString()
                        + "  (+" + gained + " EMC)")
                .withStyle(isNew ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        serverPlayer.level().playSound(null, pos,
                isNew ? SoundEvents.PLAYER_LEVELUP : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 0.6F, 1.4F);
        syncToClient(serverPlayer);
        broadcastChanges();
    }

    /** Hands over an item the player has learnt, charging its EMC. */
    public void withdraw(ServerPlayer player, Payloads.Withdraw request) {
        EmcData data = EmcData.get(player.level().getServer().overworld());
        if (!data.knows(player, request.id())) {
            return;
        }
        Identifier identifier = Identifier.tryParse(request.id());
        Item item = identifier == null ? null : BuiltInRegistries.ITEM.getValue(identifier);
        if (item == null) {
            return;
        }
        long each = EmcValues.valueOf(request.id());
        if (each <= 0) {
            return;
        }

        int wanted = Math.max(1, Math.min(request.count(), item.getDefaultMaxStackSize()));
        long affordable = data.balance(player) / each;
        int count = (int) Math.min(wanted, affordable);
        if (count <= 0) {
            player.sendSystemMessage(Component.literal(
                    "Not enough EMC: need " + each + ", have " + data.balance(player))
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (!data.spend(player, each * count)) {
            return;
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(item, count));
        player.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.0F);
        syncToClient(player);
        broadcastChanges();
    }

    // ---------- plumbing ----------

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        super.clicked(slotId, button, input, player);
        consumeInput(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == INPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (EmcValues.hasValue(stack)) {
            if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        consumeInput(player);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return pos.equals(BlockPos.ZERO)
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player player) {
        ItemStack stack = container.getItem(INPUT_SLOT);
        if (!stack.isEmpty()) {
            container.setItem(INPUT_SLOT, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(stack);
        }
        super.removed(player);
    }
}
