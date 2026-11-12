package com.pedro.musicplaylist.mixin;

import com.pedro.musicplaylist.MusicPlaylistClient;
import com.pedro.musicplaylist.playback.PlaylistPlayer;
import com.pedro.musicplaylist.playback.PlaylistSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * El MusicManager es el "DJ" de Minecraft. Cada tick:
 * <ol>
 *   <li>pregunta qué música corresponde ahora ({@code getSituationalMusic})</li>
 *   <li>si la canción actual terminó, espera {@code nextSongDelay} ticks (¡varios minutos!)</li>
 *   <li>cuando la cuenta llega a 0, llama a {@code startPlaying(music)}, que elige una AL AZAR</li>
 * </ol>
 * Nosotros nos metemos en tres puntos para que, cuando la playlist está activa, mande ella.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

	// @Shadow = "este campo ya existe en MusicManager, dejame usarlo como si fuera mío".
	@Shadow @Final private Minecraft minecraft;
	@Shadow private SoundInstance currentMusic;
	@Shadow private int nextSongDelay;
	@Shadow private boolean toastShown;

	/**
	 * Punto 1: reemplazar la elección aleatoria por la de nuestra playlist.
	 * Si la playlist no aplica (desactivada, en el menú, créditos...), no hacemos nada
	 * y el método original sigue como siempre.
	 */
	@Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
	private void musicplaylist$playFromPlaylist(Music music, CallbackInfo ci) {
		PlaylistSoundInstance next = MusicPlaylistClient.player().createNext(minecraft, music);
		if (next == null) return;

		ci.cancel(); // el startPlaying original NO se ejecuta

		// Lo mismo que hace vanilla, pero con nuestra canción.
		currentMusic = next;
		SoundEngine.PlayResult result = minecraft.getSoundManager().play(next);
		if (result == SoundEngine.PlayResult.STARTED) {
			if (MusicPlaylistClient.config().showBanner) {
				MusicPlaylistClient.banner().show(next.track()); // nuestro banner reemplaza al aviso vanilla
			} else {
				minecraft.gui.toastManager().showNowPlayingToast();
			}
			toastShown = true;
		} else if (result == SoundEngine.PlayResult.STARTED_SILENTLY) {
			toastShown = false; // volumen de música en 0: el aviso sale cuando lo subas
		}
		nextSongDelay = Integer.MAX_VALUE;
	}

	/**
	 * Banner también para la música NORMAL del juego (playlist apagada). Si llegamos al final
	 * de startPlaying es porque no lo cancelamos: sonó una canción vanilla elegida al azar.
	 */
	@Inject(method = "startPlaying", at = @At("RETURN"))
	private void musicplaylist$bannerForVanillaMusic(Music music, CallbackInfo ci) {
		if (currentMusic == null || currentMusic instanceof PlaylistSoundInstance) return;
		if (currentMusic.getSound() == null || !minecraft.getSoundManager().isActive(currentMusic)) return;

		MusicPlaylistClient.player()
				.findTrack(minecraft, currentMusic.getSound().getLocation())
				.ifPresent(MusicPlaylistClient.banner()::show);
	}

	/**
	 * Punto 2: los silencios. Corre al principio de cada tick, ANTES que la lógica vanilla.
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void musicplaylist$controlGaps(CallbackInfo ci) {
		if (currentMusic == null) return;
		PlaylistPlayer player = MusicPlaylistClient.player();

		if (currentMusic instanceof PlaylistSoundInstance) {
			boolean finished = !minecraft.getSoundManager().isActive(currentMusic);

			// Al cambiar de dimensión (ej: entrar al End) vanilla corta la música y vuelve a esperar
			// minutos. Si la playlist sigue mandando, cortamos nosotros y respetamos NUESTRA pausa.
			Music situational = minecraft.getSituationalMusic();
			boolean vanillaWouldReplace = situational != null && situational.replaceCurrentMusic()
					&& !situational.sound().value().location().equals(currentMusic.getIdentifier())
					&& player.shouldHandle(minecraft, situational);

			if (finished || vanillaWouldReplace) {
				minecraft.getSoundManager().stop(currentMusic);
				currentMusic = null;
				nextSongDelay = player.gapTicks();
			}
		} else if (player.shouldHandle(minecraft, minecraft.getSituationalMusic())) {
			// Está sonando música vanilla (ej: la del menú, que sigue al entrar al mundo)
			// pero la playlist está activa: la cortamos y arranca la playlist.
			minecraft.getSoundManager().stop(currentMusic);
			currentMusic = null;
			nextSongDelay = player.gapTicks();
		}
	}

	/**
	 * Punto 3: el aviso de "Sonando ahora". Vanilla arma la clave de traducción a partir del
	 * archivo ("records/cat" -> "records.cat"), que para los discos NO existe. Le pasamos la buena.
	 */
	@Inject(method = "getCurrentMusicTranslationKey", at = @At("HEAD"), cancellable = true)
	private void musicplaylist$titleForToast(CallbackInfoReturnable<String> cir) {
		if (currentMusic instanceof PlaylistSoundInstance instance) {
			cir.setReturnValue(instance.track().titleKey());
		}
	}
}
