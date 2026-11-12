package com.pedro.minemelody.core;

/** "185 segundos" -> "3:05", y cuánto de la canción ya sonó (0..1) para la barra. */
public final class TimeFormat {

	private TimeFormat() {
	}

	public static String minutesSeconds(double seconds) {
		long total = (long) Math.max(0, Math.floor(seconds));
		return (total / 60) + ":" + String.format("%02d", total % 60);
	}

	public static float progress(double elapsedSeconds, double totalSeconds) {
		if (totalSeconds <= 0) return 0f;
		return (float) Math.clamp(elapsedSeconds / totalSeconds, 0.0, 1.0);
	}
}
