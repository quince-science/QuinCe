package uk.ac.exeter.QuinCe.data;

import java.util.List;
import java.util.TreeSet;

import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.BackgroundTask;
import uk.ac.exeter.QuinCe.utils.BackgroundTaskException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@WebListener
public class FileDeleteScheduler extends BackgroundTask {

	protected void doTask() throws BackgroundTaskException {
		
		try {
			DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
			List<Long> deletedFiles = DataFileDB.getFilesWithDeleteFlag(dataSource);
			
			if (deletedFiles.size() > 0) {
				TreeSet<Long> killedJobs = JobManager.killFileJobs(dataSource, deletedFiles);
				
				for (long deletedFile : deletedFiles) {
					if (!killedJobs.contains(deletedFile)) {
						DataFileDB.deleteFile(dataSource, ResourceManager.getInstance().getConfig(), deletedFile);
					}
				}
			}
		} catch (Exception e) {
			throw new BackgroundTaskException(e);
		}
	}
	
	@Override
	protected long getRunInterval() {
		return 900;
	}
}
