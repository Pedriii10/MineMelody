package com.pedro.minemelody.gui;

import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.config.PlaylistConfig;
import com.pedro.minemelody.core.Playlist;
import com.pedro.minemelody.core.PlaylistCodec;
import com.pedro.minemelody.core.PlaylistLibrary;
import com.pedro.minemelody.core.SearchQuery;
import com.pedro.minemelody.core.TimeFormat;
import com.pedro.minemelody.playback.PlaylistPlayer;
import com.pedro.minemelody.playback.PlaylistSoundInstance;
import com.pedro.minemelody.track.Track;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
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
 *  │ ◀ [Minar         ] ▶  + Nueva  Borrar  Dim. │
 *  │ Canciones (60)        │ Tu playlist (3)     │
 *  │ [Buscar...         ]  │ C418 - Sweden       │
 *  │ [Overworld] Sweden    │ C418 - Wet Hands    │
 *  │ [Disco] cat (bloq.)   │ ...                 │
 *  │ ...                   │ ...                 │
 *  ├────────────────────────────────────────────┤
 *  │ Añadir Quitar ▲ ▼ +Todas +★Favs Vaciar      │
 *  │ Playlist  Modo  Pausa  Discos  Banner  Esq. │
 *  │ ▶ Escuchar ★ Fav ⏭ Sig. Exportar Importar Listo │
 *  └────────────────────────────────────────────┘
 * </pre>
 * Cada cambio se guarda al instante: si el juego crashea, no perdés tu playlist.
 */
public final class PlaylistScreen extends Screen {

