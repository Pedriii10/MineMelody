package com.pedro.minemelody.core;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Búsqueda "como la de Spotify": sin importar mayúsculas ni tildes, y todas las palabras
 * tienen que aparecer, en cualquier orden. "sweden c418" encuentra "C418 - Sweden".
 */
public final class SearchQuery {

	private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final List<String> words;

	private SearchQuery(List<String> words) {
		this.words = words;
	}

	public static SearchQuery of(String query) {
		String normalized = normalize(query).strip();
		if (normalized.isEmpty()) {
			return new SearchQuery(List.of());
		}
		return new SearchQuery(Arrays.asList(WHITESPACE.split(normalized)));
	}

	public boolean matches(String text) {
		String normalized = normalize(text);
		return words.stream().allMatch(normalized::contains);
	}

	public boolean isEmpty() {
		return words.isEmpty();
	}

	/** "Música" -> "musica": separamos la tilde de la letra (NFD) y borramos la tilde. */
	private static String normalize(String text) {
		String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
		return ACCENTS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
	}
}
