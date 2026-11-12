package com.pedro.musicplaylist.core;

/**
 * Animación de "entra deslizando, se queda, sale deslizando".
 * Devuelve cuánto se ve el banner (0 = escondido fuera de pantalla, 1 = totalmente visible),
 * y quien dibuja lo convierte en píxeles. Matemática pura: se testea sin abrir el juego.
 */
public record SlideAnimation(long totalMillis, long slideMillis) {

	public float visibility(long elapsedMillis) {
		if (elapsedMillis <= 0 || elapsedMillis >= totalMillis) return 0f;

		long remaining = totalMillis - elapsedMillis;
		float linear = Math.min(1f, Math.min(elapsedMillis, remaining) / (float) slideMillis);
		return easeOutCubic(linear);
	}

	public boolean isFinished(long elapsedMillis) {
		return elapsedMillis >= totalMillis;
	}

	/** Arranca rápido y frena suave: se siente más natural que un movimiento lineal. */
	private static float easeOutCubic(float t) {
		float inverse = 1f - t;
		return 1f - inverse * inverse * inverse;
	}
}
