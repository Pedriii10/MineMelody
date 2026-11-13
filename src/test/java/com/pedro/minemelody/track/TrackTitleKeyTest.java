package com.pedro.minemelody.track;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bug encontrado en las capturas para Modrinth: las canciones normales salían como
 * "a_familiar_room" en vez de "Aaron Cherof - A Familiar Room".
 * <p>
 * Causa: Identifier.toShortLanguageKey() devuelve el path TAL CUAL ("music/game/a_familiar_room"),
 * pero en los archivos de idioma la clave usa puntos ("music.game.a_familiar_room").
 * Vanilla (NowPlayingToast.getNowPlayingString) hace replace("/", ".") antes de traducir.
 */
class TrackTitleKeyTest {

	@Test
	void gameMusicKeyUsesDotsLikeTheLanguageFiles() {
		Track track = track("minecraft", "music/game/a_familiar_room", "music.game");

		assertEquals("music.game.a_familiar_room", track.titleKey());
	}

	@Test
	void nestedFoldersAlsoUseDots() {
		Track track = track("minecraft", "music/game/nether/nether_wastes/rubedo", "music.nether.nether_wastes");

		assertEquals("music.game.nether.nether_wastes.rubedo", track.titleKey());
	}

	@Test
	void discKeyComesFromTheJukeboxSong() {
		Track track = track("minecraft", "records/cat", "music_disc.cat");

		assertEquals("jukebox_song.minecraft.cat", track.titleKey());
	}

	@Test
	void modTracksKeepTheirNamespace() {
		Track track = track("mymod", "music/cool_song", "music.cool");

		assertEquals("mymod.music.cool_song", track.titleKey());
	}

	private static Track track(String namespace, String soundPath, String eventPath) {
		Identifier id = Identifier.fromNamespaceAndPath(namespace, soundPath);
		Identifier event = Identifier.fromNamespaceAndPath(namespace, eventPath);
		return new Track(id, event, null, TrackGroup.of(id, event));
	}
}
