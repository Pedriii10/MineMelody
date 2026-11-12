package com.pedro.minemelody.mixin;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** El SoundManager es la fachada; el que de verdad maneja los canales de audio es el SoundEngine. */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {

	@Accessor("soundEngine")
	SoundEngine minemelody$getSoundEngine();
}
