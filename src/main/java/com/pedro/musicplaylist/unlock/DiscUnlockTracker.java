package com.pedro.musicplaylist.unlock;

import com.pedro.musicplaylist.config.PlaylistConfig;
import com.pedro.musicplaylist.track.Track;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Set;

/**
 * Regla de survival: un disco se "desbloquea" la primera vez que lo tenés en el inventario,
 * y queda desbloqueado PARA ESE MUNDO aunque después lo pongas en un cofre.
 * <p>
 * Todo pasa en el cliente: miramos tu inventario, no le preguntamos nada al servidor.
 * Por eso el mod funciona aunque entres a un server que no lo tiene instalado.
 */
public final class DiscUnlockTracker {

	private static final int SCAN_EVERY_TICKS = 20;

	private final PlaylistConfig config;
	private int ticksUntilScan = 0;

	public DiscUnlockTracker(PlaylistConfig config) {
		this.config = config;
	}

	/** Se llama una vez por tick. Escanea una vez por segundo: de sobra para esto. */
	public void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || --ticksUntilScan > 0) return;
		ticksUntilScan = SCAN_EVERY_TICKS;

		Set<String> unlocked = config.unlockedDiscsFor(worldKey(minecraft));
		boolean changed = false;

		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) continue;

			var song = JukeboxSong.fromStack(stack);
			if (song.isEmpty()) continue;

			String discEvent = song.get().value().soundEvent().value().location().toString();
			if (unlocked.add(discEvent)) {
				changed = true;
				player.sendSystemMessage(Component.translatable("musicplaylist.disc_unlocked",
						song.get().value().description()).withStyle(ChatFormatting.GOLD));
			}
		}

		if (changed) config.save();
	}

	/** ¿Esta canción puede sonar ahora mismo? */
	public boolean isAvailable(Minecraft minecraft, Track track) {
		if (!track.isDisc() || !config.discsRequireUnlock) return true;

		LocalPlayer player = minecraft.player;
		if (player == null || player.isCreative() || player.isSpectator()) return true;

		return config.unlockedDiscsFor(worldKey(minecraft)).contains(track.eventId().toString());
	}

	/**
	 * Identifica el mundo: la CARPETA del mundo en singleplayer (el nombre visible se puede
	 * repetir, "Nuevo mundo" x5...), y la IP en multiplayer.
	 */
	public static String worldKey(Minecraft minecraft) {
		IntegratedServer local = minecraft.getSingleplayerServer();
		if (local != null) {
			Path folder = local.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
			return "singleplayer:" + (folder != null ? folder : local.getWorldData().getLevelName());
		}
		ServerData server = minecraft.getCurrentServer();
		if (server != null) {
			return "server:" + server.ip;
		}
		return "unknown";
	}
}
