package com.infrarch.engine.worker;

/**
 * A periodical task to be run by {@link ServletListener}.
 * 
 * @author Ivan Peikov
 * @verison 1.0
 */
public interface PeriodicalTask {
	
	/**
	 * A periodical task.
	 * 
	 * @throws Throwable
	 */
	void runPeriodically() throws Throwable;
}
