package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistCodecTest {

	@Test
	void encodesWithVersionPrefixAndShortVanillaIds() {
		String text = PlaylistCodec.encode(List.of("minecraft:music/game/sweden", "mymod:music/cool_song"));

		assertEquals("MineMelody:1:music/game/sweden,mymod:music/cool_song", text);
	}

	@Test
	void roundTripKeepsOrder() {
		List<String> playlist = List.of("minecraft:records/cat", "minecraft:music/game/sweden", "mymod:music/x");

		assertEquals(Optional.of(playlist), PlaylistCodec.decode(PlaylistCodec.encode(playlist)));
	}

	@Test
	void decodeToleratesSpacesAndNewlines() {
		Optional<List<String>> decoded = PlaylistCodec.decode("  MineMelody:1: music/game/sweden ,\n records/cat \n");

		assertEquals(Optional.of(List.of("minecraft:music/game/sweden", "minecraft:records/cat")), decoded);
	}

	@Test
	void rejectsTextThatIsNotAPlaylist() {
		assertTrue(PlaylistCodec.decode("hola, esto es un mensaje de Discord").isEmpty());
		assertTrue(PlaylistCodec.decode("").isEmpty());
	}

	@Test
	void rejectsEmptyPlaylist() {
		assertTrue(PlaylistCodec.decode("MineMelody:1:").isEmpty());
	}

	@Test
	void skipsInvalidAndDuplicatedEntries() {
		Optional<List<String>> decoded = PlaylistCodec.decode("MineMelody:1:records/cat,NO VALE!,records/cat,,music/game/sweden");

		assertEquals(Optional.of(List.of("minecraft:records/cat", "minecraft:music/game/sweden")), decoded);
	}

	@Test
	void rejectsUnknownFormatVersion() {
		assertTrue(PlaylistCodec.decode("MineMelody:99:records/cat").isEmpty());
	}
}
