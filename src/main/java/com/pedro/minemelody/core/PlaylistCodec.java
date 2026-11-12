package com.pedro.minemelody.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Convierte una playlist en un texto corto para pasarlo por Discord, y al revés.
 * <pre>
 *   MineMelody:1:music/game/sweden,records/cat,mymod:music/cool_song
 *   └─ marca ─┘ └ versión del formato
 * </pre>
 * La versión es CLAVE: si algún día cambiamos el formato, sabremos leer los textos viejos.
 * Las canciones vanilla van sin "minecraft:" para que el texto sea más corto.
 */
public final class PlaylistCodec {

	private static final String PREFIX = "MineMelody:";
	private static final int VERSION = 1;
	private static final String VANILLA_NAMESPACE = "minecraft:";
	/** Lo que Minecraft acepta en un identificador (más ':' para el namespace). */
	private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	private PlaylistCodec() {
	}

	public static String encode(List<String> trackIds) {
		StringBuilder text = new StringBuilder(PREFIX).append(VERSION).append(':');
		for (int i = 0; i < trackIds.size(); i++) {
			if (i > 0) text.append(',');
			String id = trackIds.get(i);
			text.append(id.startsWith(VANILLA_NAMESPACE) ? id.substring(VANILLA_NAMESPACE.length()) : id);
		}
		return text.toString();
	}

	/** Devuelve la playlist, o vacío si el texto no es una playlist válida de MineMelody. */
	public static Optional<List<String>> decode(String text) {
		String header = PREFIX + VERSION + ":";
		String trimmed = text.strip();
		if (!trimmed.startsWith(header)) {
			return Optional.empty();
		}

		Set<String> ids = new LinkedHashSet<>(); // sin duplicados y manteniendo el orden
		for (String raw : trimmed.substring(header.length()).split(",")) {
			String entry = raw.strip();
			if (entry.isEmpty()) continue;
			String id = entry.contains(":") ? entry : VANILLA_NAMESPACE + entry;
			if (VALID_ID.matcher(id).matches()) {
				ids.add(id);
			}
		}
		return ids.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(ids));
	}
}
