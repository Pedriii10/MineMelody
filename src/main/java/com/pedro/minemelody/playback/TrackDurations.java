package com.pedro.minemelody.playback;

import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.core.OggDuration;
import com.pedro.minemelody.track.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Duración de cada canción, leída del .ogg EN SEGUNDO PLANO y guardada en caché.
 * <p>
 * ¿Por qué en otro hilo? Para saber la duración hay que leer el archivo hasta el final
 * (varios MB). Si lo hiciéramos en el hilo del juego, la pantalla se congelaría.
 * Usamos un único hilo propio y de baja prioridad para no competir con la carga del juego.
 */
public final class TrackDurations {

	private static final double UNKNOWN = -1;

	private final Map<Identifier, Double> cache = new ConcurrentHashMap<>();
	private final Set<Identifier> pending = ConcurrentHashMap.newKeySet();
	/** Sube cada vez que llega una duración nueva: la pantalla lo mira para saber si refrescar. */
	private final AtomicInteger version = new AtomicInteger();
	private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "MineMelody duration reader");
		thread.setDaemon(true); // no impide que el juego se cierre
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});

	/** Devuelve la duración si ya la sabemos; si no, la pide (y por ahora devuelve vacío). */
	public OptionalDouble get(Minecraft minecraft, Track track) {
		Double seconds = cache.get(track.id());
		if (seconds != null) {
			return seconds > 0 ? OptionalDouble.of(seconds) : OptionalDouble.empty();
		}
		request(minecraft, track);
		return OptionalDouble.empty();
	}

	public int version() {
		return version.get();
	}

	private void request(Minecraft minecraft, Track track) {
		if (!pending.add(track.id())) return; // ya está en camino

		Identifier file = track.sound().getPath(); // "minecraft:sounds/music/game/sweden.ogg"
		worker.execute(() -> {
			double seconds = UNKNOWN;
			try {
				Optional<Resource> resource = minecraft.getResourceManager().getResource(file);
				if (resource.isPresent()) {
					try (InputStream in = resource.get().open()) {
						seconds = OggDuration.read(in).orElse(UNKNOWN);
					}
				}
			} catch (Exception e) {
				MineMelodyClient.LOGGER.debug("No pude leer la duración de {}", file, e);
			}
			cache.put(track.id(), seconds);
			pending.remove(track.id());
			version.incrementAndGet();
		});
	}
}
