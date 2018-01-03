package com.team1389.util;

import java.util.function.Supplier;

public interface Loopable {
	public default void init() {
	};

	public void update();

	public static void pollUntil(int millisBetweenPolls, Loopable loopable, Supplier<Boolean> finished) {
		loopable.init();
		while (!finished.get()) {
			loopable.update();
			try {
				Thread.sleep(millisBetweenPolls);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
