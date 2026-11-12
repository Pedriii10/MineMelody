package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatTest {

	@Test
	void formatsMinutesAndSeconds() {
		assertEquals("0:00", TimeFormat.minutesSeconds(0));
		assertEquals("0:09", TimeFormat.minutesSeconds(9.4));
		assertEquals("3:05", TimeFormat.minutesSeconds(185));
		assertEquals("12:00", TimeFormat.minutesSeconds(720));
	}

	@Test
	void truncatesInsteadOfRoundingUp() {
		// 59.9 s todavía es "0:59": si redondeáramos, el contador marcaría 1:00 antes de tiempo.
		assertEquals("0:59", TimeFormat.minutesSeconds(59.9));
	}

	@Test
	void negativeIsZero() {
		assertEquals("0:00", TimeFormat.minutesSeconds(-3));
	}

	@Test
	void progressIsClampedBetweenZeroAndOne() {
		assertEquals(0.5f, TimeFormat.progress(30, 60), 1e-6);
		assertEquals(1f, TimeFormat.progress(90, 60), 1e-6);
		assertEquals(0f, TimeFormat.progress(10, 0), 1e-6);
	}
}
