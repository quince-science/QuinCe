package uk.ac.exeter.QuinCe.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public abstract class BackgroundTask implements ServletContextListener, Runnable {

	private ScheduledExecutorService scheduler = null;
	
	private final Object lock = new Object();
	
	private boolean running = false;

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		if (null != scheduler) {
			scheduler.shutdownNow();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this, 30, getRunInterval(), TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		
		boolean doRun = false;
		
		synchronized(lock) {
			if (!running) {
				running = true;
				doRun = true;
			}
		}
		
		if (doRun) {
			try {
				doTask();
			} catch (Exception e) {
				// We don't do anything at the minute
				e.printStackTrace();
			} finally {
				synchronized(lock) {
					running = false;
				}
			}
		}
	}
	
	protected abstract void doTask() throws BackgroundTaskException;
	
	protected abstract long getRunInterval();
}
