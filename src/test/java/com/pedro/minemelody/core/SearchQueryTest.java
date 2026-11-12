package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchQueryTest {

	@Test
	void emptyQueryMatchesEverything() {
		assertTrue(SearchQuery.of("").matches("C418 - Sweden"));
		assertTrue(SearchQuery.of("   ").matches("C418 - Sweden"));
	}

	@Test
	void ignoresCase() {
		assertTrue(SearchQuery.of("SWEDEN").matches("C418 - Sweden"));
	}

	@Test
	void ignoresAccents() {
		assertTrue(SearchQuery.of("musica").matches("Música del Nether"));
		assertTrue(SearchQuery.of("música").matches("Musica del Nether"));
	}

	@Test
	void everyWordMustAppearInAnyOrder() {
		assertTrue(SearchQuery.of("sweden c418").matches("C418 - Sweden"));
		assertFalse(SearchQuery.of("sweden lena").matches("C418 - Sweden"));
	}

	@Test
	void matchesPartialWords() {
		assertTrue(SearchQuery.of("swe").matches("C418 - Sweden"));
	}

	@Test
	void doesNotMatchUnrelatedText() {
		assertFalse(SearchQuery.of("pigstep").matches("C418 - Sweden"));
	}
}
