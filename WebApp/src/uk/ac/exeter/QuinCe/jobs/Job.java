package uk.ac.exeter.QuinCe.jobs;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Abstract class containing all the required parts for a background job. All
 * jobs created for QuinCe must extend this class.
 */
public abstract class Job {

  /**
   * Status indicating that the job has been constructed but has not yet been
   * started
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
   * Status indicating that the job was killed
   */
  public static final String KILLED_STATUS = "KILLED";

  /**
   * The job's ID
   */
  protected long id = 0;

  /**
   * The User ID of the job's owner.
   */
  protected User owner;

  /**
   * Flag to indicate whether or not the job has been destroyed.
   */
  private boolean destroyed = false;

  /**
   * The database connection to be used by the job
   */
  protected ResourceManager resourceManager;

  /**
   * The database connection to be used by the job
   */
  protected DataSource dataSource;

  /**
   * The application configuration
   */
  protected Properties config;

  /**
   * The set of parameters passed to the job
   */
  protected Properties properties;

  /**
   * Data store that can be passed between jobs.
   */
  protected HashMap<String, Object> transferData;

  /**
   * Indicates the state that caused the thread to complete.
   *
   * <p>
   * This will be one of {@link Job#FINISHED_STATUS} or
   * {@link Job#KILLED_STATUS}. The value determines how the thread indicates to
   * the rest of the application that it has finished.
   * </p>
   *
   * <p>
   * This only applies if the job completes through normal running. Exceptions
   * are handled directly by the parent {@link JobThread} method.
   * </p>
   */
  private String finishState = FINISHED_STATUS;

  /**
   * Constructs a job object and validates the parameters passed to it.
   *
   * @param resourceManager
   *          The application resource manager.
   * @param config
   *          The application global properties.
   * @param id
   *          The job's database ID.
   * @param owner
   *          The job's owner.
   * @param properties
   *          The parameters for the job.
   * @throws MissingParamException
   *           If any required parameters are missig.
   * @throws InvalidJobParametersException
   *           If any of the supplied job parameters are invalid.
   */
  public Job(ResourceManager resourceManager, Properties config, long id,
    User owner, Properties properties)
    throws MissingParamException, InvalidJobParametersException {

    MissingParam.checkMissing(resourceManager, "resourceManager");

    this.resourceManager = resourceManager;
    this.config = config;
    this.id = id;
    this.owner = owner;
    this.properties = properties;

    // Extract the data source into its own variable, since it's what we use
    // most
    this.dataSource = resourceManager.getDBDataSource();

    validateParameters();
  }

  /**
   * Determines whether or not this job object has been destroyed. Destroyed
   * objects should be discarded.
   *
   * @return {@code true} if this Job object has been destroyed; {@code false}
   *         otherwise.
   */
  protected boolean isDestroyed() {
    return destroyed;
  }

  /**
   * Performs the job tasks.
   *
   * @param thread
   *          The thread that will be running the job.
   * @throws JobFailedException
   *           If an error occurs during the job.
   * @return Information regarding the next Job to be created and run as soon as
   *         this Job is finished.
   */
  protected abstract NextJobInfo execute(JobThread thread)
    throws JobFailedException;

  /**
   * Validate the parameters passed in to this job.
   *
   * @throws InvalidJobParametersException
   *           If the parameters are invalid.
   */
  protected abstract void validateParameters()
    throws InvalidJobParametersException;

  /**
   * Set the progress for the job, as a percentage.
   *
   * @param progress
   *          The progress.
   * @throws BadProgressException
   *           If the progress is not between 0 and 100.
   * @throws NoSuchJobException
   *           If the job is not in the database.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   * @throws MissingParamException
   *           If any required parameters are missing in internal calls.
   */
  protected void setProgress(double progress) throws MissingParamException,
    BadProgressException, NoSuchJobException, DatabaseException {
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      JobManager.setProgress(conn, id, progress);
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while retrieving a database connection", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Log the fact that the job has been started in the appropriate locations.
   * Initially this is just in the job manager, but it can be extended by other
   * classes.
   *
   * @param threadName
   *          The thread name.
   * @throws MissingParamException
   *           If any of the parameters to the underlying commands are missing.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   * @throws NoSuchJobException
   *           If the job has disappeared from the system.
   */
  protected void logStarted(String threadName)
    throws MissingParamException, DatabaseException, NoSuchJobException {
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      JobManager.logJobStarted(conn, id, threadName);
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while retrieving a database connection", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Log the fact that the job has been finished in the appropriate locations.
   * Initially this is just in the job manager, but it can be extended by other
   * classes.
   *
   * @throws MissingParamException
   *           If any of the parameters to the underlying commands are missing.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   * @throws NoSuchJobException
   *           If the job has disappeared.
   */
  protected void logFinished()
    throws MissingParamException, DatabaseException, NoSuchJobException {
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      JobManager.logJobFinished(conn, id);
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while updating a job's status", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Log the fact that the job has been finished in the appropriate locations.
   * Initially this is just in the job manager, but it can be extended by other
   * classes.
   *
   * @throws MissingParamException
   *           If any of the parameters to the underlying commands are missing.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   * @throws NoSuchJobException
   *           If the job has disappeared.
   */
  protected void logKilled()
    throws MissingParamException, DatabaseException, NoSuchJobException {
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      JobManager.logJobKilled(conn, id);
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while updating a job's status", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Logs a job error to the appropriate locations.
   *
   * @param error
   *          The error.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   * @throws NoSuchJobException
   *           If the job has disappeared.
   * @throws MissingParamException
   *           If any parameters in called methods are missing.
   */
  protected void logError(Throwable error)
    throws DatabaseException, MissingParamException, NoSuchJobException {
    Connection conn = null;
    try {
      ExceptionUtils.printStackTrace(error);
      conn = dataSource.getConnection();
      JobManager.logJobError(conn, id, error);
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while updating a job's status", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Get the job's database ID.
   *
   * @return The job ID.
   */
  public long getID() {
    return id;
  }

  /**
   * Destroys the job object, releasing its database connection.
   */
  protected void destroy() {
    properties = null;
    destroyed = true;
    dataSource = null;
  }

  /**
   * Set the finish state for the job that this thread is running. Must be one
   * of {@link Job#FINISHED_STATUS} or {@link Job#KILLED_STATUS}.
   *
   * @param finishState
   *          The finish state of the job.
   * @throws JobException
   *           If the supplied finish state is invalid.
   * @see #finishState
   */
  protected void setFinishState(String finishState) throws JobException {

    if (!finishState.equals(Job.FINISHED_STATUS)
      && !finishState.equals(Job.KILLED_STATUS)) {
      throw new JobException(
        "Invalid finished state (" + finishState + ") set on job");
    } else {
      this.finishState = finishState;
    }
  }

  /**
   * Return the finish state of this job. See {@link #finishState}.
   *
   * @return The job's finish state.
   */
  protected String getFinishState() {
    return finishState;

  }

  /**
   * Get the human-readable name of this job.
   *
   * @return The job name.
   */
  public abstract String getJobName();

  protected Object getTransferData(String key) {
    return null == transferData ? null : transferData.get(key);
  }
}
