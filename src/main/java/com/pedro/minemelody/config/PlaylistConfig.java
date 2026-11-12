package com.pedro.minemelody.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.core.BannerMode;
import com.pedro.minemelody.core.BannerPosition;
import com.pedro.minemelody.core.BannerScale;
import com.pedro.minemelody.core.PlaybackMode;
import com.pedro.minemelody.core.PlaylistLibrary;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

	/** Nombre de la playlist que se crea la primera vez (y al migrar desde v1). */
	public static final String DEFAULT_PLAYLIST_NAME = "Principal";

	/** Versión del FORMATO de este archivo. Ver {@link ConfigMigration}. */
	public int configVersion = ConfigMigration.CURRENT_VERSION;
	/** Si la playlist manda sobre la música del juego. */
	public boolean enabled = false;
	/** Tus playlists con nombre. Los ids de canción son tipo "minecraft:music/game/sweden". */
	public PlaylistLibrary library = PlaylistLibrary.withDefault(DEFAULT_PLAYLIST_NAME);
	/** Canciones marcadas con ★ (mismos ids que en las playlists). */
	public Set<String> favorites = new LinkedHashSet<>();
	public PlaybackMode mode = PlaybackMode.IN_ORDER;
	/** Silencio entre canciones, en segundos. */
	public int gapSeconds = 3;
	/** En survival, los discos sólo suenan si los conseguiste en ese mundo. */
	public boolean discsRequireUnlock = true;
	/** Banner de "Sonando ahora": apagado, al empezar cada canción o siempre. */
	public BannerMode bannerMode = BannerMode.ON_START;
	/** Tamaño del banner: 1.0 = normal, 0.5 = la mitad, 2.0 = el doble. */
	public double bannerScale = 1.0;
	public BannerPosition bannerPosition = BannerPosition.TOP_LEFT;
	/** Mundo/servidor -> eventos de disco desbloqueados (ej: "minecraft:music_disc.cat"). */
	public Map<String, Set<String>> unlockedDiscs = new HashMap<>();

	public static PlaylistConfig load() {
		migrateLegacyFile();
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				int fileVersion = json.has("configVersion") ? json.get("configVersion").getAsInt() : 1;
				if (fileVersion < ConfigMigration.CURRENT_VERSION) {
					backup(fileVersion);
				}

				PlaylistConfig config = GSON.fromJson(ConfigMigration.migrate(json, DEFAULT_PLAYLIST_NAME), PlaylistConfig.class);
				if (config != null) {
					config.sanitized();
					if (fileVersion < ConfigMigration.CURRENT_VERSION) {
						config.save(); // guardamos ya en el formato nuevo
						MineMelodyClient.LOGGER.info("Config migrada de v{} a v{}", fileVersion, ConfigMigration.CURRENT_VERSION);
					}
					return config;
				}
			} catch (IOException | JsonParseException | IllegalStateException e) {
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

	/**
	 * Regla de oro de las migraciones: ANTES de tocar nada, copia de seguridad.
	 * Si el migrador tuviera un bug, el archivo original sigue a salvo.
	 */
	private static void backup(int fileVersion) {
		Path backup = FILE.resolveSibling("minemelody.v" + fileVersion + ".backup.json");
		try {
			Files.copy(FILE, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			MineMelodyClient.LOGGER.error("No pude crear el backup {}", backup, e);
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
		if (library == null) library = PlaylistLibrary.withDefault(DEFAULT_PLAYLIST_NAME);
		library.repair(DEFAULT_PLAYLIST_NAME);
		if (favorites == null) favorites = new LinkedHashSet<>();
		if (mode == null) mode = PlaybackMode.IN_ORDER;
		if (unlockedDiscs == null) unlockedDiscs = new HashMap<>();
		if (bannerPosition == null) bannerPosition = BannerPosition.TOP_LEFT;
		if (bannerMode == null) bannerMode = BannerMode.ON_START;
		bannerScale = BannerScale.sanitize(bannerScale);
		gapSeconds = Math.clamp(gapSeconds, 0, 600);
		return this;
	}
}
