package com.pedro.minemelody.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Nos deja tocar la "cuenta regresiva" hasta la próxima canción.
 * Lo usamos para "Siguiente canción" y "Reproducir ahora".
 */
@Mixin(MusicManager.class)
public interface MusicManagerAccessor {

	@Accessor("nextSongDelay")
	void minemelody$setNextSongDelay(int ticks);

	@Accessor("currentMusic")
	SoundInstance minemelody$getCurrentMusic();
}
