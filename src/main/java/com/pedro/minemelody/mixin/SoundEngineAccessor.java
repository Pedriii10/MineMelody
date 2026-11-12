package com.pedro.minemelody.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Cada sonido que suena tiene su "canal" de OpenAL. Con este mapa encontramos el canal
 * de NUESTRA canción para pausar sólo ése (Minecraft sólo sabe pausar todos a la vez).
 */
@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {

	@Accessor("instanceToChannel")
	Map<SoundInstance, ChannelAccess.ChannelHandle> minemelody$getInstanceToChannel();
}
