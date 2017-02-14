package uk.ac.exeter.QuinCe.jobs;

import java.util.Collection;
import java.util.Stack;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.utils.MissingParamException;

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
	 * Indicates that a running thread was not found for a particular job
	 */
	public static final int THREAD_NOT_RUNNING = 0;
	
	/**
	 * Indicates that a running thread for a job was found, and the thread has been interrupted
	 */
	public static final int THREAD_INTERRUPTED = 1;
	
	/**
	 * The singleton instance of the thread pool
	 */
	private static JobThreadPool instance = null;
	
	/**
	 * The maximum number of threads in the pool
	 */
	private int maxThreads;
	
	/**
	 * The pool of job threads
	 */
	private Stack<JobThread> threads = new Stack<JobThread>();
	
	private Collection<JobThread> allocatedThreads = new TreeSet<JobThread>();
	
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
	 * @throws MissingParamException If any of the required parameters are null
	*/
	public JobThread getJobThread(Job job) throws MissingParamException {
		
		JobThread thread = null;
		
		synchronized(threads) {
			if (!threads.isEmpty()) {
				thread = threads.pop();
				allocatedThreads.add(thread);
			}
		}
		
		if (null != thread) {
			thread.setupJob(job);
		}
		
		
		return thread;
	}
	
	/**
	 * Returns the number of threads available in the pool
	 * @return The number of threads available in the pool
	 */
	public int getAvailableThreads() {
		return threads.size();
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
	 * @throws MissingParamException If any of the required parameters are null
	 */
	public JobThread getInstantJobThread(Job job) throws MissingParamException {

		JobThread thread = null;
		
		synchronized(threads) {
			if (!threads.isEmpty()) {
				thread = threads.pop();
			} else {
				thread = new JobThread(true);
			}
			
			allocatedThreads.add(thread);
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
			allocatedThreads.remove(thread);
			if (!thread.isOverflowThread() && threads.size() < maxThreads) {
				threads.push(new JobThread(false));
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
		return !(null == instance);
	}
	
	/**
	 * Initialise the job thread pool with the specified maximum number of threads
	 * Calling this method when the pool has already been initialised will replace the
	 * existing instance.
	 * 
	 * @param maxThreads The maximum number of threads in the pool
	 */
	public static void initialise(int maxThreads) throws InvalidThreadCountException {
		
		if (maxThreads <= 0) {
			throw new InvalidThreadCountException();
		}
		
		if (null == instance) {
			instance = new JobThreadPool(maxThreads);
		}
	}
	
	public static JobThreadPool getInstance() throws JobThreadPoolNotInitialisedException {
		if (null == instance) {
			throw new JobThreadPoolNotInitialisedException();
		}
		
		return instance;
	}
	
	public static void destroy() {
		instance = null;
	}
	
	public int getPoolThreadCount() {
		return threads.size();
	}
	
	public int getMaxThreads() {
		return maxThreads;
	}
	
	public int getRunningThreadsCount() {
		return allocatedThreads.size();
	}
	
	public int getOverflowThreadsCount() {
		int overflowThreads = 0;
		
		for (JobThread thread : allocatedThreads) {
			if (thread.isOverflowThread()) {
				overflowThreads++;
			}
		}
		
		return overflowThreads;
	}
	
	/**
	 * <p>Determines whether or not a thread with a given name is currently running
	 * as part of the job thread pool.</p>
	 * <p><b>N.B.</b> If a thread with the supplied name is running, but is not part of the job thread pool,
	 * it will not be detected.</p> 
	 * 
	 * @param threadName The name of the thread
	 * @return {@code true} if the thread is running in the job thread pool; {@code false} if it is not.
	 */
	public boolean isThreadRunning(String threadName) {
		boolean threadRunning = false;
		
		for (JobThread thread : allocatedThreads) {
			if (thread.getName().equals(threadName)) {
				threadRunning = true;
				break;
			}
		}
		
		return threadRunning;
	}

	/**
	 * Kill a job.
	 * 
	 * <p>
	 *   This checks the list of running threads to see if the specified job is currently running.
	 *   If it is, an interrupt signal is sent to it, indicating that it should close down. It is
	 *   up to the job to decide how it interprets this signal.
	 * </p>
	 * 
	 * <p>
	 *   If the job is not running, no action is taken. The return value of the method
	 *   indicates whether or not a running thread was found.
	 * </p>
	 * 
	 * @param jobId The job's database ID
	 * @return {@code #THREAD_INTERRUPTED} if a thread for the job was found; {@code #THREAD_NOT_RUNNING} if no thread was found.
	 */
	public int killJob(long jobId) {
		
		int result = THREAD_NOT_RUNNING;

		// We don't want the list of allocated threads changing underneath us
		synchronized (allocatedThreads) {
			
			for (JobThread thread : allocatedThreads) {
				if (thread.getId() == jobId) {
					// Interrupt the thread
					thread.interrupt();
					result = THREAD_INTERRUPTED;
					break;
				}
			}
		}
		
		return result;
	}
}
