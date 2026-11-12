package com.pedro.minemelody.core;

/** Tamaños del banner: de la mitad al doble, en pasos cómodos. */
public final class BannerScale {

	private static final double[] PRESETS = {0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0};
	public static final double MIN = PRESETS[0];
	public static final double MAX = PRESETS[PRESETS.length - 1];
	private static final double EPSILON = 1e-9;

	private BannerScale() {
	}

	/** El siguiente tamaño en esa dirección (+1 más grande, -1 más chico). */
	public static double step(double current, int direction) {
		if (direction > 0) {
			for (double preset : PRESETS) {
				if (preset > current + EPSILON) return preset;
			}
			return MAX;
		}
		for (int i = PRESETS.length - 1; i >= 0; i--) {
			if (PRESETS[i] < current - EPSILON) return PRESETS[i];
		}
		return MIN;
	}

	public static double sanitize(double scale) {
		if (Double.isNaN(scale)) return 1.0;
		return Math.clamp(scale, MIN, MAX);
	}
}
