package com.pedro.minemelody.hud;

import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.config.PlaylistConfig;
import com.pedro.minemelody.core.BannerPosition;
import com.pedro.minemelody.core.SlideAnimation;
import com.pedro.minemelody.core.SongTitle;
import com.pedro.minemelody.track.Track;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * Banner de "Sonando ahora" que entra deslizando por una esquina.
 * <pre>
 *  ┃ ♪ Sonando ahora
 *  ┃ [icono]  Sweden
 *  ┃          C418
 * </pre>
 * Se registra como elemento del HUD con la API de Fabric, así que Minecraft lo dibuja
 * en cada frame junto con la barra de vida, el chat, etc.
 */
public final class NowPlayingBanner implements HudElement {

	private static final SlideAnimation ANIMATION = new SlideAnimation(6000, 400);

	private static final int MARGIN = 6;
	private static final int PADDING = 6;
	private static final int ICON_SIZE = 16;
	private static final int HEIGHT = 38;
	private static final int ACCENT_WIDTH = 2;
	private static final int MIN_WIDTH = 120;

	private static final int BACKGROUND = 0xD0101418;
	private static final int ACCENT = 0xFF7CFC7C;
	private static final int HEADER_COLOR = 0xFF8A8F98;
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	private static final int ARTIST_COLOR = 0xFFB8BCC4;

	private final PlaylistConfig config;

	private Track track;
	private SongTitle song;
	private ItemStack icon;
	private long shownAt;
	private boolean errorReported;

	public NowPlayingBanner(PlaylistConfig config) {
		this.config = config;
	}

	/**
	 * Arranca la animación para esta canción. Sólo dentro de un mundo: en el menú principal
	 * no hay HUD (y los ítems ni siquiera están listos para dibujarse).
	 */
	public void show(Track track) {
		if (!config.showBanner || Minecraft.getInstance().level == null) return;
		try {
			this.song = SongTitle.parse(track.title().getString(), track.group().label().getString());
			this.icon = null; // se crea al dibujar
			this.shownAt = Util.getMillis();
			this.track = track;
		} catch (RuntimeException e) {
			reportOnce(e);
		}
	}

	/**
	 * Red de seguridad: el banner es un ADORNO. Si algo falla, lo apagamos y lo dejamos en
	 * el log, pero jamás tumbamos el juego por esto.
	 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (track == null) return;
		try {
			draw(graphics);
		} catch (RuntimeException e) {
			track = null;
			reportOnce(e);
		}
	}

	private void reportOnce(RuntimeException e) {
		if (!errorReported) {
			errorReported = true;
			MineMelodyClient.LOGGER.error("El banner de música falló; lo desactivo en esta sesión", e);
		}
	}

	private void draw(GuiGraphicsExtractor graphics) {

		long elapsed = Util.getMillis() - shownAt;
		if (ANIMATION.isFinished(elapsed)) {
			track = null;
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.hud.isHidden()) return; // F1: nada de HUD

		Font font = minecraft.font;
		Component header = Component.translatable("minemelody.banner.header");
		int textWidth = Math.max(font.width(header), Math.max(font.width(song.title()), font.width(song.artist())));
		int width = Math.max(MIN_WIDTH, ACCENT_WIDTH + PADDING + ICON_SIZE + PADDING + textWidth + PADDING);

		BannerPosition position = config.bannerPosition;
		int restingX = position.isLeft() ? MARGIN : graphics.guiWidth() - width - MARGIN;
		int y = position.isTop() ? MARGIN : graphics.guiHeight() - HEIGHT - MARGIN - 40; // 40: por encima del hotbar

		// visibility 0 -> fuera de pantalla por su lado; 1 -> en su sitio.
		int hiddenOffset = width + MARGIN;
		int offset = Math.round((1f - ANIMATION.visibility(elapsed)) * hiddenOffset);
		int x = position.isLeft() ? restingX - offset : restingX + offset;

		graphics.fill(x, y, x + width, y + HEIGHT, BACKGROUND);
		int accentX = position.isLeft() ? x : x + width - ACCENT_WIDTH;
		graphics.fill(accentX, y, accentX + ACCENT_WIDTH, y + HEIGHT, ACCENT);

		int contentX = (position.isLeft() ? x + ACCENT_WIDTH : x) + PADDING;
		graphics.text(font, header, contentX, y + 4, HEADER_COLOR);
		if (icon == null || icon.isEmpty()) {
			icon = track.icon(); // si todavía no se puede crear, devuelve EMPTY y probamos el próximo frame
		}
		if (!icon.isEmpty()) {
			graphics.item(icon, contentX, y + 16);
		}

		int textX = contentX + ICON_SIZE + PADDING;
		graphics.text(font, song.title(), textX, y + 15, TITLE_COLOR);
		graphics.text(font, song.artist(), textX, y + 26, ARTIST_COLOR);
	}
}