	private static final int[] GAP_OPTIONS = {0, 1, 3, 5, 10, 30, 60};
	/** Ciclo del botón de dimensión: null = suena en cualquiera. */
	private static final String[] DIMENSIONS = {null, "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
	private static final int INVALID_NAME_COLOR = 0xFFFF6B6B;
	private static final int VALID_NAME_COLOR = 0xFFE0E0E0;
	private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId();

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;
	private static final int DARK_GRAY = 0xFF707070;
	private static final int GREEN = 0xFF7CFC7C;
	private static final int RED = 0xFFFF6B6B;
	private static final int GOLD = 0xFFFFD166;
	private static final int PROGRESS_BACKGROUND = 0xFF3A3F47;

	private static final int MARGIN = 10;
	private static final int TITLE_Y = 6;
	private static final int NOW_PLAYING_Y = 16;
	private static final int PROGRESS_Y = 26;
	private static final int PROGRESS_HALF_WIDTH = 90;
	private static final int SELECTOR_Y = 30;
	private static final int LIST_TOP = 66;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;
	private static final int SEARCH_HEIGHT = 16;

	private final Screen parent;
	private final PlaylistConfig config = MineMelodyClient.config();
	private final PlaylistPlayer player = MineMelodyClient.player();

	private TrackListWidget catalogList;
	private TrackListWidget playlistList;
	private Button listenButton;
	private Button favoriteButton;
	private EditBox searchBox;
	private EditBox nameBox;

	/** Se guarda fuera del EditBox para no perderlo si la ventana cambia de tamaño (init se re-ejecuta). */
	private String searchText = "";
	private SearchQuery search = SearchQuery.of("");

	/** Última canción clicada en CUALQUIERA de las dos listas: es la que suena con "Escuchar". */
	private String clickedTrackId;
	/** Para refrescar las listas (y pintar de dorado) cuando cambia la canción que suena. */
	private String lastPlayingId;
	/** Para refrescar las filas cuando llegan duraciones nuevas del hilo de fondo. */
	private int lastDurationsVersion = -1;

	public PlaylistScreen(Screen parent) {
		super(Component.translatable("minemelody.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		player.catalog().refresh(minecraft.getSoundManager());

		addPlaylistSelector();

		int columnWidth = (width - MARGIN * 3) / 2;
		int listHeight = height - LIST_TOP - (BUTTON_HEIGHT + BUTTON_GAP) * 3 - MARGIN;

		searchBox = addRenderableWidget(new EditBox(font, MARGIN, LIST_TOP, columnWidth, SEARCH_HEIGHT,
				Component.translatable("minemelody.search.hint")));
		searchBox.setHint(Component.translatable("minemelody.search.hint"));
		searchBox.setMaxLength(64);
		searchBox.setValue(searchText);
		searchBox.setResponder(this::onSearchChanged);

		int catalogTop = LIST_TOP + SEARCH_HEIGHT + 4;
		catalogList = addRenderableWidget(new TrackListWidget(minecraft, MARGIN, catalogTop, columnWidth,
				listHeight - (catalogTop - LIST_TOP), this::onRowClicked, row -> addTrack(row.id())));
		playlistList = addRenderableWidget(new TrackListWidget(minecraft, MARGIN * 2 + columnWidth, LIST_TOP, columnWidth, listHeight,
				this::onRowClicked, row -> removeTrack(row.id())));

		int rowY = LIST_TOP + listHeight + MARGIN / 2;
		addButtonRow(rowY,
				button("minemelody.button.add", () -> addTrack(catalogList.selectedId())),
				button("minemelody.button.remove", () -> removeTrack(playlistList.selectedId())),
				button("minemelody.button.up", () -> moveSelected(-1))
						.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.up"))),
				button("minemelody.button.down", () -> moveSelected(1))
						.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.down"))),
				button("minemelody.button.add_all", this::addAll)
						.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.add_all"))),
				button("minemelody.button.add_favorites", this::addFavorites)
						.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.add_favorites"))),
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
		Button.Builder favorite = button("minemelody.button.favorite", this::toggleFavorite)
				.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.favorite")));
		List<Button> lastRow = addButtonRow(rowY,
				listen,
				favorite,
				button("minemelody.button.skip", () -> player.skip(minecraft)),
				button("minemelody.button.export", this::exportPlaylist)
						.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.export"))),
				button("minemelody.button.import", this::importPlaylist)
						.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.import"))),
				Button.builder(CommonComponents.GUI_DONE, b -> onClose()));
		listenButton = lastRow.get(0);
		favoriteButton = lastRow.get(1);

		refreshLists(null, null);
		updateContextButtons();
	}

	@Override
	public void tick() {
		updateContextButtons();

		String playing = player.currentSound(minecraft).map(sound -> sound.track().id().toString()).orElse(null);
		int durationsVersion = MineMelodyClient.durations().version();
		if (!Objects.equals(playing, lastPlayingId) || durationsVersion != lastDurationsVersion) {
			lastPlayingId = playing;
			lastDurationsVersion = durationsVersion;
			refreshLists(catalogList.selectedId(), playlistList.selectedId());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(font, title, width / 2, TITLE_Y, WHITE);
		graphics.centeredText(font, nowPlaying(), width / 2, NOW_PLAYING_Y, GREEN);
		extractProgressBar(graphics);
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

	// ---------------------------------------------------------------- playlists con nombre

	/** ◀ [nombre editable] ▶  + Nueva  Borrar  Dimensión */
	private void addPlaylistSelector() {
		int small = 20;
		int medium = 56;
		int dimensionWidth = 112;
		int fixed = small * 2 + medium * 2 + dimensionWidth + BUTTON_GAP * 5;
		int nameWidth = Math.max(80, width - MARGIN * 2 - fixed);
		int x = MARGIN;

		addRenderableWidget(Button.builder(Component.literal("◀"), b -> switchPlaylist(-1))
				.bounds(x, SELECTOR_Y, small, BUTTON_HEIGHT).build());
		x += small + BUTTON_GAP;

		nameBox = addRenderableWidget(new EditBox(font, x, SELECTOR_Y, nameWidth, BUTTON_HEIGHT,
				Component.translatable("minemelody.playlist.name")));
		nameBox.setMaxLength(PlaylistLibrary.MAX_NAME_LENGTH);
		nameBox.setValue(config.library.active().name());
		nameBox.setResponder(this::onNameChanged);
		nameBox.setTooltip(Tooltip.create(Component.translatable("minemelody.tooltip.playlist_name")));
		x += nameWidth + BUTTON_GAP;

		addRenderableWidget(Button.builder(Component.literal("▶"), b -> switchPlaylist(1))
				.bounds(x, SELECTOR_Y, small, BUTTON_HEIGHT).build());
		x += small + BUTTON_GAP;

		addRenderableWidget(button("minemelody.button.new_playlist", this::createPlaylist)
				.bounds(x, SELECTOR_Y, medium, BUTTON_HEIGHT).build());
		x += medium + BUTTON_GAP;

		Button delete = addRenderableWidget(button("minemelody.button.delete_playlist", this::deletePlaylist)
				.tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.delete_playlist")))
				.bounds(x, SELECTOR_Y, medium, BUTTON_HEIGHT).build());
		delete.active = config.library.playlists().size() > 1;
		x += medium + BUTTON_GAP;

		addRenderableWidget(toggle(this::dimensionLabel, "minemelody.tooltip.dimension", this::cycleDimension)
				.bounds(x, SELECTOR_Y, dimensionWidth, BUTTON_HEIGHT).build());
	}

	private void onNameChanged(String text) {
		boolean valid = config.library.renameActive(text);
		nameBox.setTextColor(valid ? VALID_NAME_COLOR : INVALID_NAME_COLOR);
		if (valid) config.save();
	}

	private void switchPlaylist(int direction) {
		config.library.cycle(direction);
		config.save();
		rebuildWidgets(); // cambia todo: nombre, canciones, dimensión...
	}

	private void createPlaylist() {
		config.library.create(Component.translatable("minemelody.playlist.default_name").getString());
		config.save();
		rebuildWidgets();
	}

	private void deletePlaylist() {
		Playlist active = config.library.active();
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed && config.library.deleteActive()) {
				player.resetQueue(active);
				config.save();
			}
			minecraft.gui.setScreen(this);
		},
				Component.translatable("minemelody.delete.confirm_title", active.name()),
				Component.translatable("minemelody.delete.confirm_message", active.tracks().size())));
	}

	private Component dimensionLabel() {
		String dimension = config.library.active().dimension();
		String key = dimension == null ? "minemelody.dimension.any" : "minemelody.dimension." + dimension.substring(dimension.indexOf(':') + 1);
		return Component.translatable("minemelody.button.dimension", Component.translatable(key));
	}

	private void cycleDimension() {
		String current = config.library.active().dimension();
		int index = 0;
		for (int i = 0; i < DIMENSIONS.length; i++) {
			if (java.util.Objects.equals(DIMENSIONS[i], current)) index = i;
		}
		config.library.bindActiveTo(DIMENSIONS[(index + 1) % DIMENSIONS.length]);
	}

	// ---------------------------------------------------------------- acciones

	private void onSearchChanged(String text) {
		searchText = text;
		search = SearchQuery.of(text);
		refreshLists(catalogList.selectedId(), playlistList.selectedId());
	}

	private void onRowClicked(TrackListWidget.Row row) {
		clickedTrackId = row.id();
		updateContextButtons();
	}

	private void listenToClicked() {
		clickableTrack().ifPresent(track -> player.playNow(minecraft, track));
	}

	/** La canción clicada, si existe y se puede escuchar (un disco bloqueado NO: sería hacer trampa). */
	private Optional<Track> clickableTrack() {
		if (clickedTrackId == null) return Optional.empty();
		return player.catalog().get(clickedTrackId).filter(track -> player.unlocks().isAvailable(minecraft, track));
	}

	/** Los botones que actúan sobre la canción clicada: se activan/cambian según cuál sea. */
	private void updateContextButtons() {
		if (listenButton != null) {
			listenButton.active = clickableTrack().isPresent();
		}
		if (favoriteButton != null) {
			favoriteButton.active = clickedTrackId != null;
			boolean isFavorite = clickedTrackId != null && config.favorites.contains(clickedTrackId);
			favoriteButton.setMessage(Component.translatable(
					isFavorite ? "minemelody.button.unfavorite" : "minemelody.button.favorite"));
		}
	}

	private void toggleFavorite() {
		if (clickedTrackId == null) return;
		if (!config.favorites.remove(clickedTrackId)) {
			config.favorites.add(clickedTrackId);
		}
		changed(playlistList.selectedId());
		updateContextButtons();
	}

	// ---------------------------------------------------------------- compartir

	private void exportPlaylist() {
		if (tracks().isEmpty()) {
			toast("minemelody.toast.export_empty");
			return;
		}
		minecraft.keyboardHandler.setClipboard(PlaylistCodec.encode(tracks()));
		toast("minemelody.toast.exported", tracks().size());
	}

	/**
	 * Importar REEMPLAZA la playlist, así que primero preguntamos. Perder una playlist armada
	 * con cariño por un clic sin querer es justo el tipo de cosa que hace odiar un mod.
	 */
	private void importPlaylist() {
		var imported = PlaylistCodec.decode(minecraft.keyboardHandler.getClipboard());
		if (imported.isEmpty()) {
			toast("minemelody.toast.import_invalid");
			return;
		}
		List<String> tracks = imported.get();
		if (tracks().isEmpty()) {
			applyImport(tracks);
			return;
		}
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) applyImport(tracks);
			minecraft.gui.setScreen(this);
		},
				Component.translatable("minemelody.import.confirm_title"),
				Component.translatable("minemelody.import.confirm_message", tracks().size(), tracks.size())));
	}

	private void applyImport(List<String> tracks) {
		tracks().clear();
		tracks().addAll(tracks);
		player.resetQueue(config.library.active());
		changed(null);
		long missing = tracks.stream().filter(id -> player.catalog().get(id).isEmpty()).count();
		toast(missing == 0 ? "minemelody.toast.imported" : "minemelody.toast.imported_missing", tracks.size(), missing);
	}

	private void toast(String messageKey, Object... args) {
		SystemToast.addOrUpdate(minecraft.gui.toastManager(), TOAST_ID,
				Component.translatable("minemelody.screen.title"), Component.translatable(messageKey, args));
	}

	private void addFavorites() {
		for (String id : config.favorites) {
			if (!tracks().contains(id) && player.catalog().get(id).isPresent()) {
				tracks().add(id);
			}
		}
		changed(null);
	}

	private void addTrack(String id) {
		if (id == null || tracks().contains(id)) return;
		tracks().add(id);
		changed(id);
	}

	private void addAll() {
		for (Track track : player.catalog().all()) {
			String id = track.id().toString();
			if (!tracks().contains(id)) tracks().add(id);
		}
		changed(null);
	}

	private void removeTrack(String id) {
		if (id == null) return;
		int index = tracks().indexOf(id);
		if (index < 0) return;
		tracks().remove(index);
		// Dejamos seleccionada la que quedó en ese lugar: así podés borrar varias seguidas.
		changed(tracks().isEmpty() ? null : tracks().get(Math.min(index, tracks().size() - 1)));
	}

	private void moveSelected(int direction) {
		String id = playlistList.selectedId();
		if (id == null) return;
		int from = tracks().indexOf(id);
		int to = from + direction;
		if (from < 0 || to < 0 || to >= tracks().size()) return;
		tracks().add(to, tracks().remove(from));
		changed(id);
	}

	private void clearPlaylist() {
		tracks().clear();
		player.resetQueue(config.library.active());
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

	/** La playlist que estás editando es la ACTIVA de la biblioteca. */
	private List<String> tracks() {
		return config.library.active().tracks();
	}

	// ---------------------------------------------------------------- filas

	private void refreshLists(String selectedCatalogId, String selectedPlaylistId) {
		List<TrackListWidget.Row> catalogRows = new ArrayList<>();
		for (Track track : player.catalog().all()) {
			if (search.matches(searchableText(track))) {
				catalogRows.add(catalogRow(track));
			}
		}
		catalogList.setRows(catalogRows, selectedCatalogId);

		List<TrackListWidget.Row> playlistRows = new ArrayList<>();
		for (int i = 0; i < tracks().size(); i++) {
			playlistRows.add(playlistRow(i + 1, tracks().get(i)));
		}
		playlistList.setRows(playlistRows, selectedPlaylistId);
	}

	/** Lo que se busca: grupo + título, y la palabra "favorita" si lo es (así "fav" las filtra). */
	private String searchableText(Track track) {
		String text = track.group().label().getString() + " " + track.title().getString();
		if (config.favorites.contains(track.id().toString())) {
			text += " " + Component.translatable("minemelody.search.favorite_keyword").getString();
		}
		return text;
	}

	private String star(String id) {
		return config.favorites.contains(id) ? "★ " : "";
	}

	private TrackListWidget.Row catalogRow(Track track) {
		String id = track.id().toString();
		boolean available = player.unlocks().isAvailable(minecraft, track);
		boolean inPlaylist = tracks().contains(id);

		StringBuilder text = new StringBuilder()
				.append(inPlaylist ? "✔ " : "")
				.append(star(id))
				.append('[').append(track.group().label().getString()).append("] ")
				.append(track.title().getString());
		if (!available) {
			text.append("  ").append(Component.translatable("minemelody.locked").getString());
		}

		int color = !available ? DARK_GRAY : id.equals(lastPlayingId) ? GOLD : inPlaylist ? GREEN : WHITE;
		return catalogList.new Row(id, text.toString(), duration(track), color);
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
		return playlistList.new Row(id, (id.equals(lastPlayingId) ? "♪ " : "") + star(id) + text, duration(track.get()), color);
	}

	/** "3:49" o null si todavía no la sabemos (se está leyendo en segundo plano). */
	private String duration(Track track) {
		var seconds = MineMelodyClient.durations().get(minecraft, track);
		return seconds.isPresent() ? TimeFormat.minutesSeconds(seconds.getAsDouble()) : null;
	}

	/** "1:23 / 3:49", o sólo "1:23" si la duración todavía no llegó. */
	private String elapsedText(Track track) {
		String elapsed = TimeFormat.minutesSeconds(MineMelodyClient.clock().elapsedSeconds());
		String total = duration(track);
		return total == null ? elapsed : elapsed + " / " + total;
	}

	private void extractProgressBar(GuiGraphicsExtractor graphics) {
		Optional<PlaylistSoundInstance> current = player.currentSound(minecraft);
		if (current.isEmpty()) return;
		var total = MineMelodyClient.durations().get(minecraft, current.get().track());
		if (total.isEmpty()) return;

		float progress = TimeFormat.progress(MineMelodyClient.clock().elapsedSeconds(), total.getAsDouble());
		int left = width / 2 - PROGRESS_HALF_WIDTH;
		int right = width / 2 + PROGRESS_HALF_WIDTH;
		graphics.fill(left, PROGRESS_Y, right, PROGRESS_Y + 2, PROGRESS_BACKGROUND);
		graphics.fill(left, PROGRESS_Y, left + Math.round((right - left) * progress), PROGRESS_Y + 2, GREEN);
	}

	private Component nowPlaying() {
		Optional<PlaylistSoundInstance> current = player.currentSound(minecraft);
		if (current.isPresent()) {
			Playlist from = current.get().playlist();
			Component line = from == null
					? Component.translatable("minemelody.screen.previewing", current.get().track().title())
					: Component.translatable("minemelody.screen.now_playing_from", current.get().track().title(), from.name());
			return Component.empty().append(line).append("   " + elapsedText(current.get().track()));
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
