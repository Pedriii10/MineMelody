package com.pedro.minemelody.track;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Agrupa las canciones para que la lista sea legible. Se deduce de la carpeta del archivo.
 */
public enum TrackGroup {
	OVERWORLD,
	CREATIVE,
	WATER,
	NETHER,
	END,
	DISC;

	public static TrackGroup of(Identifier soundId, Identifier eventId) {
		if (Track.isDiscEvent(eventId)) return DISC;
		String path = soundId.getPath();
		if (path.startsWith("music/game/nether/")) return NETHER;
		if (path.startsWith("music/game/end/")) return END;
		if (path.startsWith("music/game/creative/")) return CREATIVE;
		if (path.startsWith("music/game/water/")) return WATER;
		return OVERWORLD;
	}

	public Component label() {
		return Component.translatable("minemelody.group." + name().toLowerCase());
	}
}
