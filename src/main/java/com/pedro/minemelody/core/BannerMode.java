package com.pedro.minemelody.core;

/** Cuándo se ve el banner de "Sonando ahora". */
public enum BannerMode {
	/** Nunca. */
	OFF,
	/** Unos segundos cada vez que empieza una canción. */
	ON_START,
	/** Siempre que suene música, con barra de progreso. */
	ALWAYS;

	public BannerMode cycle() {
		BannerMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public String translationKey() {
		return "minemelody.banner_mode." + name().toLowerCase();
	}
}
