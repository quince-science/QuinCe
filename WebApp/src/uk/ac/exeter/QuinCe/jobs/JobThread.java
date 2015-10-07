package uk.ac.exeter.QuinCe.jobs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.List;

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
	 * @throws InvalidJobClassException If the specified job class is not of the correct type
	 * @throws JobException If a problem is encountered while building the job object
	 * @throws InvalidJobParametersException If the parameters supplied to the job are invalid
	 */
	protected void setupJob(String jobClass, List<String> parameters, Connection dbConnection) throws JobClassNotFoundException, InvalidJobClassException, JobException, InvalidJobParametersException {
		
		// Make sure the specified job class is of the correct type
		try {
			Class<?> jobClazz = Class.forName(jobClass);
			Constructor<?> jobConstructor = null;
			
			boolean jobClassOK = true;
			
			// Does it inherit from the job class?
			if (!(jobClazz.getSuperclass().equals(Job.class))) {
				jobClassOK = false;
			} else {
				// Is there a constructor that takes the right parameters?
				// We also check that the List is designated to contain String objects
				jobConstructor = jobClazz.getConstructor(Connection.class, List.class);
				Type[] constructorGenericTypes = jobConstructor.getGenericParameterTypes();
				if (constructorGenericTypes.length != 1) {
					jobClassOK = false;
				} else {
					if (!(constructorGenericTypes[0] instanceof ParameterizedType)) {
						jobClassOK = false;
					} else {
						Type[] actualTypeArguments = ((ParameterizedType) constructorGenericTypes[0]).getActualTypeArguments();
						if (actualTypeArguments.length != 1) {
							jobClassOK = false;
						} else {
							Class<?> typeArgumentClass = (Class<?>) actualTypeArguments[0];
							if (!typeArgumentClass.equals(String.class)) {
								jobClassOK = false;
							}
						}
					}
				}
			}
			
			if (!jobClassOK) {
				throw new InvalidJobClassException();
			} else {
				
				// Instantiate the Job object, which will automatically validate the parameters
				job = (Job) jobConstructor.newInstance(dbConnection, parameters);
			}
		} catch (ClassNotFoundException e) {
			throw new JobClassNotFoundException(jobClass);
		} catch (NoSuchMethodException e) {
			throw new InvalidJobClassException();
		} catch (InvocationTargetException|IllegalAccessException|InstantiationException e) {
			throw new JobException("Unable to initialise job object", e);
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
	 * Start the thread and run the job
	 */
	public void run() {
		job.run();
	}
}
