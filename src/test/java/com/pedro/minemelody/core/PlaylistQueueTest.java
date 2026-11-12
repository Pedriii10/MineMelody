package com.pedro.minemelody.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaylistQueueTest {

	private static final Predicate<String> ALL = id -> true;
	private static final List<String> PLAYLIST = List.of("a", "b", "c");

	@Test
	void emptyPlaylistReturnsNothing() {
		PlaylistQueue queue = new PlaylistQueue(new Random(1));

		assertEquals(Optional.empty(), queue.next(List.of(), ALL, PlaybackMode.IN_ORDER));
	}

	@Test
	void playlistWithEverythingUnavailableReturnsNothing() {
		PlaylistQueue queue = new PlaylistQueue(new Random(1));

		assertEquals(Optional.empty(), queue.next(PLAYLIST, id -> false, PlaybackMode.SHUFFLE));
	}

	@Nested
	class InOrder {

		@Test
		void playsInOrderAndWrapsAround() {
			PlaylistQueue queue = new PlaylistQueue(new Random(1));

			List<String> played = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				played.add(queue.next(PLAYLIST, ALL, PlaybackMode.IN_ORDER).orElseThrow());
			}

			assertEquals(List.of("a", "b", "c", "a", "b"), played);
		}

		@Test
		void skipsUnavailableTracks() {
			PlaylistQueue queue = new PlaylistQueue(new Random(1));
			Predicate<String> lockedB = id -> !id.equals("b");

			assertEquals("a", queue.next(PLAYLIST, lockedB, PlaybackMode.IN_ORDER).orElseThrow());
			assertEquals("c", queue.next(PLAYLIST, lockedB, PlaybackMode.IN_ORDER).orElseThrow());
			assertEquals("a", queue.next(PLAYLIST, lockedB, PlaybackMode.IN_ORDER).orElseThrow());
		}

		@Test
		void continuesFromPositionWhenLastTrackIsRemoved() {
			PlaylistQueue queue = new PlaylistQueue(new Random(1));
			queue.next(PLAYLIST, ALL, PlaybackMode.IN_ORDER); // a
			queue.next(PLAYLIST, ALL, PlaybackMode.IN_ORDER); // b

			// El usuario saca "b" de la playlist mientras sonaba: seguimos con la que venía después.
			assertEquals("c", queue.next(List.of("a", "c"), ALL, PlaybackMode.IN_ORDER).orElseThrow());
		}
	}

	@Nested
	class RepeatOne {

		@Test
		void repeatsTheLastPlayedTrack() {
			PlaylistQueue queue = new PlaylistQueue(new Random(1));
			queue.next(PLAYLIST, ALL, PlaybackMode.IN_ORDER); // a
			queue.next(PLAYLIST, ALL, PlaybackMode.IN_ORDER); // b

			assertEquals("b", queue.next(PLAYLIST, ALL, PlaybackMode.REPEAT_ONE).orElseThrow());
			assertEquals("b", queue.next(PLAYLIST, ALL, PlaybackMode.REPEAT_ONE).orElseThrow());
		}

		@Test
		void startsWithFirstTrackWhenNothingPlayedYet() {
			PlaylistQueue queue = new PlaylistQueue(new Random(1));

			assertEquals("a", queue.next(PLAYLIST, ALL, PlaybackMode.REPEAT_ONE).orElseThrow());
		}
	}

	@Nested
	class Shuffle {

		@Test
		void playsEveryTrackOnceBeforeRepeating() {
			List<String> playlist = List.of("a", "b", "c", "d", "e");
			PlaylistQueue queue = new PlaylistQueue(new Random(42));

			Set<String> firstRound = new HashSet<>();
			for (int i = 0; i < playlist.size(); i++) {
				firstRound.add(queue.next(playlist, ALL, PlaybackMode.SHUFFLE).orElseThrow());
			}

			assertEquals(Set.copyOf(playlist), firstRound);
		}

		@Test
		void neverRepeatsTheSameTrackBackToBack() {
			PlaylistQueue queue = new PlaylistQueue(new Random(7));

			String previous = queue.next(PLAYLIST, ALL, PlaybackMode.SHUFFLE).orElseThrow();
			for (int i = 0; i < 200; i++) {
				String current = queue.next(PLAYLIST, ALL, PlaybackMode.SHUFFLE).orElseThrow();
				assertNotEquals(previous, current);
				previous = current;
			}
		}

		@Test
		void singleTrackPlaylistRepeatsThatTrack() {
			PlaylistQueue queue = new PlaylistQueue(new Random(1));

			assertEquals("a", queue.next(List.of("a"), ALL, PlaybackMode.SHUFFLE).orElseThrow());
			assertEquals("a", queue.next(List.of("a"), ALL, PlaybackMode.SHUFFLE).orElseThrow());
		}

		@Test
		void onlyPicksAvailableTracks() {
			PlaylistQueue queue = new PlaylistQueue(new Random(3));
			Predicate<String> onlyC = id -> id.equals("c");

			for (int i = 0; i < 10; i++) {
				assertEquals("c", queue.next(PLAYLIST, onlyC, PlaybackMode.SHUFFLE).orElseThrow());
			}
		}

		@Test
		void newlyAddedTrackEventuallyPlays() {
			PlaylistQueue queue = new PlaylistQueue(new Random(5));
			queue.next(PLAYLIST, ALL, PlaybackMode.SHUFFLE);

			List<String> bigger = List.of("a", "b", "c", "nueva");
			Set<String> seen = new HashSet<>();
			for (int i = 0; i < 8; i++) {
				seen.add(queue.next(bigger, ALL, PlaybackMode.SHUFFLE).orElseThrow());
			}

			assertTrue(seen.contains("nueva"));
		}
	}
}
