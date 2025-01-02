package uk.ac.exeter.QuinCe.jobs;

import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A thread object that is used to run a job.
 */
public class JobThread extends Thread implements Comparable<JobThread> {

  /**
   * The name set on any thread that is in the stack waiting to run
   */
  private static final String WAITING_THREAD_NAME = "waiting";

  /**
   * The object that will run the job
   */
  private Job job;

  /**
   * Indicates whether or not this is an overflow thread. Overflow threads are
   * not returned to the stack, but are destroyed once they have completed.
   */
  private boolean overflowThread;

  /**
   * Creates a job thread
   *
   * @param overflowThread
   *          Indicates whether or not this is an overflow thread
   */
  public JobThread(boolean overflowThread) {
    this.overflowThread = overflowThread;
    setName(WAITING_THREAD_NAME);
  }

  /**
   * Sets up the job that this thread will run.
   *
   * @param job
   *          The job
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public void setupJob(Job job) throws MissingParamException {
    MissingParam.checkMissing(job, "job");
    this.job = job;
  }

  /**
   * Reset this job thread so it can be returned to the job pool.
   */
  protected void reset() {
    setName(WAITING_THREAD_NAME);
    if (null != job) {
      job.destroy();
    }
    job = null;
  }

  /**
   * Checks whether or not this is an overflow thread, and therefore whether it
   * should be destroyed when finished with or returned to the thread pool
   *
   * @return {@code true} if it is an overflow thread; {@code false} if it is
   *         not
   */
  protected boolean isOverflowThread() {
    return overflowThread;
  }

  /**
   * Start the thread and run the job. When finished the thread will return
   * itself to the thread pool
   */
  public void run() {
    try {
      setName(String.valueOf(job.getId()) + '_' + System.currentTimeMillis());

      while (null != job) {

        // Run the job
        job.setFinishState(Job.FINISHED_STATUS);
        job.setProgress(0);
        job.logStarted(getName());
        NextJobInfo nextJob = job.execute(this);

        switch (job.getFinishState()) {
        case (Job.KILLED_STATUS): {
          job.logKilled();
          job.destroy();

          // Clear the job so we drop out of the loop
          job = null;

          break;
        }
        case (Job.FINISHED_STATUS): {
          job.logFinished();
          job.destroy();

          if (null == nextJob) {
            job = null;
          } else {
            long nextJobId = JobManager.addJob(
              ResourceManager.getInstance().getDBDataSource(), job.owner,
              nextJob.jobClass, nextJob.properties);
            job = JobManager.getJob(ResourceManager.getInstance(),
              ResourceManager.getInstance().getConfig(), nextJobId);
            job.transferData = nextJob.transferData;
          }
          break;
        }
        default: {
          throw new JobException(
            "Invalid finished state (" + job.getFinishState() + ") set on job");
        }
        }
      }
    } catch (Throwable e) {
      try {
        job.logError(e);

        // Certain types of error are known to be transitory, so we can requeue
        // the jobs
        if (e instanceof JobFailedException) {
          Throwable cause = e.getCause();
          if (null != cause) {
            if (StringUtils.stackTraceToString(cause).contains("Deadlock")) {
              JobManager.setStatus(
                ResourceManager.getInstance().getDBDataSource(), job.getId(),
                Job.WAITING_STATUS);
            }
          }
        }
      } catch (Exception e2) {
        System.out
          .println("Job failed, but could not store error in database.");
        System.out.println("Job error:");
        ExceptionUtils.printStackTrace(e);
        System.out.println("Storage error:");
        ExceptionUtils.printStackTrace(e2);
      }
    } finally {
      setName(WAITING_THREAD_NAME);
      try {
        // Return ourselves to the thread pool
        JobThreadPool.getInstance().returnThread(this);
      } catch (JobThreadPoolNotInitialisedException e) {
        // If the thread pool is gone, what happens to this thread
        // is irrelevant.
      }
    }
  }

  @Override
  public int compareTo(JobThread o) {
    return Long.signum(getId() - o.getId());
  }

  /**
   * Stop the job immediately, and set its status is {@link Job#KILLED_STATUS}.
   */
  @Override
  public void interrupt() {
    // Do the standard thread interruption
    super.interrupt();

    // Set the finish state on the job to KILLED
    try {
      job.setFinishState(Job.KILLED_STATUS);
    } catch (JobException e) {
      ExceptionUtils.printStackTrace(e);
    }
  }
}
