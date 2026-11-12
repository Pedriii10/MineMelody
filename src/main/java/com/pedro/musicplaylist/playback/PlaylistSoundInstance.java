package com.pedro.musicplaylist.playback;

import com.pedro.musicplaylist.track.Track;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * Un sonido de música que SIEMPRE reproduce la canción que elegimos.
 * <p>
 * El truco: en vanilla, {@code getOrResolve} busca el evento (ej: "music.game") y elige
 * una canción AL AZAR. Nosotros dejamos que haga eso y después pisamos el resultado
 * con nuestro Track. Mismo camino que la música normal, pero con la canción que vos quieras.
 * <p>
 * Los parámetros (volumen 1, sin atenuación, relativo al jugador) son los mismos que usa
 * {@code SimpleSoundInstance.forMusic}, así que suena exactamente igual que la música de siempre.
 */
public final class PlaylistSoundInstance extends AbstractSoundInstance {

	private final Track track;

	/**
	 * @param situationalEvent el evento de música que pide el juego en este momento. Lo usamos como
	 *                         identificador para que el MusicManager no crea que "hay que cambiar
	 *                         de música" y nos corte la canción a mitad.
	 */
	public PlaylistSoundInstance(Identifier situationalEvent, Track track) {
		super(situationalEvent, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
		this.track = track;
		this.volume = 1.0F;
		this.pitch = 1.0F;
		this.looping = false;
		this.delay = 0;
		this.attenuation = Attenuation.NONE;
		this.relative = true;
	}

	@Override
	public WeighedSoundEvents getOrResolve(SoundManager soundManager) {
		WeighedSoundEvents event = super.getOrResolve(soundManager);
		if (event != null) {
			this.sound = track.sound();
		}
		return event;
	}

	public Track track() {
		return track;
	}
}
