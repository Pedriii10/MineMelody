package com.pedro.minemelody.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Todas tus playlists y cuál está activa (la que editás y la que suena por defecto).
 * <p>
 * Reglas que garantiza esta clase (y que testeamos):
 * <ul>
 *   <li>siempre hay al menos una playlist</li>
 *   <li>los nombres no se repiten (sin importar mayúsculas)</li>
 *   <li>como mucho una playlist por dimensión</li>
 * </ul>
 */
public final class PlaylistLibrary {

	public static final int MAX_NAME_LENGTH = 32;

	private String active;
	private List<Playlist> playlists = new ArrayList<>();

	/** Para Gson. Usá {@link #withDefault(String)} en tu código. */
	public PlaylistLibrary() {
	}

	public static PlaylistLibrary withDefault(String name) {
		PlaylistLibrary library = new PlaylistLibrary();
		library.playlists.add(new Playlist(name));
		library.active = name;
		return library;
	}

	public List<Playlist> playlists() {
		return Collections.unmodifiableList(playlists);
	}

	public Playlist active() {
		return find(active).orElse(playlists.getFirst());
	}

	public Optional<Playlist> find(String name) {
		if (name == null) return Optional.empty();
		return playlists.stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst();
	}

	public void setActive(String name) {
		find(name).ifPresent(p -> active = p.name());
	}

	/** Crea una playlist nueva (con nombre único) y la deja activa. */
	public Playlist create(String baseName) {
		Playlist playlist = new Playlist(uniqueName(baseName));
		playlists.add(playlist);
		active = playlist.name();
		return playlist;
	}

	/** Borra la activa. No deja borrar la última: siempre tiene que quedar una. */
	public boolean deleteActive() {
		if (playlists.size() <= 1) return false;
		int index = playlists.indexOf(active());
		playlists.remove(index);
		active = playlists.get(Math.max(0, index - 1)).name();
		return true;
	}

	public boolean renameActive(String rawName) {
		String name = rawName.strip();
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) return false;

		Playlist current = active();
		boolean takenByAnother = playlists.stream()
				.anyMatch(p -> p != current && p.name().equalsIgnoreCase(name));
		if (takenByAnother) return false;

		current.rename(name);
		active = name;
		return true;
	}

	/** +1 = siguiente, -1 = anterior, dando la vuelta. */
	public void cycle(int direction) {
		int index = playlists.indexOf(active());
		int next = Math.floorMod(index + direction, playlists.size());
		active = playlists.get(next).name();
	}

	/** Ata la activa a una dimensión (o la desata con null). Si otra estaba atada ahí, se desata. */
	public void bindActiveTo(String dimension) {
		Playlist current = active();
		if (dimension != null) {
			playlists.stream()
					.filter(p -> p != current && dimension.equals(p.dimension()))
					.forEach(p -> p.bindTo(null));
		}
		current.bindTo(dimension);
	}

	/** La que tiene que sonar en esta dimensión: la atada (si tiene canciones) o la activa. */
	public Playlist effective(String dimension) {
		return playlists.stream()
				.filter(p -> dimension != null && dimension.equals(p.dimension()) && !p.tracks().isEmpty())
				.findFirst()
				.orElseGet(this::active);
	}

	/** Deja la biblioteca en un estado válido aunque venga de un JSON roto o editado a mano. */
	public void repair(String defaultName) {
		if (playlists == null) playlists = new ArrayList<>();
		playlists.removeIf(Objects::isNull);

		Set<String> seenNames = new HashSet<>();
		Set<String> seenDimensions = new HashSet<>();
		for (Playlist playlist : playlists) {
			playlist.repair(defaultName);
			while (!seenNames.add(playlist.name().toLowerCase(Locale.ROOT))) {
				playlist.rename(playlist.name() + "*");
			}
			if (playlist.dimension() != null && !seenDimensions.add(playlist.dimension())) {
				playlist.bindTo(null);
			}
		}
		if (playlists.isEmpty()) playlists.add(new Playlist(defaultName));
		if (find(active).isEmpty()) active = playlists.getFirst().name();
	}

	private String uniqueName(String baseName) {
		if (find(baseName).isEmpty()) return baseName;
		int number = 2;
		while (find(baseName + " " + number).isPresent()) number++;
		return baseName + " " + number;
	}
}
