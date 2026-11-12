package com.pedro.musicplaylist.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.pedro.musicplaylist.MusicPlaylistClient;
import com.pedro.musicplaylist.core.BannerPosition;
import com.pedro.musicplaylist.core.PlaybackMode;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Todo lo que el jugador configura. Se serializa tal cual a JSON con Gson,
 * así que los nombres de los campos SON el formato del archivo.
 */
public final class PlaylistConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("musicplaylist.json");

	/** Si la playlist manda sobre la música del juego. */
	public boolean enabled = false;
	/** Ids de los archivos de sonido, en orden. Ej: "minecraft:music/game/sweden". */
	public List<String> tracks = new ArrayList<>();
	public PlaybackMode mode = PlaybackMode.IN_ORDER;
	/** Silencio entre canciones, en segundos. */
	public int gapSeconds = 3;
	/** En survival, los discos sólo suenan si los conseguiste en ese mundo. */
	public boolean discsRequireUnlock = true;
	/** Banner de "Sonando ahora" en una esquina cuando empieza una canción. */
	public boolean showBanner = true;
	public BannerPosition bannerPosition = BannerPosition.TOP_LEFT;
	/** Mundo/servidor -> eventos de disco desbloqueados (ej: "minecraft:music_disc.cat"). */
	public Map<String, Set<String>> unlockedDiscs = new HashMap<>();

	public static PlaylistConfig load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				PlaylistConfig config = GSON.fromJson(reader, PlaylistConfig.class);
				if (config != null) {
					return config.sanitized();
				}
			} catch (IOException | JsonParseException e) {
				MusicPlaylistClient.LOGGER.error("No pude leer {}, uso valores por defecto", FILE, e);
			}
		}
		return new PlaylistConfig();
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			MusicPlaylistClient.LOGGER.error("No pude guardar {}", FILE, e);
		}
	}

	public Set<String> unlockedDiscsFor(String worldKey) {
		return unlockedDiscs.computeIfAbsent(worldKey, key -> new LinkedHashSet<>());
	}

	/** Un JSON editado a mano puede traer nulls: los reemplazamos por valores sanos. */
	private PlaylistConfig sanitized() {
		if (tracks == null) tracks = new ArrayList<>();
		if (mode == null) mode = PlaybackMode.IN_ORDER;
		if (unlockedDiscs == null) unlockedDiscs = new HashMap<>();
		if (bannerPosition == null) bannerPosition = BannerPosition.TOP_LEFT;
		gapSeconds = Math.clamp(gapSeconds, 0, 600);
		return this;
	}
}
