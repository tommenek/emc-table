package dev.emctable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A handheld transmutation table: right-click anywhere to open the EMC menu. */
public class TransmutationTabletItem extends Item {

    public TransmutationTabletItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            EmcTableMenu.open(serverPlayer, null, player.getItemInHand(hand).getHoverName());
        }
        return InteractionResult.SUCCESS;
    }
}
