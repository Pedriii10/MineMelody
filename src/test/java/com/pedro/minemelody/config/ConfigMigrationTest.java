package com.pedro.minemelody.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigrationTest {

	@Test
	void migratesV1SinglePlaylistIntoNamedLibrary() {
		JsonObject v1 = JsonParser.parseString("""
				{
				  "enabled": true,
				  "tracks": ["minecraft:music/game/sweden", "minecraft:records/cat"],
				  "gapSeconds": 5
				}
				""").getAsJsonObject();

		JsonObject migrated = ConfigMigration.migrate(v1, "Principal");

		assertEquals(ConfigMigration.CURRENT_VERSION, migrated.get("configVersion").getAsInt());
		assertFalse(migrated.has("tracks"));
		JsonObject library = migrated.getAsJsonObject("library");
		assertEquals("Principal", library.get("active").getAsString());
		JsonObject playlist = library.getAsJsonArray("playlists").get(0).getAsJsonObject();
		assertEquals("Principal", playlist.get("name").getAsString());
		JsonArray tracks = playlist.getAsJsonArray("tracks");
		assertEquals(2, tracks.size());
		assertEquals("minecraft:music/game/sweden", tracks.get(0).getAsString());
		// Lo que no tiene que ver con la migración queda intacto.
		assertTrue(migrated.get("enabled").getAsBoolean());
		assertEquals(5, migrated.get("gapSeconds").getAsInt());
	}

	@Test
	void v1WithoutTracksStillGetsALibrary() {
		JsonObject migrated = ConfigMigration.migrate(new JsonObject(), "Principal");

		assertEquals(1, migrated.getAsJsonObject("library").getAsJsonArray("playlists").size());
	}

	@Test
	void currentVersionIsLeftUntouched() {
		JsonObject current = JsonParser.parseString("""
				{"configVersion": 2, "library": {"active": "X", "playlists": []}}
				""").getAsJsonObject();

		assertEquals(current, ConfigMigration.migrate(current.deepCopy(), "Principal"));
	}
}
