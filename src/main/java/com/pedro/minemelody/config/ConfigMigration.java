package com.pedro.minemelody.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Migraciones del archivo de config, igual que las migraciones de una base de datos:
 * cada versión sabe cómo pasar a la siguiente, y se aplican en orden.
 * <pre>
 *   v1 (MineMelody 0.1 - 0.2):  { "tracks": [...] }                        una sola playlist
 *   v2 (MineMelody 0.3+):       { "configVersion": 2, "library": {...} }   varias con nombre
 * </pre>
 * Trabajamos sobre el JSON "crudo" (JsonObject) y no sobre la clase, porque la clase ya
 * tiene la forma NUEVA: no puede representar el formato viejo.
 */
public final class ConfigMigration {

	public static final int CURRENT_VERSION = 2;

	private ConfigMigration() {
	}

	public static JsonObject migrate(JsonObject json, String defaultPlaylistName) {
		int version = json.has("configVersion") ? json.get("configVersion").getAsInt() : 1;

		if (version < 2) {
			migrateV1ToV2(json, defaultPlaylistName);
		}
		// if (version < 3) { migrateV2ToV3(json); }  <- así se agregan las futuras

		json.addProperty("configVersion", CURRENT_VERSION);
		return json;
	}

	/** La lista "tracks" suelta pasa a ser la primera playlist de la biblioteca. */
	private static void migrateV1ToV2(JsonObject json, String defaultPlaylistName) {
		JsonElement oldTracks = json.remove("tracks");

		JsonObject playlist = new JsonObject();
		playlist.addProperty("name", defaultPlaylistName);
		playlist.add("tracks", oldTracks != null && oldTracks.isJsonArray() ? oldTracks : new JsonArray());

		JsonArray playlists = new JsonArray();
		playlists.add(playlist);

		JsonObject library = new JsonObject();
		library.addProperty("active", defaultPlaylistName);
		library.add("playlists", playlists);
		json.add("library", library);
	}
}
