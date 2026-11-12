package com.pedro.minemelody.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.OptionalDouble;

/**
 * Calcula cuánto dura un archivo OGG/Vorbis leyendo su formato binario.
 * <p>
 * Un .ogg es una serie de "páginas". Cada página empieza con la marca "OggS" y tiene una
 * cabecera de 27 bytes:
 * <pre>
 *  offset  tamaño  campo
 *   0       4      "OggS"
 *   4       1      versión (siempre 0)
 *   5       1      tipo de cabecera
 *   6       8      granule position  ← en Vorbis: TOTAL de muestras de audio hasta esta página
 *  14       4      serial
 *  18       4      número de página
 *  22       4      CRC
 *  26       1      cantidad de segmentos (N)
 *  27       N      tabla de segmentos (tamaño de cada uno) → después vienen los datos
 * </pre>
 * La PRIMERA página trae la cabecera de identificación de Vorbis, con la frecuencia de
 * muestreo (ej. 44100 muestras por segundo). La ÚLTIMA página dice cuántas muestras hay en
 * total. Duración = muestras / frecuencia. Todo en little-endian.
 * <p>
 * No cargamos el archivo entero en memoria: guardamos el principio y un "colchón" con lo
 * último que leímos, que es donde está la última página.
 */
public final class OggDuration {

	private static final byte[] CAPTURE_PATTERN = "OggS".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] VORBIS = "vorbis".getBytes(StandardCharsets.US_ASCII);
	private static final int PAGE_HEADER_SIZE = 27;
	/** Una página OGG mide como mucho ~64 KB: con guardar los últimos 128 KB, la última entra seguro. */
	private static final int TAIL_SIZE = 128 * 1024;
	private static final int HEAD_SIZE = 4096;

	private OggDuration() {
	}

	public static OptionalDouble read(InputStream in) throws IOException {
		byte[] head = in.readNBytes(HEAD_SIZE);
		int sampleRate = sampleRate(head);
		if (sampleRate <= 0) return OptionalDouble.empty();

		byte[] tail = readTail(in, head);
		long samples = lastGranulePosition(tail);
		if (samples <= 0) return OptionalDouble.empty();

		return OptionalDouble.of((double) samples / sampleRate);
	}

	/** Lee todo el resto del stream quedándose sólo con los últimos TAIL_SIZE bytes. */
	private static byte[] readTail(InputStream in, byte[] head) throws IOException {
		byte[] window = Arrays.copyOf(head, head.length);
		byte[] chunk = new byte[TAIL_SIZE];
		int read;
		while ((read = in.readNBytes(chunk, 0, chunk.length)) > 0) {
			// ventana = lo que había (recortado) + lo nuevo, nunca más de 2 * TAIL_SIZE
			int keep = Math.min(window.length, TAIL_SIZE);
			byte[] next = new byte[keep + read];
			System.arraycopy(window, window.length - keep, next, 0, keep);
			System.arraycopy(chunk, 0, next, keep, read);
			window = next;
		}
		return window;
	}

	/** Frecuencia de muestreo de la cabecera de identificación Vorbis (primera página). */
	private static int sampleRate(byte[] head) {
		if (head.length < PAGE_HEADER_SIZE || !startsWith(head, 0, CAPTURE_PATTERN)) return -1;

		int segments = head[26] & 0xFF;
		int dataStart = PAGE_HEADER_SIZE + segments;
		// Cabecera Vorbis: [1]["vorbis"][versión 4][canales 1][frecuencia 4]...
		if (head.length < dataStart + 16 || head[dataStart] != 1 || !startsWith(head, dataStart + 1, VORBIS)) {
			return -1;
		}
		return littleEndian(head, dataStart + 12).getInt();
	}

	/** Busca hacia atrás la última página con granule position válido (-1 = "sin dato"). */
	private static long lastGranulePosition(byte[] tail) {
		for (int i = tail.length - PAGE_HEADER_SIZE; i >= 0; i--) {
			if (startsWith(tail, i, CAPTURE_PATTERN) && tail[i + 4] == 0) {
				long granule = littleEndian(tail, i + 6).getLong();
				if (granule > 0) return granule;
			}
		}
		return -1;
	}

	private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
		if (offset < 0 || offset + prefix.length > data.length) return false;
		for (int i = 0; i < prefix.length; i++) {
			if (data[offset + i] != prefix[i]) return false;
		}
		return true;
	}

	private static ByteBuffer littleEndian(byte[] data, int offset) {
		return ByteBuffer.wrap(data, offset, data.length - offset).order(ByteOrder.LITTLE_ENDIAN);
	}
}
