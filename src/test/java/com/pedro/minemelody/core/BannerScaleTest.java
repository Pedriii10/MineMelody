package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BannerScaleTest {

	@Test
	void stepsThroughThePresetSizes() {
		assertEquals(1.25, BannerScale.step(1.0, 1), 1e-9);
		assertEquals(0.75, BannerScale.step(1.0, -1), 1e-9);
	}

	@Test
	void staysWithinTheSmallestAndBiggestSize() {
		assertEquals(BannerScale.MIN, BannerScale.step(BannerScale.MIN, -1), 1e-9);
		assertEquals(BannerScale.MAX, BannerScale.step(BannerScale.MAX, 1), 1e-9);
	}

	@Test
	void handEditedValuesSnapToTheNextPresetInThatDirection() {
		assertEquals(1.25, BannerScale.step(1.1, 1), 1e-9);
		assertEquals(1.0, BannerScale.step(1.1, -1), 1e-9);
	}

	@Test
	void sanitizeClampsOutOfRangeValues() {
		assertEquals(BannerScale.MAX, BannerScale.sanitize(50), 1e-9);
		assertEquals(BannerScale.MIN, BannerScale.sanitize(-1), 1e-9);
		assertEquals(1.0, BannerScale.sanitize(Double.NaN), 1e-9);
	}
}
