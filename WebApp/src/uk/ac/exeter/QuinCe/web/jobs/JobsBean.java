package uk.ac.exeter.QuinCe.web.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobSummary;
import uk.ac.exeter.QuinCe.jobs.JobThreadPool;
import uk.ac.exeter.QuinCe.jobs.test.TenSecondJob;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for handling jobs in the web application
 * 
 * @author Steve Jones
 *
 */
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
  private Map<String, Integer> jobCounts = null;

  /**
   * The complete list of jobs
   */
  private List<JobSummary> jobList = null;

  /**
   * The number of chunks in the test job
   */
  private int chunkCount = 1;

  /**
   * The ID of the job that is currently being worked on
   */
  private long chosenJob = 0;

  ////////////// *** METHODS *** ///////////////////////

  /**
   * Update the details shown on the job list page
   */
  public void update() {

    try {
      idleThreads = JobThreadPool.getInstance().getPoolThreadCount();
    } catch (Exception e) {
      e.printStackTrace();
      idleThreads = -1;
    }

    try {
      runningThreads = JobThreadPool.getInstance().getRunningThreadsCount();
    } catch (Exception e) {
      e.printStackTrace();
      runningThreads = -1;
    }

    try {
      overflowThreads = JobThreadPool.getInstance().getOverflowThreadsCount();
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

  /**
   * Submit a test job to the queue.
   * 
   * @see TenSecondJob
   */
  public void submitJob() {
    try {
      JobManager.addJob(ServletUtils.getDBDataSource(), getUser(),
        "uk.ac.exeter.QuinCe.jobs.test.TenSecondJob", getNewJobParams());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Finally, update everything
    update();
  }

  /**
   * Submit a test job to be run immediately
   * 
   * @see TenSecondJob
   */
  public void submitImmediateJob() {
    try {
      JobManager.addInstantJob(ServletUtils.getResourceManager(),
        ServletUtils.getAppConfig(), getUser(),
        "uk.ac.exeter.QuinCe.jobs.test.TenSecondJob", getNewJobParams());
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Finally, update everything
    update();
  }

  /**
   * Build the parameters for a test job
   * 
   * @return The test job parameters
   * @see #submitJob()
   * @see #submitImmediateJob()
   */
  private Map<String, String> getNewJobParams() {
    Map<String, String> parameters = new HashMap<String, String>(1);
    parameters.put(TenSecondJob.CHUNK_KEY, String.valueOf(chunkCount));
    return parameters;
  }

  /**
   * Manually start the next job in the queue
   * 
   * @see JobManager#startNextJob(uk.ac.exeter.QuinCe.web.system.ResourceManager,
   *      java.util.Properties)
   */
  public void runNext() {
    try {
      JobManager.startNextJob(ServletUtils.getResourceManager(),
        ServletUtils.getAppConfig());
    } catch (Exception e) {
      e.printStackTrace();
    }

    update();
  }

  /**
   * Place a job back in the queue to be run again
   * 
   * @see JobManager#requeueJob(javax.sql.DataSource, long)
   */
  public void requeue() {
    try {
      JobManager.requeueJob(ServletUtils.getDBDataSource(), chosenJob);
    } catch (Exception e) {
      e.printStackTrace();
    }

    update();
  }

  //////////////// *** GETTERS AND SETTERS *** /////////////////////////

  /**
   * Get the number of idle threads
   * 
   * @return The number of idle threads
   */
  public int getIdleThreads() {
    return idleThreads;
  }

  /**
   * Set the number of idle threads
   * 
   * @param idleThreads
   *          The number of idle threads
   */
  public void setIdleThreads(int idleThreads) {
    this.idleThreads = idleThreads;
  }

  /**
   * Get the maximum number of threads
   * 
   * @return The maximum number of threads
   */
  public int getMaxThreads() {
    return maxThreads;
  }

  /**
   * Set maximum number of threads
   * 
   * @param maxThreads
   *          The maximum number of threads
   */
  public void setMaxThreads(int maxThreads) {
    this.maxThreads = maxThreads;
  }

  /**
   * Get the number of running threads
   * 
   * @return The number of running threads
   */
  public int getRunningThreads() {
    return runningThreads;
  }

  /**
   * Set the number of running threads
   * 
   * @param runningThreads
   *          The number of running threads
   */
  public void setRunningThreads(int runningThreads) {
    this.runningThreads = runningThreads;
  }

  /**
   * Get the number of overflow threads
   * 
   * @return The number of overflow threads
   */
  public int getOverflowThreads() {
    return overflowThreads;
  }

  /**
   * Set the number of overflow threads
   * 
   * @param overflowThreads
   *          The number of overflow threads
   */
  public void setOverflowThreads(int overflowThreads) {
    this.overflowThreads = overflowThreads;
  }

  /**
   * Get the number of waiting jobs
   * 
   * @return The number of waiting jobs
   */
  public int getWaitingJobs() {
    return getJobCount(Job.WAITING_STATUS);
  }

  /**
   * Get the number of running jobs
   * 
   * @return The number of running jobs
   */
  public int getRunningJobs() {
    return getJobCount(Job.RUNNING_STATUS);
  }

  /**
   * Get the number of jobs with errors
   * 
   * @return The number of jobs with errors
   */
  public int getErrorJobs() {
    return getJobCount(Job.ERROR_STATUS);
  }

  /**
   * Get the number of jobs that have finished successfully
   * 
   * @return The number of jobs that have finished successfully
   */
  public int getFinishedJobs() {
    return getJobCount(Job.FINISHED_STATUS);
  }

  /**
   * Get the list of all jobs in the system
   * 
   * @return The list of jobs
   */
  public List<JobSummary> getJobList() {
    return jobList;
  }

  /**
   * Get the number of jobs with a specified status
   * 
   * @param status
   *          The status
   * @return The number of jobs with the specified status
   */
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

  /**
   * Get the number of chunks for the test job
   * 
   * @return The number of chunks for the test job
   * @see #submitJob()
   * @see #submitImmediateJob()
   * @see TenSecondJob
   */
  public int getChunkCount() {
    return chunkCount;
  }

  /**
   * Set the number of chunks for the test job
   * 
   * @param chunkCount
   *          The number of chunks for the test job
   * @see #submitJob()
   * @see #submitImmediateJob()
   * @see TenSecondJob
   */
  public void setChunkCount(int chunkCount) {
    this.chunkCount = chunkCount;
  }

  /**
   * Set the ID of the job that is currently being worked on
   * 
   * @param chosenJob
   *          The job ID
   */
  public void setChosenJob(long chosenJob) {
    this.chosenJob = chosenJob;
  }

  /**
   * Get the ID of the job that is currently being worked on
   * 
   * @return The job ID
   */
  public long getChosenJob() {
    return chosenJob;
  }
}
