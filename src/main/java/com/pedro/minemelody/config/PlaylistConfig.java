package com.pedro.minemelody.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.core.BannerPosition;
import com.pedro.minemelody.core.PlaybackMode;
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
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("minemelody.json");
	/** Nombre del archivo antes de renombrar el mod a MineMelody (v0.1.0). */
	private static final Path LEGACY_FILE = FabricLoader.getInstance().getConfigDir().resolve("musicplaylist.json");

	/** Si la playlist manda sobre la música del juego. */
	public boolean enabled = false;
	/** Ids de los archivos de sonido, en orden. Ej: "minecraft:music/game/sweden". */
	public List<String> tracks = new ArrayList<>();
	/** Canciones marcadas con ★ (ids, igual que en tracks). */
	public Set<String> favorites = new LinkedHashSet<>();
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
		migrateLegacyFile();
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				PlaylistConfig config = GSON.fromJson(reader, PlaylistConfig.class);
				if (config != null) {
					return config.sanitized();
				}
			} catch (IOException | JsonParseException e) {
				MineMelodyClient.LOGGER.error("No pude leer {}, uso valores por defecto", FILE, e);
			}
		}
		return new PlaylistConfig();
	}

	/**
	 * La v0.1.0 se llamaba "Music Playlist" y guardaba en musicplaylist.json. Si existe ese archivo
	 * y todavía no hay uno nuevo, lo renombramos: así nadie pierde su playlist al actualizar.
	 */
	private static void migrateLegacyFile() {
		if (Files.exists(FILE) || !Files.exists(LEGACY_FILE)) return;
		try {
			Files.move(LEGACY_FILE, FILE);
			MineMelodyClient.LOGGER.info("Config migrada de {} a {}", LEGACY_FILE.getFileName(), FILE.getFileName());
		} catch (IOException e) {
			MineMelodyClient.LOGGER.error("No pude migrar {}", LEGACY_FILE, e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			MineMelodyClient.LOGGER.error("No pude guardar {}", FILE, e);
		}
	}

	public Set<String> unlockedDiscsFor(String worldKey) {
		return unlockedDiscs.computeIfAbsent(worldKey, key -> new LinkedHashSet<>());
	}

	/** Un JSON editado a mano puede traer nulls: los reemplazamos por valores sanos. */
	private PlaylistConfig sanitized() {
		if (tracks == null) tracks = new ArrayList<>();
		if (favorites == null) favorites = new LinkedHashSet<>();
		if (mode == null) mode = PlaybackMode.IN_ORDER;
		if (unlockedDiscs == null) unlockedDiscs = new HashMap<>();
		if (bannerPosition == null) bannerPosition = BannerPosition.TOP_LEFT;
		gapSeconds = Math.clamp(gapSeconds, 0, 600);
		return this;
	}
}
