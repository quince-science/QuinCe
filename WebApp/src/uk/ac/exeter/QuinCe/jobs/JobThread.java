package uk.ac.exeter.QuinCe.jobs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.MissingData;
import uk.ac.exeter.QuinCe.utils.MissingDataException;

/**
 * A thread object that is used to run a job.
 * @author Steve Jones
 *
 */
public class JobThread extends Thread {

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
	 * @throws MissingDataException If any of the required parameters are null
	 */
	protected void setupJob(String jobClass, List<String> parameters, Connection dbConnection) throws MissingDataException, JobClassNotFoundException, InvalidJobClassTypeException, JobException, InvalidJobParametersException {
		
		MissingData.checkMissing(jobClass, "jobClass");
		MissingData.checkMissing(dbConnection, "dbConnection");
		
		// Make sure the specified job class is of the correct type
		
		int classCheck = JobManager.checkJobClass(jobClass);
		switch (classCheck) {
		case JobManager.CLASS_CHECK_OK: {
			try {
				Class<?> jobClazz = Class.forName(jobClass);
				Constructor<?> jobConstructor = jobClazz.getConstructor(Connection.class, List.class);
	
				// Instantiate the Job object, which will automatically validate the parameters
				job = (Job) jobConstructor.newInstance(dbConnection, parameters);
			} catch (ClassNotFoundException e) {
				throw new JobClassNotFoundException(jobClass);
			} catch (NoSuchMethodException|InvocationTargetException|IllegalAccessException|InstantiationException e) {
				throw new InvalidJobClassTypeException(jobClass);
			}
			break;
		}
		case JobManager.CLASS_CHECK_NO_SUCH_CLASS: {
			throw new JobClassNotFoundException(jobClass);
		}
		default: {
			throw new InvalidJobClassTypeException(jobClass);
		}
		}
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
			// Run the job
			job.run();
		} catch (Exception e) {
			// The job should not throw any exceptions, but just in case...
		} finally {
			job.destroy();
			try {
				// Return ourselves to the thread pool
				JobThreadPool.getInstance().returnThread(this);
			} catch (JobThreadPoolNotInitialisedException e) {
				// If the thread pool is gone, what happens to this thread
				// is irrelevant.
			}
		}
		
	}
}
