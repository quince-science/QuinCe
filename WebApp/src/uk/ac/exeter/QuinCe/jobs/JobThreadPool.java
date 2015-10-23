package uk.ac.exeter.QuinCe.jobs;

import java.util.Stack;

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
	 * @param job The job to be executed
	 * @return A configured job thread, or {@code null} if the thread stack is empty
	 * @throws MissingDataException If any of the required parameters are null
	*/
	public JobThread getJobThread(Job job) throws MissingDataException {
		
		JobThread thread = null;
		
		synchronized(threads) {
			if (!threads.isEmpty()) {
				thread = threads.pop();
			}
		}
		
		if (null != thread) {
			thread.setupJob(job);
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
	 * @param job The job to be executed
	 * @return A configured job thread
	 * @throws MissingDataException If any of the required parameters are null
	 */
	public JobThread getInstantJobThread(Job job) throws MissingDataException {

		JobThread thread = null;
		
		synchronized(threads) {
			if (!threads.isEmpty()) {
				thread = threads.pop();
			} else {
				thread = new JobThread(true);
			}
		}
		
		thread.setupJob(job);
		
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
