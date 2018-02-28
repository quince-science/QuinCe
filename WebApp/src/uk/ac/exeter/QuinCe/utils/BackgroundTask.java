package uk.ac.exeter.QuinCe.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * QuinCe has a number of maintenance tasks that run on a periodic basis.
 *
 * This abstract class provides a framework for those tasks, providing scheduling
 * details and ensuring that only one instance of a given task is running concurrently.
 *
 * @author Steve Jones
 */
public abstract class BackgroundTask implements ServletContextListener, Runnable {

	/**
	 * The scheduler for the task.
	 */
	private ScheduledExecutorService scheduler = null;

	/**
	 * A simple lock object for use when starting tasks.
	 * It is used when setting the {@link #running} status of the task
	 * to ensure that only one instance of a given task can be started.
	 */
	private final Object lock = new Object();

	/**
	 * Indicates whether or not this task is running.
	 */
	private boolean running = false;

	/**
	 * Shut down the scheduler for the task when the application is shut down
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		if (null != scheduler) {
			scheduler.shutdownNow();
		}
	}

	/**
	 * Initialise the scheduler to run the task.
	 * All tasks are initially delayed for 30 seconds to allow
	 * the application startup to complete. After that they
	 * are run at a regular interval specified by {@link #getRunInterval()}.
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this, 30, getRunInterval(), TimeUnit.SECONDS);
	}

	/**
	 * Runs the task. Sets the {@link #running} flag to ensure that
	 * only one instance of the task can run at any one time.
	 */
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

	/**
	 * Perform the task actions
	 * @throws BackgroundTaskException If an error occurs in the task
	 */
	protected abstract void doTask() throws BackgroundTaskException;

	/**
	 * Returns the amount of time (in seconds) to wait before
	 * running the task again.
	 * @return The number of seconds to wait between task runs
	 */
	protected abstract long getRunInterval();
}
