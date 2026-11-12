package com.pedro.minemelody.track;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Una canción concreta (un archivo .ogg), no un "evento" de sonido.
 * <p>
 * Ojo con la diferencia: el evento "minecraft:music.game" agrupa ~30 canciones y Minecraft
 * elige una al azar. Un Track es UNA de esas canciones: "minecraft:music/game/sweden".
 *
 * @param id       ubicación del archivo, es la clave que guardamos en la config
 * @param eventId  evento al que pertenece (para discos: "minecraft:music_disc.cat")
 * @param sound    el sonido ya cargado por Minecraft, listo para reproducir
 * @param group    categoría para mostrar en la lista
 */
public record Track(Identifier id, Identifier eventId, Sound sound, TrackGroup group) {

	private static final String DISC_EVENT_PREFIX = "music_disc.";

	public boolean isDisc() {
		return group == TrackGroup.DISC;
	}

	/**
	 * Clave de traducción con "Artista - Título". Minecraft ya trae estas claves en sus
	 * archivos de idioma: "music.game.sweden" y, para discos, "jukebox_song.minecraft.cat".
	 */
	public String titleKey() {
		if (isDisc()) {
			String disc = eventId.getPath().substring(DISC_EVENT_PREFIX.length());
			return "jukebox_song." + eventId.getNamespace() + "." + disc;
		}
		return id.toShortLanguageKey();
	}

	public Component title() {
		String key = titleKey();
		if (Language.getInstance().has(key)) {
			return Component.translatable(key);
		}
		// Canciones de otros mods o resource packs sin traducción: mostramos el nombre del archivo.
		String path = id.getPath();
		return Component.literal(path.substring(path.lastIndexOf('/') + 1));
	}

	/**
	 * Icono para el banner: el propio disco si es un disco ("music_disc.cat" -> item
	 * "minecraft:music_disc_cat"), y un bloque musical para el resto.
	 */
	public ItemStack icon() {
		Item item = group == TrackGroup.NETHER ? Items.NETHERRACK : Items.NOTE_BLOCK;
		if (isDisc()) {
			String disc = eventId.getPath().substring(DISC_EVENT_PREFIX.length());
			Identifier itemId = Identifier.fromNamespaceAndPath(eventId.getNamespace(), "music_disc_" + disc);
			if (BuiltInRegistries.ITEM.containsKey(itemId)) {
				item = BuiltInRegistries.ITEM.getValue(itemId);
			}
		}
		// OJO: en el menú principal los ítems todavía no tienen sus "componentes" cargados
		// (se cargan al entrar a un mundo) y crear un ItemStack crashea el juego.
		if (!item.builtInRegistryHolder().areComponentsBound()) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item);
	}

	static boolean isDiscEvent(Identifier eventId) {
		return eventId.getPath().startsWith(DISC_EVENT_PREFIX);
	}
}
