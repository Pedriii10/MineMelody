package com.pedro.minemelody.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistLibraryTest {

	private static final String NETHER = "minecraft:the_nether";
	private static final String OVERWORLD = "minecraft:overworld";

	@Test
	void startsWithOneActivePlaylist() {
		PlaylistLibrary library = PlaylistLibrary.withDefault("Principal");

		assertEquals(1, library.playlists().size());
		assertEquals("Principal", library.active().name());
	}

	@Nested
	class Creating {

		@Test
		void createdPlaylistGetsAUniqueNameAndBecomesActive() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("Playlist");

			Playlist second = library.create("Playlist");
			Playlist third = library.create("Playlist");

			assertEquals("Playlist 2", second.name());
			assertEquals("Playlist 3", third.name());
			assertEquals("Playlist 3", library.active().name());
		}
	}

	@Nested
	class Deleting {

		@Test
		void deletingTheActiveOneActivatesANeighbour() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("A");
			library.create("B");

			assertTrue(library.deleteActive());

			assertEquals(List.of("A"), library.playlists().stream().map(Playlist::name).toList());
			assertEquals("A", library.active().name());
		}

		@Test
		void cannotDeleteTheLastPlaylist() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("A");

			assertFalse(library.deleteActive());
			assertEquals(1, library.playlists().size());
		}
	}

	@Nested
	class Renaming {

		@Test
		void renamesTheActivePlaylist() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("A");

			assertTrue(library.renameActive("  Para minar  "));
			assertEquals("Para minar", library.active().name());
		}

		@Test
		void rejectsBlankOrDuplicatedNamesIgnoringCase() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("Minar");
			library.create("Construir");

			assertFalse(library.renameActive("   "));
			assertFalse(library.renameActive("MINAR"));
			assertEquals("Construir", library.active().name());
		}

		@Test
		void renamingToItsOwnNameWithDifferentCaseIsAllowed() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("minar");

			assertTrue(library.renameActive("Minar"));
		}
	}

	@Nested
	class Cycling {

		@Test
		void cyclesForwardAndBackwardsWrappingAround() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("A");
			library.create("B");
			library.create("C"); // activa: C

			library.cycle(1);
			assertEquals("A", library.active().name());
			library.cycle(-1);
			assertEquals("C", library.active().name());
		}
	}

	@Nested
	class Dimensions {

		@Test
		void boundPlaylistWinsInItsDimension() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("General");
			library.active().tracks().add("minecraft:music/game/sweden");
			Playlist nether = library.create("Nether");
			nether.tracks().add("minecraft:music/game/nether/rubedo");
			library.bindActiveTo(NETHER);
			library.cycle(-1); // activa: General

			assertEquals("Nether", library.effective(NETHER).name());
			assertEquals("General", library.effective(OVERWORLD).name());
		}

		@Test
		void emptyBoundPlaylistFallsBackToActive() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("General");
			library.create("Nether");
			library.bindActiveTo(NETHER);
			library.cycle(-1);

			assertEquals("General", library.effective(NETHER).name());
		}

		@Test
		void onlyOnePlaylistPerDimension() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("A");
			library.bindActiveTo(NETHER);
			library.create("B");
			library.bindActiveTo(NETHER);

			assertNull(library.find("A").orElseThrow().dimension());
			assertEquals(NETHER, library.find("B").orElseThrow().dimension());
		}

		@Test
		void bindingToNullUnbinds() {
			PlaylistLibrary library = PlaylistLibrary.withDefault("A");
			library.bindActiveTo(NETHER);
			library.bindActiveTo(null);

			assertNull(library.active().dimension());
		}
	}

	@Test
	void repairsBrokenStateFromHandEditedConfig() {
		PlaylistLibrary library = new PlaylistLibrary();
		library.repair("Principal");

		assertEquals("Principal", library.active().name());
	}
}
