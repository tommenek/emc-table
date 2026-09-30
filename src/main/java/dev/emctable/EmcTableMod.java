package dev.emctable;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmcTableMod implements ModInitializer {

    public static final String MOD_ID = "emctable";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModRegistry.init();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> output.accept(ModRegistry.EMC_TABLE_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(ModRegistry.TRANSMUTATION_TABLET));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(output -> output.accept(ModRegistry.EMC_ORB));

        PayloadTypeRegistry.clientboundPlay().register(Payloads.TableData.TYPE, Payloads.TableData.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payloads.Values.TYPE, Payloads.Values.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Payloads.Withdraw.TYPE, Payloads.Withdraw.CODEC);

        // recipes are loaded by now, so this is when values can be worked out
        ServerLifecycleEvents.SERVER_STARTED.register(EmcValues::compute);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                EmcValues.compute(server);
                // values may have changed, so tooltips need the new ones
                Payloads.Values values = new Payloads.Values(EmcValues.snapshot());
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, values);
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sender.sendPacket(new Payloads.Values(EmcValues.snapshot())));

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Withdraw.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().containerMenu instanceof EmcTableMenu menu) {
                        menu.withdraw(context.player(), payload);
                    }
                }));
    }
}
