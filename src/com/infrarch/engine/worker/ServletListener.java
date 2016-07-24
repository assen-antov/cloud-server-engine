package com.infrarch.engine.worker;

import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

/**
 * A background thread to run periodical tasks and finalize objects
 * at shutdown.
 * 
 * @author Ivan Peikov
 * @version 1.2, 07/2016
 */
public class ServletListener implements ServletContextListener {
	
	private static final int TASK_RUN_PERIOD = 1000;	// run tasks each second
	private Timer timer = new Timer("Engine Background Thread", true);
	private PeriodicalTask[] tasks = null;
	
	private final PeriodicalTask[] getTasks() {
		if (tasks == null) {
			if (ConfigurationManager.getInstance().isInitialized()) {
				tasks = new PeriodicalTask[] {
					FileManager.getInstance()
				};		
			}
		}
		return tasks;
	}
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		timer.scheduleAtFixedRate(new TimerTask() {			
			@Override
			public void run() {
				PeriodicalTask[] tasks = getTasks();
				if (tasks == null) return;
				
				for (PeriodicalTask task : tasks) {
					try {
						task.runPeriodically();
					} catch (Throwable e) {
						Logger.getLogger(getClass()).error("Failed while running periodical task", e);
					}
				}
			}
		}, 0, TASK_RUN_PERIOD);
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		Logger.getLogger(getClass()).info("Server shutting down...");
		timer.cancel();
		ConfigurationManager.destroyInstance();
	}
}
