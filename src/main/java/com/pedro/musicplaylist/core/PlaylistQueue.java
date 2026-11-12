package com.pedro.musicplaylist.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

/**
 * El "cerebro" de la playlist: decide qué canción suena a continuación.
 * <p>
 * IMPORTANTE: esta clase no conoce Minecraft. Trabaja con ids (Strings) y un predicado
 * que dice si una canción está disponible (por ejemplo, un disco bloqueado en survival).
 * Por eso se puede testear con JUnit puro, sin abrir el juego.
 */
public final class PlaylistQueue {

	private final Random random;
	private final List<String> shuffleBag = new ArrayList<>();
	private String lastPlayed;
	private int lastIndex = -1;

	public PlaylistQueue(Random random) {
		this.random = random;
	}

	public Optional<String> next(List<String> playlist, Predicate<String> isAvailable, PlaybackMode mode) {
		List<String> candidates = playlist.stream().filter(isAvailable).distinct().toList();
		if (candidates.isEmpty()) {
			return Optional.empty();
		}

		String chosen = switch (mode) {
			case IN_ORDER -> nextInOrder(candidates);
			case SHUFFLE -> nextShuffled(candidates);
			case REPEAT_ONE -> candidates.contains(lastPlayed) ? lastPlayed : nextInOrder(candidates);
		};

		lastPlayed = chosen;
		lastIndex = candidates.indexOf(chosen);
		return Optional.of(chosen);
	}

	/** Olvida el historial (por ejemplo, al cambiar la playlist entera). */
	public void reset() {
		shuffleBag.clear();
		lastPlayed = null;
		lastIndex = -1;
	}

	private String nextInOrder(List<String> candidates) {
		int position = candidates.indexOf(lastPlayed);
		if (position >= 0) {
			return candidates.get((position + 1) % candidates.size());
		}
		// La última canción ya no está (la sacaron o se bloqueó): seguimos desde donde estaba.
		return candidates.get(Math.max(lastIndex, 0) % candidates.size());
	}

	/**
	 * "Bolsa" de shuffle: barajamos todas, las vamos sacando de a una y, cuando la bolsa
	 * se vacía, volvemos a barajar. Así nunca escuchás la misma dos veces seguidas.
	 */
	private String nextShuffled(List<String> candidates) {
		shuffleBag.retainAll(candidates);
		if (shuffleBag.isEmpty()) {
			refillBag(candidates);
		}
		return shuffleBag.removeFirst();
	}

	private void refillBag(List<String> candidates) {
		shuffleBag.addAll(candidates);
		Collections.shuffle(shuffleBag, random);
		if (shuffleBag.size() > 1 && shuffleBag.getFirst().equals(lastPlayed)) {
			Collections.swap(shuffleBag, 0, 1 + random.nextInt(shuffleBag.size() - 1));
		}
	}
}
