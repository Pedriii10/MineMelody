package com.pedro.musicplaylist.core;

import java.util.regex.Pattern;

/**
 * Minecraft guarda los nombres como "Artista - Título" en un solo texto.
 * Para el banner los queremos separados.
 */
public record SongTitle(String artist, String title) {

	/** Un guion CON espacios alrededor: así "ballad-of-the-cats" no se corta. */
	private static final Pattern SEPARATOR = Pattern.compile("\\s+-\\s+");

	public static SongTitle parse(String full, String fallbackArtist) {
		String[] parts = SEPARATOR.split(full.strip(), 2);
		if (parts.length < 2 || parts[0].isBlank()) {
			return new SongTitle(fallbackArtist, full.strip());
		}
		return new SongTitle(parts[0].strip(), parts[1].strip());
	}
}
