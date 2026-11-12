package com.pedro.minemelody.core;

/**
 * Cómo se elige la siguiente canción de la playlist.
 */
public enum PlaybackMode {
	/** Una detrás de otra, y al terminar vuelve a empezar. */
	IN_ORDER,
	/** Aleatorio, pero sin repetir hasta haber pasado por todas (como Spotify). */
	SHUFFLE,
	/** Repite la misma canción en bucle. */
	REPEAT_ONE;

	public PlaybackMode cycle() {
		PlaybackMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public String translationKey() {
		return "minemelody.mode." + name().toLowerCase();
	}
}
