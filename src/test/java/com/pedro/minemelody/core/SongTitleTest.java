package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SongTitleTest {

	@Test
	void splitsArtistAndTitle() {
		SongTitle song = SongTitle.parse("C418 - Sweden", "Superficie");

		assertEquals("C418", song.artist());
		assertEquals("Sweden", song.title());
	}

	@Test
	void onlySplitsAtTheFirstSeparator() {
		SongTitle song = SongTitle.parse("Lena Raine - Creator - Music Box", "Disco");

		assertEquals("Lena Raine", song.artist());
		assertEquals("Creator - Music Box", song.title());
	}

	@Test
	void usesFallbackWhenThereIsNoArtist() {
		SongTitle song = SongTitle.parse("mi_cancion_custom", "Superficie");

		assertEquals("Superficie", song.artist());
		assertEquals("mi_cancion_custom", song.title());
	}

	@Test
	void doesNotSplitHyphensInsideWords() {
		SongTitle song = SongTitle.parse("ballad-of-the-cats", "Nether");

		assertEquals("Nether", song.artist());
		assertEquals("ballad-of-the-cats", song.title());
	}

	@Test
	void trimsSpaces() {
		SongTitle song = SongTitle.parse("  C418  -  Wet Hands ", "x");

		assertEquals("C418", song.artist());
		assertEquals("Wet Hands", song.title());
	}
}
