package com.pedro.minemelody.playback;

import com.pedro.minemelody.config.PlaylistConfig;
import com.pedro.minemelody.core.Playlist;
import com.pedro.minemelody.core.PlaylistQueue;
import com.pedro.minemelody.mixin.MusicManagerAccessor;
import com.pedro.minemelody.track.Track;
import com.pedro.minemelody.track.TrackCatalog;
import com.pedro.minemelody.unlock.DiscUnlockTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Pega las piezas: la config (qué querés), el catálogo (qué existe), los discos
 * (qué tenés desbloqueado) y la cola (qué sigue). Lo usa el mixin del MusicManager.
 */
public final class PlaylistPlayer {

	/**
	 * Música que NO pisamos: los créditos y la pelea con el dragón son momentos especiales
	 * y el juego cambia la música a propósito. Respetamos eso.
	 */
	private static final Set<Identifier> SPECIAL_EVENTS = Set.of(
			Identifier.withDefaultNamespace("music.credits"),
			Identifier.withDefaultNamespace("music.dragon"));

	private final PlaylistConfig config;
	private final TrackCatalog catalog;
	private final DiscUnlockTracker unlocks;
	private final Random random = new Random();
	/** Una cola por playlist: cada una recuerda por dónde iba (y su bolsa de shuffle). */
	private final Map<Playlist, PlaylistQueue> queues = new IdentityHashMap<>();

	/** Canción pedida con "Escuchar": suena la próxima, aunque la playlist esté apagada. */
	private String requestedTrack;

	public PlaylistPlayer(PlaylistConfig config, TrackCatalog catalog, DiscUnlockTracker unlocks) {
		this.config = config;
		this.catalog = catalog;
		this.unlocks = unlocks;
	}

	/**
	 * ¿Le toca a la playlist manejar esta música? Sólo dentro de un mundo (en el menú principal
	 * suena la música de siempre) y sólo si hay al menos una canción que se pueda reproducir.
	 */
	public boolean shouldHandle(Minecraft minecraft, Music situational) {
		if (!config.enabled || minecraft.level == null || minecraft.player == null || isSpecial(situational)) {
			return false;
		}
		ensureCatalog(minecraft);
		return effectivePlaylist(minecraft).tracks().stream()
				.map(catalog::get)
				.flatMap(Optional::stream)
				.anyMatch(track -> unlocks.isAvailable(minecraft, track));
	}

	/** Crea el sonido de la próxima canción, o null si hay que dejar que suene vanilla. */
	public PlaylistSoundInstance createNext(Minecraft minecraft, Music situational) {
		if (requestedTrack != null && !isSpecial(situational)) {
			String requested = requestedTrack;
			requestedTrack = null;
			catalog.refresh(minecraft.getSoundManager());
			Optional<Track> track = catalog.get(requested);
			if (track.isPresent()) {
				return new PlaylistSoundInstance(situational.sound().value().location(), track.get(), null);
			}
		}

		if (!shouldHandle(minecraft, situational)) return null;

		catalog.refresh(minecraft.getSoundManager()); // por si cambiaron los resource packs
		Playlist playlist = effectivePlaylist(minecraft);
		return queueFor(playlist).next(playlist.tracks(), id -> isPlayable(minecraft, id), config.mode)
				.flatMap(catalog::get)
				.map(track -> new PlaylistSoundInstance(situational.sound().value().location(), track, playlist))
				.orElse(null);
	}

	/**
	 * Pre-escucha: corta lo que suena y pone ESTA canción. Funciona en el menú principal y con
	 * la playlist apagada; al terminar, sigue la playlist (o la música normal si está apagada).
	 */
	public void playNow(Minecraft minecraft, Track track) {
		requestedTrack = track.id().toString();
		skip(minecraft);
	}

	/** Busca el Track de un archivo de sonido cualquiera (lo usamos para la música vanilla). */
	public Optional<Track> findTrack(Minecraft minecraft, Identifier soundLocation) {
		ensureCatalog(minecraft);
		return catalog.get(soundLocation.toString());
	}

	/** Corta la canción actual y arranca la siguiente casi de inmediato. */
	public void skip(Minecraft minecraft) {
		MusicManager music = minecraft.getMusicManager();
		music.stopPlaying();
		((MusicManagerAccessor) music).minemelody$setNextSongDelay(1);
	}

	/** La canción de la playlist que está sonando ahora, si hay una. */
	public Optional<PlaylistSoundInstance> currentSound(Minecraft minecraft) {
		MusicManager music = minecraft.getMusicManager();
		if (music == null) return Optional.empty();
		return ((MusicManagerAccessor) music).minemelody$getCurrentMusic() instanceof PlaylistSoundInstance ours
				? Optional.of(ours)
				: Optional.empty();
	}

	/**
	 * La playlist que tiene que sonar AHORA: la atada a tu dimensión (si tiene canciones)
	 * o la activa.
	 */
	public Playlist effectivePlaylist(Minecraft minecraft) {
		return config.library.effective(dimensionId(minecraft));
	}

	/** Se llama cuando cambia una playlist entera, para que su orden arranque de cero. */
	public void resetQueue(Playlist playlist) {
		queues.remove(playlist);
	}

	/**
	 * ¿La canción que suena salió de otra playlist distinta de la que toca ahora? Pasa al cambiar
	 * de playlist activa o al entrar en una dimensión que tiene su propia playlist.
	 */
	public boolean playlistChanged(Minecraft minecraft, PlaylistSoundInstance playing) {
		return config.enabled && playing.playlist() != null && playing.playlist() != effectivePlaylist(minecraft);
	}

	/** Pasa a la siguiente/anterior playlist (tecla rápida) y avisa en pantalla. */
	public void cyclePlaylist(Minecraft minecraft, int direction) {
		config.library.cycle(direction);
		config.save();
		if (minecraft.player != null) {
			minecraft.player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
					"minemelody.overlay.playlist", config.library.active().name()));
		}
	}

	private PlaylistQueue queueFor(Playlist playlist) {
		return queues.computeIfAbsent(playlist, k -> new PlaylistQueue(random));
	}

	public static String dimensionId(Minecraft minecraft) {
		return minecraft.level == null ? null : minecraft.level.dimension().identifier().toString();
	}

	public int gapTicks() {
		return Math.max(1, config.gapSeconds * 20);
	}

	public TrackCatalog catalog() {
		return catalog;
	}

	public DiscUnlockTracker unlocks() {
		return unlocks;
	}

	private boolean isPlayable(Minecraft minecraft, String id) {
		return catalog.get(id).map(track -> unlocks.isAvailable(minecraft, track)).orElse(false);
	}

	private static boolean isSpecial(Music music) {
		return music == null || SPECIAL_EVENTS.contains(music.sound().value().location());
	}

	private void ensureCatalog(Minecraft minecraft) {
		if (catalog.isEmpty()) {
			catalog.refresh(minecraft.getSoundManager());
		}
	}
}
