package uk.ac.exeter.QuinCe.jobs;

import javax.servlet.annotation.WebListener;

import uk.ac.exeter.QuinCe.utils.BackgroundTask;
import uk.ac.exeter.QuinCe.utils.BackgroundTaskException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Monitors the job queue and runs jobs if needed
 * @author Steve Jones
 *
 */
@WebListener
public class JobRunScheduler extends BackgroundTask {

	protected void doTask() throws BackgroundTaskException {
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
			throw new BackgroundTaskException(e);
		}
	}

	@Override
	protected long getRunInterval() {
		return 15;
	}
}
