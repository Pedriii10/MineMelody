package com.pedro.minemelody.gui;

import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.config.PlaylistConfig;
import com.pedro.minemelody.playback.PlaylistPlayer;
import com.pedro.minemelody.playback.PlaylistSoundInstance;
import com.pedro.minemelody.track.Track;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Pantalla para armar la playlist.
 * <pre>
 *  ┌──────────── Playlist de música ────────────┐
 *  │ Canciones (60)        │ Tu playlist (3)     │
 *  │ [Overworld] Sweden    │ C418 - Sweden       │
 *  │ [Disco] cat (bloq.)   │ C418 - Wet Hands    │
 *  │ ...                   │ ...                 │
 *  ├────────────────────────────────────────────┤
 *  │ Añadir  Quitar  Subir  Bajar  Vaciar  Todas │
 *  │ Playlist  Modo  Pausa  Discos  Banner  Esq. │
 *  │ ▶ Escuchar   ⏭ Siguiente          Listo     │
 *  └────────────────────────────────────────────┘
 * </pre>
 * Cada cambio se guarda al instante: si el juego crashea, no perdés tu playlist.
 */
public final class PlaylistScreen extends Screen {

	private static final int[] GAP_OPTIONS = {0, 1, 3, 5, 10, 30, 60};

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;
	private static final int DARK_GRAY = 0xFF707070;
	private static final int GREEN = 0xFF7CFC7C;
	private static final int RED = 0xFFFF6B6B;
	private static final int GOLD = 0xFFFFD166;

	private static final int MARGIN = 10;
	private static final int TITLE_Y = 6;
	private static final int NOW_PLAYING_Y = 18;
	private static final int LIST_TOP = 46;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;

	private final Screen parent;
	private final PlaylistConfig config = MineMelodyClient.config();
	private final PlaylistPlayer player = MineMelodyClient.player();

	private TrackListWidget catalogList;
	private TrackListWidget playlistList;
	private Button listenButton;

	/** Última canción clicada en CUALQUIERA de las dos listas: es la que suena con "Escuchar". */
	private String clickedTrackId;
	/** Para refrescar las listas (y pintar de dorado) cuando cambia la canción que suena. */
	private String lastPlayingId;

