package com.pedro.minemelody.track;

import com.pedro.minemelody.mixin.WeighedSoundEventsAccessor;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Descubre TODAS las canciones que hay cargadas: las del juego, las de los discos y,
 * de regalo, las que agreguen otros mods o resource packs.
 * <p>
 * Nada hardcodeado: leemos el registro de sonidos de Minecraft (lo que viene de los sounds.json).
 */
public final class TrackCatalog {

	private Map<Identifier, Track> tracks = Map.of();

	/** Vuelve a leer los sonidos. Barato (unos cientos de eventos), así que se puede llamar seguido. */
	public void refresh(SoundManager soundManager) {
		Map<Identifier, Track> found = new LinkedHashMap<>();

		for (Identifier eventId : sortedMusicEvents(soundManager.getAvailableSounds())) {
			WeighedSoundEvents event = soundManager.getSoundEvent(eventId);
			if (event == null) continue;

			for (Weighted<Sound> entry : ((WeighedSoundEventsAccessor) event).minemelody$getList()) {
				// Las entradas pueden ser un Sound (un archivo) o una referencia a OTRO evento
				// (ej: music.creative incluye music.game). Las referencias las ignoramos:
				// ese otro evento ya lo recorremos por su cuenta.
				if (entry instanceof Sound sound && !isEmpty(sound)) {
					found.putIfAbsent(sound.getLocation(),
							new Track(sound.getLocation(), eventId, sound, TrackGroup.of(sound.getLocation(), eventId)));
				}
			}
		}

		this.tracks = found;
	}

	public Optional<Track> get(String id) {
		Identifier identifier = Identifier.tryParse(id);
		return identifier == null ? Optional.empty() : Optional.ofNullable(tracks.get(identifier));
	}

	/** Todas las canciones, ordenadas por grupo y título. */
	public List<Track> all() {
		return tracks.values().stream()
				.sorted(Comparator.comparing(Track::group).thenComparing(track -> track.title().getString()))
				.toList();
	}

	public boolean isEmpty() {
		return tracks.isEmpty();
	}

	/**
	 * Eventos de música: "music.*" (juego, biomas, menú...) y "music_disc.*" (discos).
	 * Los de discos van primero para que, si un archivo aparece en los dos, quede como disco.
	 */
	private static List<Identifier> sortedMusicEvents(Collection<Identifier> all) {
		return all.stream()
				.filter(id -> id.getPath().startsWith("music.") || Track.isDiscEvent(id))
				.sorted(Comparator.comparing((Identifier id) -> !Track.isDiscEvent(id)).thenComparing(Identifier::toString))
				.toList();
	}

	private static boolean isEmpty(Sound sound) {
		return sound == SoundManager.EMPTY_SOUND || sound == SoundManager.INTENTIONALLY_EMPTY_SOUND;
	}
}
