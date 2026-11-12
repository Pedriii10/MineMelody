package com.pedro.minemelody.core;

/**
 * Sube/baja el volumen de a 10%, "encajando" en la cuadrícula: desde 53% subís a 60%, no a 63%.
 */
public final class VolumeStep {

	private static final int STEPS = 10;

	private VolumeStep() {
	}

	/** @param direction +1 sube, -1 baja */
	public static double apply(double current, int direction) {
		double scaled = current * STEPS;
		// Si ya está justo en un escalón, nos movemos uno; si está entre dos, vamos al próximo en esa dirección.
		double next = direction > 0 ? Math.floor(scaled + 1e-9) + 1 : Math.ceil(scaled - 1e-9) - 1;
		return Math.clamp(next / STEPS, 0.0, 1.0);
	}

	public static int percent(double volume) {
		return (int) Math.round(volume * 100);
	}
}
