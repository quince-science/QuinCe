package uk.ac.exeter.QuinCe.web.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobSummary;
import uk.ac.exeter.QuinCe.jobs.JobThreadPool;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class JobsBean extends BaseManagedBean {

	/**
	 * The number of threads in the thread pool
	 */
	private int idleThreads = 0;
	
	/**
	 * The maximum number of threads in the thread pool
	 */
	private int maxThreads = 0;
	
	/**
	 * The number of running threads (normal priority)
	 */
	private int runningThreads = 0;
	
	/**
	 * The number of running immediate-priority threads
	 */
	private int overflowThreads = 0;
	
	/**
	 * The number of jobs with different statuses
	 */
	private Map<String,Integer> jobCounts = null;
	
	/**
	 * The complete list of jobs
	 */
	private List<JobSummary> jobList = null;
	
	/**
	 * The number of chunks in the test job
	 */
	private int chunkCount = 1;
	
	private long chosenJob = 0;
	
	////////////// *** METHODS *** ///////////////////////
	
	public void update() {
		
		try {
			idleThreads = JobThreadPool.getInstance().getPoolThreadCount();
		} catch (Exception e) {
			e.printStackTrace();
			idleThreads = -1;
		}
		
		try {
			runningThreads = JobThreadPool.getInstance().getRunningThreads();
		} catch (Exception e) {
			e.printStackTrace();
			runningThreads = -1;
		}
		
		try {
			overflowThreads = JobThreadPool.getInstance().getOverflowThreads();
		} catch (Exception e) {
			e.printStackTrace();
			overflowThreads = -1;
		}
		
		try {
			maxThreads = JobThreadPool.getInstance().getMaxThreads();
		} catch (Exception e) {
			e.printStackTrace();
			maxThreads = -1;
		}
			
		try {
			jobCounts = JobManager.getJobCounts(ServletUtils.getDBDataSource());
		} catch (Exception e) {
			e.printStackTrace();
			jobCounts = null;
		}
		
		try {
			jobList = JobManager.getJobList(ServletUtils.getDBDataSource());
		} catch (Exception e) {
			e.printStackTrace();
			jobList = null;
		}
	}
	
	public void submitJob() {
		List<String> parameters = new ArrayList<String>(1);
		parameters.add(String.valueOf(chunkCount));
		
		try {
			JobManager.addJob(ServletUtils.getDBDataSource(), getUser(), "uk.ac.exeter.QuinCe.jobs.test.TenSecondJob", parameters);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Finally, update everything
		update();
	}
	
	public void submitImmediateJob() {
		List<String> parameters = new ArrayList<String>(1);
		parameters.add(String.valueOf(chunkCount));
		
		try {
			JobManager.addInstantJob(ServletUtils.getResourceManager(), ServletUtils.getAppConfig(), getUser(), "uk.ac.exeter.QuinCe.jobs.test.TenSecondJob", parameters);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Finally, update everything
		update();
	}
	
	public void runNext() {
		try {
			JobManager.startNextJob(ServletUtils.getResourceManager(), ServletUtils.getAppConfig());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		update();
	}
	
	public void requeue() {
		try {
			JobManager.requeueJob(ServletUtils.getDBDataSource(), chosenJob);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		update();
	}

	//////////////// *** GETTERS AND SETTERS *** /////////////////////////
	
	public int getIdleThreads() {
		return idleThreads;
	}

	public void setIdleThreads(int idleThreads) {
		this.idleThreads = idleThreads;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public int getRunningThreads() {
		return runningThreads;
	}

	public void setRunningThreads(int runningThreads) {
		this.runningThreads = runningThreads;
	}

	public int getOverflowThreads() {
		return overflowThreads;
	}

	public void setOverflowThreads(int overflowThreads) {
		this.overflowThreads = overflowThreads;
	}
	
	public int getWaitingJobs() {
		return getJobCount(Job.WAITING_STATUS);
	}
	
	public int getRunningJobs() {
		return getJobCount(Job.RUNNING_STATUS);
	}
	
	public int getErrorJobs() {
		return getJobCount(Job.ERROR_STATUS);
	}
	
	public int getFinishedJobs() {
		return getJobCount(Job.FINISHED_STATUS);
	}
	
	public List<JobSummary> getJobList() {
		return jobList;
	}
	
	private int getJobCount(String status) {
		int result = 0;
		
		if (null == jobCounts) {
			result = -1;
		} else {
			if (null != jobCounts.get(status)) {
				result = jobCounts.get(status);
			}
		}
			
		return result;
	}
	
	public int getChunkCount() {
		return chunkCount;
	}
	
	public void setChunkCount(int chunkCount) {
		this.chunkCount = chunkCount;
	}
	
	public void setChosenJob(long chosenJob) {
		this.chosenJob = chosenJob;
	}
	
	public long getChosenJob() {
		return chosenJob;
	}
}
