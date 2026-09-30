package dev.emctable;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class EmcTableClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModRegistry.EMC_TABLE_MENU, EmcTableScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(Payloads.TableData.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().player != null
                            && context.client().player.containerMenu instanceof EmcTableMenu menu) {
                        menu.setClientData(payload.balance(), payload.known());
                    }
                }));
    }
}
