package uk.ac.exeter.QuinCe.jobs;

import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.BackgroundTask;
import uk.ac.exeter.QuinCe.utils.BackgroundTaskException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Background task to remove old finished jobs from the system.
 * 
 * <p>
 *   Finished jobs more than 28 days old are removed. Jobs that
 *   were killed or had errors are not removed.
 * </p>
 * 
 * <p>
 *   The job will run once per day.
 * </p>
 * 
 * @author Steve Jones
 * @see JobManager#deleteFinishedJobs(DataSource, int)
 */
@WebListener
public class JobTidier extends BackgroundTask {

	@Override
	protected void doTask() throws BackgroundTaskException {
		try {
			JobManager.deleteFinishedJobs(ResourceManager.getInstance().getDBDataSource(), 28);
		} catch (Exception e) {
			throw new BackgroundTaskException(e);
		}
	}

	@Override
	protected long getRunInterval() {
		return 86400;
	}

}
