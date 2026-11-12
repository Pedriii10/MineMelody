package com.pedro.musicplaylist.mixin;

import com.pedro.musicplaylist.gui.PlaylistScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Agrega el botón "Playlist de música..." al final de Opciones -> Música y sonidos.
 * <p>
 * Extendemos OptionsSubScreen (el padre real de SoundOptionsScreen) sólo para poder usar
 * su campo protegido {@code list}. El constructor nunca se ejecuta: Mixin copia los métodos
 * dentro de SoundOptionsScreen, no instancia esta clase.
 */
@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends OptionsSubScreen {

	protected SoundOptionsScreenMixin(Screen lastScreen, Options options, Component title) {
		super(lastScreen, options, title);
	}

	@Inject(method = "addOptions", at = @At("TAIL"))
	private void musicplaylist$addPlaylistButton(CallbackInfo ci) {
		Screen self = this;
		this.list.addBig(Button.builder(Component.translatable("musicplaylist.options.button"),
				button -> this.minecraft.gui.setScreen(new PlaylistScreen(self))).build());
	}
}
