package com.pedro.minemelody;

import com.pedro.minemelody.config.PlaylistConfig;
import com.pedro.minemelody.gui.PlaylistScreen;
import com.pedro.minemelody.hud.NowPlayingBanner;
import com.pedro.minemelody.playback.PlaylistPlayer;
import com.pedro.minemelody.track.TrackCatalog;
import com.pedro.minemelody.unlock.DiscUnlockTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada. Fabric lo llama una vez al arrancar el juego (ver "entrypoints"
 * en fabric.mod.json). Acá armamos las piezas y las conectamos.
 */
public final class MineMelodyClient implements ClientModInitializer {

	public static final String MOD_ID = "minemelody";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static PlaylistConfig config;
	private static PlaylistPlayer player;
	private static NowPlayingBanner banner;

	private static final KeyMapping.Category KEY_CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	private KeyMapping openScreenKey;
	private KeyMapping skipKey;

	@Override
	public void onInitializeClient() {
		config = PlaylistConfig.load();
		DiscUnlockTracker unlocks = new DiscUnlockTracker(config);
		player = new PlaylistPlayer(config, new TrackCatalog(), unlocks);

		openScreenKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.minemelody.open", InputConstants.KEY_M, KEY_CATEGORY));
		skipKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.minemelody.skip", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY));

		banner = new NowPlayingBanner(config);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "now_playing"), banner);

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		LOGGER.info("MineMelody listo: {} canciones en tu playlist", config.tracks.size());
	}

	private void onClientTick(Minecraft minecraft) {
		unlocks().tick(minecraft);

		while (openScreenKey.consumeClick()) {
			minecraft.gui.setScreen(new PlaylistScreen(minecraft.gui.screen()));
		}
		while (skipKey.consumeClick()) {
			player.skip(minecraft);
		}
	}

	// Acceso global simple. Para un mod de este tamaño alcanza; en algo más grande
	// usaríamos inyección de dependencias en vez de estáticos.
	public static PlaylistConfig config() {
		return config;
	}

	public static PlaylistPlayer player() {
		return player;
	}

	public static NowPlayingBanner banner() {
		return banner;
	}

	private static DiscUnlockTracker unlocks() {
		return player.unlocks();
	}
}
