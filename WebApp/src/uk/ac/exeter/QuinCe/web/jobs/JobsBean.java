package uk.ac.exeter.QuinCe.web.jobs;

import java.util.Map;

import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
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
	
	////////////// *** METHODS *** ///////////////////////
	
	public void updateSummary() {
		
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
}
