package uk.ac.exeter.QuinCe.jobs;

import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * A thread object that is used to run a job.
 * @author Steve Jones
 *
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
	 * Indicates whether or not this is an overflow thread.
	 * Overflow threads are not returned to the stack, but are destroyed
	 * once they have completed.
	 */
	private boolean overflowThread;
		
	/**
	 * Creates a job thread
	 * @param overflowThread Indicates whether or not this is an overflow thread
	 */
	public JobThread(boolean overflowThread) {
		this.overflowThread = overflowThread;
		setName(WAITING_THREAD_NAME);
	}
	
	/**
	 * Sets up the job that this thread will run.
	 * @param jobClass The class of the specific job type
	 * @param parameters The parameters to be passed to the job
	 * @param dbConnection A database connection for the job to use
	 * @throws JobClassNotFoundException If the specified job class does not exist
	 * @throws InvalidJobClassTypeException If the specified job class is not of the correct type
	 * @throws JobException If a problem is encountered while building the job object
	 * @throws InvalidJobParametersException If the parameters supplied to the job are invalid
	 * @throws MissingParamException If any of the required parameters are null
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
		job.destroy();
		job = null;
	}
	
	/**
	 * Checks whether or not this is an overflow thread, and therefore
	 * whether it should be destroyed when finished with or returned to the thread pool
	 * @return {@code true} if it is an overflow thread; {@code false} if it is not
	 */
	protected boolean isOverflowThread() {
		return overflowThread;
	}
	
	/**
	 * Start the thread and run the job.
	 * When finished the thread will return itself to the thread pool
	 */
	public void run() {
		try {
			setName(String.valueOf(job.getID()) + '_' + System.currentTimeMillis());
			
			// Run the job
			job.setFinishState(Job.FINISHED_STATUS);
			job.setProgress(0);
			job.logStarted(getName());
			job.execute(this);
			
			switch (job.getFinishState()) {
			case (Job.KILLED_STATUS):
			{
				job.logKilled();
				break;
			}
			case (Job.FINISHED_STATUS):
			{
				job.logFinished();
				break;
			}
			default: {
				throw new JobException("Invalid finished state (" + job.getFinishState() + ") set on job");
			}
			}
		} catch (Throwable e) {
			try {
				job.logError(e);
			} catch (Exception e2) {
				e.printStackTrace();
			}
		} finally {
			job.destroy();
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
	
	@Override
	public void interrupt() {
		// Do the standard thread interruption
		super.interrupt();
		
		// Set the finish state on the job to KILLED
		try {
			job.setFinishState(Job.KILLED_STATUS);
		} catch (JobException e) {
			e.printStackTrace();
		}
	}
}