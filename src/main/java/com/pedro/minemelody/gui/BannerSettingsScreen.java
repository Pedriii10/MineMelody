package com.pedro.minemelody.gui;

import com.pedro.minemelody.MineMelodyClient;
import com.pedro.minemelody.config.PlaylistConfig;
import com.pedro.minemelody.core.BannerScale;
import com.pedro.minemelody.mixin.MusicManagerAccessor;
import com.pedro.minemelody.playback.PlaylistSoundInstance;
import com.pedro.minemelody.track.Track;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Ajustes del banner con VISTA PREVIA EN VIVO: cambiás algo y el banner real se dibuja en su
 * esquina y con su tamaño, encima de esta pantalla. Mejor que cualquier descripción.
 */
public final class BannerSettingsScreen extends Screen {

	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;

	private final Screen parent;
	private final PlaylistConfig config = MineMelodyClient.config();

	private Button sizeLabel;

	public BannerSettingsScreen(Screen parent) {
		super(Component.translatable("minemelody.banner_settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = (width - BUTTON_WIDTH) / 2;
		int y = height / 2 - (BUTTON_HEIGHT + GAP) * 2;

		addRenderableWidget(Button.builder(modeLabel(), b -> {
			config.bannerMode = config.bannerMode.cycle();
			changed(b, modeLabel());
		}).tooltip(Tooltip.create(Component.translatable("minemelody.tooltip.banner_mode")))
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		y += BUTTON_HEIGHT + GAP;
		addRenderableWidget(Button.builder(positionLabel(), b -> {
			config.bannerPosition = config.bannerPosition.cycle();
			changed(b, positionLabel());
		}).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

		y += BUTTON_HEIGHT + GAP;
		int small = BUTTON_HEIGHT;
		addRenderableWidget(Button.builder(Component.literal("−"), b -> resize(-1))
				.bounds(x, y, small, BUTTON_HEIGHT).build());
		sizeLabel = addRenderableWidget(Button.builder(sizeText(), b -> {
		}).bounds(x + small + GAP, y, BUTTON_WIDTH - (small + GAP) * 2, BUTTON_HEIGHT).build());
		sizeLabel.active = false; // es sólo un cartel
		addRenderableWidget(Button.builder(Component.literal("+"), b -> resize(1))
				.bounds(x + BUTTON_WIDTH - small, y, small, BUTTON_HEIGHT).build());

		y += BUTTON_HEIGHT + GAP * 3;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, height / 2 - (BUTTON_HEIGHT + GAP) * 2 - 24, WHITE);
		graphics.centeredText(font, Component.translatable("minemelody.banner_settings.preview_hint"),
				width / 2, height / 2 - (BUTTON_HEIGHT + GAP) * 2 - 12, GRAY);

		MineMelodyClient.banner().renderPreview(graphics, previewTrack());
	}

	@Override
	public void onClose() {
		config.save();
		minecraft.gui.setScreen(parent);
	}

	private void changed(Button button, Component label) {
		button.setMessage(label);
		config.save();
	}

	private void resize(int direction) {
		config.bannerScale = BannerScale.step(config.bannerScale, direction);
		sizeLabel.setMessage(sizeText());
		config.save();
	}

	/** La canción que suena; si no suena nada, una cualquiera para que se vea algo. */
	private Track previewTrack() {
		SoundInstance current = ((MusicManagerAccessor) minecraft.getMusicManager()).minemelody$getCurrentMusic();
		if (current instanceof PlaylistSoundInstance ours) return ours.track();
		if (current != null && current.getSound() != null) {
			var vanilla = MineMelodyClient.player().findTrack(minecraft, current.getSound().getLocation());
			if (vanilla.isPresent()) return vanilla.get();
		}
		List<Track> all = MineMelodyClient.player().catalog().all();
		return all.isEmpty() ? null : all.getFirst();
	}

	private Component modeLabel() {
		return Component.translatable("minemelody.button.banner_mode", Component.translatable(config.bannerMode.translationKey()));
	}

	private Component positionLabel() {
		return Component.translatable("minemelody.button.banner_position", Component.translatable(config.bannerPosition.translationKey()));
	}

	private Component sizeText() {
		return Component.translatable("minemelody.button.banner_size", (int) Math.round(config.bannerScale * 100));
	}
}