	public PlaylistScreen(Screen parent) {
		super(Component.translatable("minemelody.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		player.catalog().refresh(minecraft.getSoundManager());

		int columnWidth = (width - MARGIN * 3) / 2;
		int listHeight = height - LIST_TOP - (BUTTON_HEIGHT + BUTTON_GAP) * 3 - MARGIN;

		catalogList = addRenderableWidget(new TrackListWidget(minecraft, MARGIN, LIST_TOP, columnWidth, listHeight,
				this::onRowClicked, row -> addTrack(row.id())));
		playlistList = addRenderableWidget(new TrackListWidget(minecraft, MARGIN * 2 + columnWidth, LIST_TOP, columnWidth, listHeight,
				this::onRowClicked, row -> removeTrack(row.id())));

		int rowY = LIST_TOP + listHeight + MARGIN / 2;
		addButtonRow(rowY,
				button("minemelody.button.add", () -> addTrack(catalogList.selectedId())),
				button("minemelody.button.remove", () -> removeTrack(playlistList.selectedId())),
				button("minemelody.button.up", () -> moveSelected(-1)),
				button("minemelody.button.down", () -> moveSelected(1)),
				button("minemelody.button.add_all", this::addAll),
				button("minemelody.button.clear", this::clearPlaylist));

		rowY += BUTTON_HEIGHT + BUTTON_GAP;
		addButtonRow(rowY,
				toggle(() -> Component.translatable("minemelody.button.enabled", onOff(config.enabled)),
						"minemelody.tooltip.enabled", this::toggleEnabled),
				toggle(() -> Component.translatable("minemelody.button.mode", Component.translatable(config.mode.translationKey())),
						"minemelody.tooltip.mode", () -> config.mode = config.mode.cycle()),
				toggle(() -> Component.translatable("minemelody.button.gap", config.gapSeconds),
						"minemelody.tooltip.gap", this::cycleGap),
				toggle(() -> Component.translatable("minemelody.button.discs", onOff(config.discsRequireUnlock)),
						"minemelody.tooltip.discs", () -> config.discsRequireUnlock = !config.discsRequireUnlock),
				toggle(() -> Component.translatable("minemelody.button.banner", onOff(config.showBanner)),
						"minemelody.tooltip.banner", () -> config.showBanner = !config.showBanner),
				toggle(() -> Component.translatable(config.bannerPosition.translationKey()),
						"minemelody.tooltip.position", () -> config.bannerPosition = config.bannerPosition.cycle()));

		rowY += BUTTON_HEIGHT + BUTTON_GAP;
		Button.Builder listen = button("minemelody.button.listen", this::listenToClicked)
				.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.listen")));
		listenButton = addButtonRow(rowY,
				listen,
				button("minemelody.button.skip", () -> player.skip(minecraft)),
				Button.builder(CommonComponents.GUI_DONE, b -> onClose())).getFirst();

		refreshLists(null, null);
		updateListenButton();
	}

	@Override
	public void tick() {
		updateListenButton();

		String playing = player.currentSound(minecraft).map(sound -> sound.track().id().toString()).orElse(null);
		if (!Objects.equals(playing, lastPlayingId)) {
			lastPlayingId = playing;
			refreshLists(catalogList.selectedId(), playlistList.selectedId());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(font, title, width / 2, TITLE_Y, WHITE);
		graphics.centeredText(font, nowPlaying(), width / 2, NOW_PLAYING_Y, GREEN);
		graphics.text(font, Component.translatable("minemelody.screen.catalog", catalogList.size()),
				catalogList.getX(), LIST_TOP - 11, GRAY);
		graphics.text(font, Component.translatable("minemelody.screen.playlist", playlistList.size()),
				playlistList.getX(), LIST_TOP - 11, GRAY);
	}

	@Override
	public void onClose() {
		config.save();
		minecraft.gui.setScreen(parent);
	}

	// ---------------------------------------------------------------- acciones

	private void onRowClicked(TrackListWidget.Row row) {
		clickedTrackId = row.id();
		updateListenButton();
	}

	private void listenToClicked() {
		clickableTrack().ifPresent(track -> player.playNow(minecraft, track));
	}

	/** La canción clicada, si existe y se puede escuchar (un disco bloqueado NO: sería hacer trampa). */
	private Optional<Track> clickableTrack() {
		if (clickedTrackId == null) return Optional.empty();
		return player.catalog().get(clickedTrackId).filter(track -> player.unlocks().isAvailable(minecraft, track));
	}

	private void updateListenButton() {
		if (listenButton != null) {
			listenButton.active = clickableTrack().isPresent();
		}
	}

	private void addTrack(String id) {
		if (id == null || config.tracks.contains(id)) return;
		config.tracks.add(id);
		changed(id);
	}

	private void addAll() {
		for (Track track : player.catalog().all()) {
			String id = track.id().toString();
			if (!config.tracks.contains(id)) config.tracks.add(id);
		}
		changed(null);
	}

	private void removeTrack(String id) {
		if (id == null) return;
		int index = config.tracks.indexOf(id);
		if (index < 0) return;
		config.tracks.remove(index);
		// Dejamos seleccionada la que quedó en ese lugar: así podés borrar varias seguidas.
		changed(config.tracks.isEmpty() ? null : config.tracks.get(Math.min(index, config.tracks.size() - 1)));
	}

	private void moveSelected(int direction) {
		String id = playlistList.selectedId();
		if (id == null) return;
		int from = config.tracks.indexOf(id);
		int to = from + direction;
		if (from < 0 || to < 0 || to >= config.tracks.size()) return;
		config.tracks.add(to, config.tracks.remove(from));
		changed(id);
	}

	private void clearPlaylist() {
		config.tracks.clear();
		player.resetQueue();
		changed(null);
	}

	private void toggleEnabled() {
		config.enabled = !config.enabled;
		boolean playingOurs = player.currentSound(minecraft).isPresent();
		if (config.enabled || playingOurs) {
			// Al activar arranca ya; al desactivar cortamos la nuestra y vuelve la del juego.
			player.skip(minecraft);
		}
	}

	private void cycleGap() {
		int next = GAP_OPTIONS[0];
		for (int option : GAP_OPTIONS) {
			if (option > config.gapSeconds) {
				next = option;
				break;
			}
		}
		config.gapSeconds = next;
	}

	private void changed(String selectPlaylistId) {
		config.save();
		refreshLists(catalogList.selectedId(), selectPlaylistId);
	}

	// ---------------------------------------------------------------- filas

	private void refreshLists(String selectedCatalogId, String selectedPlaylistId) {
		List<TrackListWidget.Row> catalogRows = new ArrayList<>();
		for (Track track : player.catalog().all()) {
			catalogRows.add(catalogRow(track));
		}
		catalogList.setRows(catalogRows, selectedCatalogId);

		List<TrackListWidget.Row> playlistRows = new ArrayList<>();
		for (int i = 0; i < config.tracks.size(); i++) {
			playlistRows.add(playlistRow(i + 1, config.tracks.get(i)));
		}
		playlistList.setRows(playlistRows, selectedPlaylistId);
	}

	private TrackListWidget.Row catalogRow(Track track) {
		String id = track.id().toString();
		boolean available = player.unlocks().isAvailable(minecraft, track);
		boolean inPlaylist = config.tracks.contains(id);

		StringBuilder text = new StringBuilder()
				.append(inPlaylist ? "✔ " : "")
				.append('[').append(track.group().label().getString()).append("] ")
				.append(track.title().getString());
		if (!available) {
			text.append("  ").append(Component.translatable("minemelody.locked").getString());
		}

		int color = !available ? DARK_GRAY : id.equals(lastPlayingId) ? GOLD : inPlaylist ? GREEN : WHITE;
		return catalogList.new Row(id, text.toString(), color);
	}

	private TrackListWidget.Row playlistRow(int position, String id) {
		Optional<Track> track = player.catalog().get(id);
		if (track.isEmpty()) {
			// Canción de un mod/resource pack que ya no está. La mostramos para que puedas quitarla.
			return playlistList.new Row(id, position + ". " + id, RED);
		}
		boolean available = player.unlocks().isAvailable(minecraft, track.get());
		String text = position + ". " + track.get().title().getString()
				+ (available ? "" : "  " + Component.translatable("minemelody.locked").getString());
		int color = !available ? DARK_GRAY : id.equals(lastPlayingId) ? GOLD : WHITE;
		return playlistList.new Row(id, (id.equals(lastPlayingId) ? "♪ " : "") + text, color);
	}

	private Component nowPlaying() {
		Optional<PlaylistSoundInstance> current = player.currentSound(minecraft);
		if (current.isPresent()) {
			return Component.translatable("minemelody.screen.now_playing", current.get().track().title());
		}
		return config.enabled
				? Component.translatable("minemelody.screen.waiting")
				: Component.translatable("minemelody.screen.disabled");
	}

	// ---------------------------------------------------------------- helpers de botones

	private Button.Builder button(String key, Runnable action) {
		return Button.builder(Component.translatable(key), b -> action.run());
	}

	/** Botón cuyo texto depende del estado: al clickearlo cambia el estado y se re-dibuja el texto. */
	private Button.Builder toggle(Supplier<Component> label, String tooltipKey, Runnable action) {
		return Button.builder(label.get(), b -> {
			action.run();
			config.save();
			b.setMessage(label.get());
			refreshLists(catalogList.selectedId(), playlistList.selectedId());
		}).tooltip(Tooltip.create(Component.translatable(tooltipKey)));
	}

	private List<Button> addButtonRow(int y, Button.Builder... builders) {
		int count = builders.length;
		int buttonWidth = Math.min(150, (width - MARGIN * 2 - BUTTON_GAP * (count - 1)) / count);
		int rowWidth = buttonWidth * count + BUTTON_GAP * (count - 1);
		int x = (width - rowWidth) / 2;
		List<Button> created = new ArrayList<>();
		for (Button.Builder builder : builders) {
			created.add(addRenderableWidget(builder.bounds(x, y, buttonWidth, BUTTON_HEIGHT).build()));
			x += buttonWidth + BUTTON_GAP;
		}
		return created;
	}

	private static Component onOff(boolean value) {
		return value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
	}
}
