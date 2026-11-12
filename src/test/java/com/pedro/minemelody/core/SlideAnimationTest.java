package com.pedro.minemelody.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlideAnimationTest {

	// 6 s en total, 400 ms para entrar y 400 ms para salir.
	private final SlideAnimation animation = new SlideAnimation(6000, 400);

	@Test
	void startsHidden() {
		assertEquals(0f, animation.visibility(0), 0.0001f);
	}

	@Test
	void isFullyVisibleInTheMiddle() {
		assertEquals(1f, animation.visibility(3000), 0.0001f);
	}

	@Test
	void slidesInGradually() {
		float early = animation.visibility(100);
		float later = animation.visibility(300);

		assertTrue(early > 0f && early < later && later < 1f);
	}

	@Test
	void slidesOutAtTheEnd() {
		assertTrue(animation.visibility(5800) < 1f);
		assertEquals(0f, animation.visibility(6000), 0.0001f);
	}

	@Test
	void isFinishedAfterTheDuration() {
		assertTrue(animation.isFinished(6000));
		assertTrue(!animation.isFinished(5999));
	}

	@Test
	void negativeTimeIsHidden() {
		assertEquals(0f, animation.visibility(-50), 0.0001f);
	}
}
