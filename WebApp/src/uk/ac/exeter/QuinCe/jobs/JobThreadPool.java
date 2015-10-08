package uk.ac.exeter.QuinCe.jobs;

import java.sql.Connection;
import java.util.List;
import java.util.Stack;

import uk.ac.exeter.QuinCe.utils.MissingData;
import uk.ac.exeter.QuinCe.utils.MissingDataException;

/**
 * Thread pool for background jobs.
 * The pool has a maximum number of threads. However,
 * high priority jobs will always be given a thread,
 * so the true number of threads may occasionally be
 * larger. Any extra threads will be destroyed when
 * they are finished.
 * 
 * @author Steve Jones
 *
 */
public class JobThreadPool {

	/**
	 * The singleton instance of the thread pool
	 */
	private static JobThreadPool itsInstance = null;
	
	/**
	 * The maximum number of threads in the pool
	 */
	private int maxThreads;
	
	/**
	 * The pool of job threads
	 */
	private Stack<JobThread> threads = new Stack<JobThread>();
	
	/**
	 * Creates the thread pool and fills it with waiting job threads
	 * @param maxThreads The maximum number of threads in the pool
	 */
	private JobThreadPool(int maxThreads) {
		this.maxThreads = maxThreads;
		
		synchronized(threads) {
			for (int i = 0; i < maxThreads; i++) {
				threads.push(new JobThread(false));
			}
		}
	}
	
	/**
	 * Retrieves a job thread from the pool and configures it ready to execute a job.
	 * If there are no available threads in the stack, {@code null} is returned.
	 * 
	 * @param jobClass The job class to be executed
	 * @param parameters The parameters to be passed to the job
	 * @param threadName The name to give to the thread
	 * @return A configured job thread, or {@code null} if the thread stack is empty
	 * @throws JobClassNotFoundException If the specified job class cannot be found
	 * @throws InvalidJobClassException If the specified job class is not of the correct type or does not contain the correct methods
	 * @throws JobException If a problem is encountered while building the job object
	 * @throws InvalidJobParametersException If the parameters supplied to the job are invalid
	 * @throws MissingDataException If any of the required parameters are null
	*/
	public JobThread getJobThread(String jobClass, List<String> parameters, String threadName, Connection dbConnection) throws MissingDataException, JobClassNotFoundException, InvalidJobClassException, JobException, InvalidJobParametersException {
		
		MissingData.checkMissing(jobClass, "jobClass");
		MissingData.checkMissing(threadName, "threadName");
		MissingData.checkMissing(dbConnection, "dbConnection");
		
		JobThread thread = null;
		
		synchronized(threads) {
			if (!threads.isEmpty()) {
				thread = threads.pop();
			}
		}
		
		if (null != thread) {
			thread.setName(threadName);
			thread.setupJob(jobClass, parameters, dbConnection);
		}
		
		return thread;
	}
	
	/**
	 * Retrieves a job thread from the pool and configures it ready to execute a job.
	 * If there are no available threads in the stack, an overflow thread is created instead.
	 * The overflow thread will not be returned to the stack when the job is finished.
	 * This method should only be used for high priority jobs that cannot wait for a
	 * normal thread to become available.
	 *
	 * @param jobClass The job class to be executed
	 * @param parameters The parameters to be passed to the job
	 * @param threadName The name to give to the thread
	 * @param dbConnection A database connection for the job to use 
	 * @return A configured job thread
	 * @throws JobClassNotFoundException If the specified job class cannot be found
	 * @throws InvalidJobClassException If the specified job class is not of the correct type or does not contain the correct methods
	 * @throws JobException If a problem is encountered while building the job object
	 * @throws InvalidJobParametersException If the parameters supplied to the job are invalid
	 * @throws MissingDataException If any of the required parameters are null
	 */
	public JobThread getInstantJobThread(String jobClass, List<String> parameters, String threadName, Connection dbConnection) throws MissingDataException, JobClassNotFoundException, InvalidJobClassException, JobException, InvalidJobParametersException {

		JobThread thread = null;
		
		try {
			synchronized(threads) {
				if (!threads.isEmpty()) {
					thread = threads.pop();
				} else {
					thread = new JobThread(true);
				}
			}
			
			thread.setName(threadName);
			thread.setupJob(jobClass, parameters, dbConnection);
		} catch (JobClassNotFoundException|InvalidJobClassException e) {
			
			// Return the thread to the pool
			thread.reset();
			returnThread(thread);
			
			throw e;
		}
		
		return thread;
		
	}
	
	/**
	 * Returns a job thread to the stack ready for another job.
	 * If the thread is an overflow thread, it is not returned to the stack.
	 * @param thread The thread to be returned.
	 */
	public void returnThread(JobThread thread) {
		thread.reset();
		
		synchronized(threads) {
			if (!thread.isOverflowThread() && threads.size() <= maxThreads) {
				threads.push(thread);
			}
		}
	}
	
	/**
	 * Tests whether or not the thread pool has been initialised,
	 * i.e. whether an instance of the singleton exists.
	 * 
	 * @return {@code true} if the pool has been initialised; {@code false} if it has not.
	 */
	public static boolean isInitialised() {
		return !(null == itsInstance);
	}
	
	/**
	 * Initialise the job thread pool with the specified maximum number of threads
	 * Calling this method when the pool has already been initialised will replace the
	 * existing instance.
	 * 
	 * @param maxThreads The maximum number of threads in the pool
	 */
	public static void initialise(int maxThreads) {
		if (null == itsInstance) {
			itsInstance = new JobThreadPool(maxThreads);
		}
	}
	
	
	public static JobThreadPool getInstance() throws JobThreadPoolNotInitialisedException {
		if (null == itsInstance) {
			throw new JobThreadPoolNotInitialisedException();
		}
		
		return itsInstance;
	}
	
}
