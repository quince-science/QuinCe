package uk.ac.exeter.QuinCe.jobs;

import javax.servlet.annotation.WebListener;

import uk.ac.exeter.QuinCe.utils.BackgroundTask;
import uk.ac.exeter.QuinCe.utils.BackgroundTaskException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

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
