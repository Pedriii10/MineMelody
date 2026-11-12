package com.pedro.minemelody.hud;

import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.config.PlaylistConfig;
import com.pedro.minemelody.core.BannerMode;
import com.pedro.minemelody.core.BannerPosition;
import com.pedro.minemelody.core.SlideAnimation;
import com.pedro.minemelody.core.SongTitle;
import com.pedro.minemelody.core.TimeFormat;
import com.pedro.minemelody.mixin.MusicManagerAccessor;
import com.pedro.minemelody.playback.PlaylistSoundInstance;
import com.pedro.minemelody.track.Track;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalDouble;

/**
 * Banner de "Sonando ahora".
 * <pre>
 *  ┃ ♪ Sonando ahora      1:23 / 3:49
 *  ┃ [icono]  Sweden
 *  ┃          C418
 *  ┃ ▬▬▬▬▬▬▬▬▬▬▬▬───────────────────
 * </pre>
 * Tres modos: apagado, unos segundos al empezar cada canción, o siempre visible.
 * <p>
 * El TAMAÑO se aplica con la matriz de transformación (pose) de la interfaz: dibujamos todo
 * como si midiera lo normal y la matriz lo escala. Así no hay que recalcular cada medida a mano.
 */
public final class NowPlayingBanner implements HudElement {

	private static final SlideAnimation ANIMATION = new SlideAnimation(6000, 400);

	private static final int MARGIN = 6;
	private static final int PADDING = 6;
	private static final int ICON_SIZE = 16;
	private static final int HEIGHT = 44;
	private static final int ACCENT_WIDTH = 2;
	private static final int MIN_WIDTH = 150;
	private static final int HOTBAR_CLEARANCE = 40;

	private static final int BACKGROUND = 0xD0101418;
	private static final int ACCENT = 0xFF7CFC7C;
	private static final int HEADER_COLOR = 0xFF8A8F98;
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	private static final int ARTIST_COLOR = 0xFFB8BCC4;
	private static final int BAR_BACKGROUND = 0xFF3A3F47;

	private final PlaylistConfig config;

	/** La última canción que "anunciamos" (para la animación de entrada). */
	private Track announced;
	private long shownAt;
	private ItemStack icon = ItemStack.EMPTY;
	private Track iconFor;
	private boolean errorReported;

	public NowPlayingBanner(PlaylistConfig config) {
		this.config = config;
	}

	/**
	 * Avisa que empezó una canción: arranca la animación de entrada. Sólo dentro de un mundo:
	 * en el menú principal no hay HUD (y los ítems ni siquiera están listos para dibujarse).
	 */
	public void show(Track track) {
		if (config.bannerMode == BannerMode.OFF || Minecraft.getInstance().level == null) return;
		announced = track;
		shownAt = Util.getMillis();
	}

	/**
	 * Red de seguridad: el banner es un ADORNO. Si algo falla, lo apagamos y lo dejamos en
	 * el log, pero jamás tumbamos el juego por esto.
	 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (errorReported) return;
		try {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui.hud.isHidden()) return; // F1: nada de HUD

			switch (config.bannerMode) {
				case OFF -> {
				}
				case ON_START -> drawAnnounced(graphics);
				case ALWAYS -> drawAlways(graphics, minecraft);
			}
		} catch (RuntimeException e) {
			errorReported = true;
			MineMelodyClient.LOGGER.error("El banner de música falló; lo desactivo en esta sesión", e);
		}
	}

	/** Vista previa para la pantalla de ajustes: el banner tal cual, en su sitio y tamaño reales. */
	public void renderPreview(GuiGraphicsExtractor graphics, Track track) {
		if (track != null) {
			draw(graphics, track, 1f);
		}
	}

	private void drawAnnounced(GuiGraphicsExtractor graphics) {
		if (announced == null) return;
		long elapsed = Util.getMillis() - shownAt;
		if (ANIMATION.isFinished(elapsed)) {
			announced = null;
			return;
		}
		draw(graphics, announced, ANIMATION.visibility(elapsed));
	}

	private void drawAlways(GuiGraphicsExtractor graphics, Minecraft minecraft) {
		Track playing = playingTrack(minecraft);
		if (playing == null) return;

		// Si la acabamos de anunciar, entra deslizando; después se queda fija.
		long elapsed = Util.getMillis() - shownAt;
		boolean slidingIn = playing == announced && elapsed < ANIMATION.slideMillis();
		draw(graphics, playing, slidingIn ? ANIMATION.visibility(elapsed) : 1f);
	}

