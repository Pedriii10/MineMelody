package com.pedro.minemelody.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Una playlist con nombre. Opcionalmente "atada" a una dimensión: si estás en esa dimensión,
 * suena ésta en vez de la activa.
 * <p>
 * Es una clase normal (no un record) porque Gson la crea vacía y le rellena los campos.
 */
public final class Playlist {

	private String name;
	private List<String> tracks = new ArrayList<>();
	/** Id de la dimensión (ej: "minecraft:the_nether"), o null si suena en cualquiera. */
	private String dimension;

	public Playlist() {
	}

	public Playlist(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	/** Lista MUTABLE: la pantalla añade, quita y reordena directamente sobre ella. */
	public List<String> tracks() {
		return tracks;
	}

	public String dimension() {
		return dimension;
	}

	void rename(String name) {
		this.name = name;
	}

	void bindTo(String dimension) {
		this.dimension = dimension;
	}

	/** Arregla lo que pueda venir roto de un JSON editado a mano. */
	void repair(String fallbackName) {
		if (name == null || name.isBlank()) name = fallbackName;
		if (tracks == null) tracks = new ArrayList<>();
		tracks.removeIf(id -> id == null || id.isBlank());
	}
}
