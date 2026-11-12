package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VolumeStepTest {

	@Test
	void stepsUpAndDownByTenPercent() {
		assertEquals(0.6, VolumeStep.apply(0.5, 1), 1e-9);
		assertEquals(0.4, VolumeStep.apply(0.5, -1), 1e-9);
	}

	@Test
	void snapsOddValuesToTheGrid() {
		// El slider vanilla permite 0.53: el siguiente paso arriba es 0.6, no 0.63.
		assertEquals(0.6, VolumeStep.apply(0.53, 1), 1e-9);
		assertEquals(0.5, VolumeStep.apply(0.53, -1), 1e-9);
	}

	@Test
	void staysWithinBounds() {
		assertEquals(1.0, VolumeStep.apply(1.0, 1), 1e-9);
		assertEquals(0.0, VolumeStep.apply(0.0, -1), 1e-9);
	}

	@Test
	void formatsAsPercentage() {
		assertEquals(70, VolumeStep.percent(0.7));
		assertEquals(0, VolumeStep.percent(0.0));
	}
}
