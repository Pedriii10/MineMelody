package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OggDurationTest {

	private static final int RATE = 44_100;

	@Test
	void readsDurationFromFirstAndLastPage() throws IOException {
		byte[] ogg = ogg(RATE, new long[]{RATE * 90L}, 0);

		assertEquals(90.0, OggDuration.read(new ByteArrayInputStream(ogg)).orElseThrow(), 1e-6);
	}

	@Test
	void worksWithLongFilesWhereTheLastPageIsFarAway() throws IOException {
		// 3 MB de "audio" entre medio: el lector no puede cargar todo, tiene que ir al final.
		byte[] ogg = ogg(RATE, new long[]{RATE * 10L, RATE * 245L}, 3_000_000);

		assertEquals(245.0, OggDuration.read(new ByteArrayInputStream(ogg)).orElseThrow(), 1e-6);
	}

	@Test
	void usesOtherSampleRates() throws IOException {
		byte[] ogg = ogg(48_000, new long[]{48_000L * 60}, 1000);

		assertEquals(60.0, OggDuration.read(new ByteArrayInputStream(ogg)).orElseThrow(), 1e-6);
	}

	@Test
	void ignoresTrailingPageWithoutGranulePosition() throws IOException {
		// granule -1 = "en esta página no termina ningún paquete": hay que usar la anterior.
		byte[] ogg = ogg(RATE, new long[]{RATE * 30L, -1L}, 500);

		assertEquals(30.0, OggDuration.read(new ByteArrayInputStream(ogg)).orElseThrow(), 1e-6);
	}

	@Test
	void notAnOggFileGivesNothing() throws IOException {
		byte[] garbage = "esto no es un ogg, es un txt cualquiera".getBytes(StandardCharsets.UTF_8);

		assertTrue(OggDuration.read(new ByteArrayInputStream(garbage)).isEmpty());
	}

	@Test
	void emptyStreamGivesNothing() throws IOException {
		assertTrue(OggDuration.read(new ByteArrayInputStream(new byte[0])).isEmpty());
	}

	@Test
	void zeroSampleRateGivesNothing() throws IOException {
		OptionalDouble duration = OggDuration.read(new ByteArrayInputStream(ogg(0, new long[]{1000}, 0)));

		assertTrue(duration.isEmpty());
	}

	// ------------------------------------------------------------ fabricar un OGG de mentira

	/**
	 * Primera página con la cabecera Vorbis (de donde sale la frecuencia), {@code filler} bytes
	 * de relleno y una página por cada granule position de {@code granules}.
	 */
	private static byte[] ogg(int sampleRate, long[] granules, int filler) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ByteBuffer id = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
		id.put((byte) 1).put("vorbis".getBytes(StandardCharsets.US_ASCII));
		id.putInt(0);              // versión de vorbis
		id.put((byte) 2);          // canales
		id.putInt(sampleRate);     // ← lo que buscamos
		id.putInt(0).putInt(128_000).putInt(0); // bitrates
		id.put((byte) 0xB8).put((byte) 1);      // blocksizes + framing
		out.write(page(0, id.array()));

		out.write(new byte[filler]);

		for (long granule : granules) {
			out.write(page(granule, new byte[]{1, 2, 3, 4}));
		}
		return out.toByteArray();
	}

	private static byte[] page(long granule, byte[] data) {
		ByteBuffer page = ByteBuffer.allocate(27 + 1 + data.length).order(ByteOrder.LITTLE_ENDIAN);
		page.put("OggS".getBytes(StandardCharsets.US_ASCII));
		page.put((byte) 0);        // versión del formato
		page.put((byte) 0);        // tipo de cabecera
		page.putLong(granule);     // ← total de muestras hasta aquí
		page.putInt(1234);         // serial
		page.putInt(0);            // número de página
		page.putInt(0);            // CRC (no lo validamos)
		page.put((byte) 1);        // 1 segmento
		page.put((byte) data.length);
		page.put(data);
		return page.array();
	}
}