	/** La canción que suena ahora mismo, sea de la playlist o la música normal del juego. */
	private static Track playingTrack(Minecraft minecraft) {
		if (minecraft.getMusicManager() == null) return null;
		SoundInstance current = ((MusicManagerAccessor) minecraft.getMusicManager()).minemelody$getCurrentMusic();
		if (current instanceof PlaylistSoundInstance ours) return ours.track();
		if (current == null || current.getSound() == null) return null;
		return MineMelodyClient.player().findTrack(minecraft, current.getSound().getLocation()).orElse(null);
	}

	/**
	 * @param visibility 0 = escondido fuera de pantalla por su lado, 1 = en su sitio
	 */
	private void draw(GuiGraphicsExtractor graphics, Track track, float visibility) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;

		SongTitle song = SongTitle.parse(track.title().getString(), track.group().label().getString());
		Component header = Component.translatable("minemelody.banner.header");
		OptionalDouble total = MineMelodyClient.durations().get(minecraft, track);
		boolean isPlaying = track == playingTrack(minecraft);
		double elapsed = isPlaying ? MineMelodyClient.clock().elapsedSeconds() : 0;
		String time = timeText(elapsed, total);

		// ---- medidas SIN escalar
		int textWidth = Math.max(font.width(header) + 12 + font.width(time),
				Math.max(font.width(song.title()), font.width(song.artist())) + ICON_SIZE + PADDING);
		int width = Math.max(MIN_WIDTH, ACCENT_WIDTH + PADDING + textWidth + PADDING);

		// ---- posición en pantalla, con el tamaño YA escalado
		float scale = (float) config.bannerScale;
		int scaledWidth = Math.round(width * scale);
		int scaledHeight = Math.round(HEIGHT * scale);
		BannerPosition position = config.bannerPosition;
		int restingX = position.isLeft() ? MARGIN : graphics.guiWidth() - scaledWidth - MARGIN;
		int y = position.isTop() ? MARGIN : graphics.guiHeight() - scaledHeight - MARGIN - HOTBAR_CLEARANCE;
		int offset = Math.round((1f - visibility) * (scaledWidth + MARGIN));
		int x = position.isLeft() ? restingX - offset : restingX + offset;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		try {
			drawContent(graphics, font, track, song, header, time, width, position.isLeft(),
					total.isPresent() ? TimeFormat.progress(elapsed, total.getAsDouble()) : -1f);
		} finally {
			graphics.pose().popMatrix(); // SIEMPRE: si no, todo lo que se dibuje después saldría escalado
		}
	}

	/** Dibuja en coordenadas locales: (0,0) es la esquina del banner y nada está escalado. */
	private void drawContent(GuiGraphicsExtractor graphics, Font font, Track track, SongTitle song, Component header,
			String time, int width, boolean accentOnLeft, float progress) {
		graphics.fill(0, 0, width, HEIGHT, BACKGROUND);
		int accentX = accentOnLeft ? 0 : width - ACCENT_WIDTH;
		graphics.fill(accentX, 0, accentX + ACCENT_WIDTH, HEIGHT, ACCENT);

		int left = (accentOnLeft ? ACCENT_WIDTH : 0) + PADDING;
		int right = width - (accentOnLeft ? 0 : ACCENT_WIDTH) - PADDING;

		graphics.text(font, header, left, 4, HEADER_COLOR);
		graphics.text(font, time, right - font.width(time), 4, HEADER_COLOR);

		ItemStack stack = iconFor(track);
		if (!stack.isEmpty()) {
			graphics.item(stack, left, 15);
		}
		int textX = left + ICON_SIZE + PADDING;
		graphics.text(font, song.title(), textX, 15, TITLE_COLOR);
		graphics.text(font, song.artist(), textX, 26, ARTIST_COLOR);

		if (progress >= 0) {
			int barY = HEIGHT - 5;
			graphics.fill(left, barY, right, barY + 2, BAR_BACKGROUND);
			graphics.fill(left, barY, left + Math.round((right - left) * progress), barY + 2, ACCENT);
		}
	}

	/** El ItemStack se crea una vez por canción, y sólo cuando los ítems ya están listos. */
	private ItemStack iconFor(Track track) {
		if (track != iconFor || icon.isEmpty()) {
			icon = track.icon(); // EMPTY si todavía no se puede crear: probamos el próximo frame
			iconFor = track;
		}
		return icon;
	}

	private static String timeText(double elapsed, OptionalDouble total) {
		String now = TimeFormat.minutesSeconds(elapsed);
		return total.isPresent() ? now + " / " + TimeFormat.minutesSeconds(total.getAsDouble()) : now;
	}
}
