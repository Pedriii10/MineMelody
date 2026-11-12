package com.pedro.minemelody.playback;

import com.mojang.blaze3d.audio.Channel;
import com.pedro.minemelody.core.VolumeStep;
import com.pedro.minemelody.mixin.MusicManagerAccessor;
import com.pedro.minemelody.mixin.SoundEngineAccessor;
import com.pedro.minemelody.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

/**
 * Teclas rápidas: pausar/reanudar la canción y subir/bajar el volumen de la música.
 * <p>
 * La pausa trabaja a nivel de CANAL de audio (OpenAL): pausamos sólo el canal de la canción
 * actual. Mientras está pausada, el canal sigue "vivo", así que el MusicManager cree que la
 * canción continúa y no arranca otra. Justo lo que queremos.
 */
public final class MusicControls {

	/** La canción que el usuario pausó, o null si no hay nada pausado. */
	private SoundInstance pausedSound;

	public void togglePause(Minecraft minecraft) {
		SoundInstance current = currentMusic(minecraft);
		if (current == null) return;

		if (current == pausedSound) {
			pausedSound = null;
			withChannel(minecraft, current, Channel::unpause);
			overlay(minecraft, Component.translatable("minemelody.overlay.resumed"));
		} else {
			pausedSound = current;
			withChannel(minecraft, current, Channel::pause);
			overlay(minecraft, Component.translatable("minemelody.overlay.paused"));
		}
	}

	/**
	 * Se llama cada tick. Dos casos a cubrir:
	 * <ul>
	 *   <li>la canción cambió (saltaste, cambiaste de mundo...): ya no hay nada pausado</li>
	 *   <li>al cerrar el menú de pausa, Minecraft reanuda TODOS los sonidos, también el nuestro:
	 *       lo volvemos a pausar</li>
	 * </ul>
	 */
	public void tick(Minecraft minecraft) {
		if (pausedSound == null) return;
		if (currentMusic(minecraft) != pausedSound) {
			pausedSound = null;
			return;
		}
		withChannel(minecraft, pausedSound, channel -> {
			if (channel.playing()) channel.pause();
		});
	}

	public boolean isPaused() {
		return pausedSound != null;
	}

	public void changeVolume(Minecraft minecraft, int direction) {
		OptionInstance<Double> music = minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC);
		double volume = VolumeStep.apply(music.get(), direction);
		music.set(volume); // la misma opción que el slider de "Música": se aplica y se ve en ajustes
		minecraft.options.save();
		overlay(minecraft, Component.translatable("minemelody.overlay.volume", VolumeStep.percent(volume)));
	}

	private static SoundInstance currentMusic(Minecraft minecraft) {
		if (minecraft.getMusicManager() == null) return null;
		return ((MusicManagerAccessor) minecraft.getMusicManager()).minemelody$getCurrentMusic();
	}

	/** Ejecuta algo sobre el canal OpenAL del sonido (en el hilo de audio, que es donde se debe). */
	private static void withChannel(Minecraft minecraft, SoundInstance sound, java.util.function.Consumer<Channel> action) {
		var engine = ((SoundManagerAccessor) minecraft.getSoundManager()).minemelody$getSoundEngine();
		ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) engine).minemelody$getInstanceToChannel().get(sound);
		if (handle != null && !handle.isStopped()) {
			handle.execute(action);
		}
	}

	private static void overlay(Minecraft minecraft, Component message) {
		if (minecraft.player != null) {
			minecraft.player.sendOverlayMessage(message);
		}
	}
}
