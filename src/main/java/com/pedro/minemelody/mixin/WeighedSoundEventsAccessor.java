package com.pedro.minemelody.mixin;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * WeighedSoundEvents guarda sus canciones en un campo PRIVADO llamado "list".
 * Un @Accessor genera un getter en tiempo de carga para que podamos leerlo.
 */
@Mixin(WeighedSoundEvents.class)
public interface WeighedSoundEventsAccessor {

	@Accessor("list")
	List<Weighted<Sound>> minemelody$getList();
}
