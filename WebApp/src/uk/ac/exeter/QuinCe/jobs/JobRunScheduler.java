package uk.ac.exeter.QuinCe.jobs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Monitors the job queue and runs jobs if needed
 * @author Steve Jones
 *
 */
@WebListener
public class JobRunScheduler implements ServletContextListener, Runnable {

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
        scheduler.scheduleAtFixedRate(this, 30, 15, TimeUnit.SECONDS);
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
			ResourceManager resourceManager = ResourceManager.getInstance();
			try {
				JobManager.resetInterruptedJobs(resourceManager);
			} catch (Exception e) {
				// We don't mind if this fails.
			}
				
			try {
				JobManager.resetInterruptedJobs(resourceManager);
				
				boolean ranJob = true;
				while (ranJob) {
					ranJob = JobManager.startNextJob(resourceManager, resourceManager.getConfig());
				}
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
}
