package uk.ac.exeter.QuinCe.jobs;

import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Abstract class containing all the required parts for a backgorund job.
 * All jobs created for QuinCe must extend this class.
 * 
 * @author Steve Jones
 *
 */
public abstract class Job {
	
	/**
	 * Status indicating that the job has been constructed
	 * but has not yet been started
	 */
	public static final String WAITING_STATUS = "WAITING";
	
	/**
	 * Status indicating that the job is running
	 */
	public static final String RUNNING_STATUS = "RUNNING";
	
	/**
	 * Status indicating that the job has encountered an error
	 */
	public static final String ERROR_STATUS = "ERROR";
	
	/**
	 * Status indicating that the job has completed successfully
	 */
	public static final String FINISHED_STATUS = "FINISHED";
	
	/**
	 * The job's ID 
	 */
	protected long id = 0;
	
	/**
	 * Flag to indicate whether or not the job has been destroyed.
	 */
	private boolean destroyed = false;
	
	/**
	 * The database connection to be used by the job
	 */
	protected DataSource dataSource;
	
	/**
	 * The set of parameters passed to the job
	 */
	protected List<String> parameters;
	
	/**
	 * Constructs a job object, and validates the parameters passed to it
	 * @param dataSource A database connection
	 * @param id The id of the job in the database
	 * @param parameters The parameters for the job
	 * @throws InvalidJobParametersException If the parameters are not valid for the job
	 */
	public Job(DataSource dataSource, long id, List<String> parameters) throws MissingParamException, InvalidJobParametersException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		
		this.dataSource = dataSource;
		this.id = id;
		this.parameters = parameters;
		validateParameters();
	}
	
	/**
	 * Determines whether or not this job object has been destroyed.
	 * Destroyed objects should be discarded.
	 * @return {@code true} if this Job object has been destroyed; {@code false} otherwise.
	 */
	protected boolean isDestroyed() {
		return destroyed;
	}
	
	/**
	 * Performs the job tasks
	 */
	protected abstract void run() throws JobFailedException;
	
	/**
	 * Validate the parameters passed in to this job
	 * @throws InvalidJobParametersException If the parameters are invalid
	 */
	protected abstract void validateParameters() throws InvalidJobParametersException; 
	
	/**
	 * Set the progress for the job, as a percentage
	 * @param progress The progress
	 * @throws BadProgressException If the progress is not between 0 and 100
	 * @throws NoSuchJobException If the job is not in the database
	 * @throws DatabaseException If an error occurs while updating the database
	 */
	protected void setProgress(double progress) throws MissingParamException, BadProgressException, NoSuchJobException, DatabaseException {
		JobManager.setProgress(dataSource, id, progress);
	}
	
	/**
	 * Get the job's ID
	 * @return The job ID
	 */
	public long getID() {
		return id;
	}
	
	/**
	 * Destroys the job object, releasing the database connection.
	 */
	protected void destroy() {
		parameters = null;
		destroyed = true;
		dataSource = null;
	}
}
