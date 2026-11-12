package com.pedro.minemelody.playback;

import com.pedro.minemelody.mixin.MusicManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;

/**
 * Cuánto lleva sonando la canción actual.
 * <p>
 * Contamos TICKS (20 por segundo) en vez de mirar el reloj del sistema: así es fácil no
 * contar el tiempo en que la pausaste con la tecla. OJO: el menú de pausa del juego NO para
 * la música (Minecraft deja sonar MUSIC y UI), así que ahí el reloj sigue corriendo.
 */
public final class TrackClock {

	private static final double TICKS_PER_SECOND = 20.0;

	private SoundInstance tracked;
	private long ticks;

	public void tick(Minecraft minecraft, boolean pausedByUser) {
		SoundInstance current = minecraft.getMusicManager() == null ? null
				: ((MusicManagerAccessor) minecraft.getMusicManager()).minemelody$getCurrentMusic();

		if (current != tracked) { // empezó otra canción (o ninguna)
			tracked = current;
			ticks = 0;
		}
		if (current != null && !pausedByUser && minecraft.getSoundManager().isActive(current)) {
			ticks++;
		}
	}

	public double elapsedSeconds() {
		return ticks / TICKS_PER_SECOND;
	}
}
