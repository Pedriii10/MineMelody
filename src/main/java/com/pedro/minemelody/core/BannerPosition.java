package com.pedro.minemelody.core;

/**
 * Esquina de la pantalla donde aparece el banner de "Sonando ahora".
 */
public enum BannerPosition {
	TOP_LEFT(true, true),
	TOP_RIGHT(true, false),
	BOTTOM_RIGHT(false, false),
	BOTTOM_LEFT(false, true);

	private final boolean top;
	private final boolean left;

	BannerPosition(boolean top, boolean left) {
		this.top = top;
		this.left = left;
	}

	public boolean isTop() {
		return top;
	}

	public boolean isLeft() {
		return left;
	}

	public BannerPosition cycle() {
		BannerPosition[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public String translationKey() {
		return "minemelody.position." + name().toLowerCase();
	}
}
