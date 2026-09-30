package dev.emctable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click to open the transmutation table. All data is per player, so the block holds none. */
public class EmcTableBlock extends Block {

    public EmcTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new EmcTableMenu(containerId, inventory, pos),
                    this.getName()));
            // the client only has its screen once openMenu has run, so sync after it
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof EmcTableMenu menu) {
                menu.syncToClient(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
