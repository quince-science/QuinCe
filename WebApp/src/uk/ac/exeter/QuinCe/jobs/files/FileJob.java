package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.BadProgressException;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.NoSuchJobException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * A version of the Job class specifically for
 * jobs operating on data files. It ensures that
 * the job details and progress in the job table
 * are updated along with those in the Job Manager.
 * 
 * @author Steve Jones
 *
 */
public abstract class FileJob extends Job {
	
	/**
	 * The ID of the 
	 */
	protected long fileId;
	
	/**
	 * Constructs a job object that will operate on a data file
	 * The parameters must contain a single entry that is the database ID
	 * of the file to be processed. Any other parameters will be ignored.
	 * 
	 * @param dataSource A database connection
	 * @param config The application configuration
	 * @param jobId The id of the job in the database
	 * @param parameters The job parameters, containing the file ID
	 * @throws InvalidJobParametersException If the parameters are not valid for the job
	 */
	public FileJob(DataSource dataSource, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException {
		// File jobs have no parameters.
		super(dataSource, config, jobId, parameters);
	}

	/**
	 * Check that the supplied file ID exists.
	 * It's not strictly a parameter, but it's a handy place to do it.
	 */
	protected void validateParameters() throws InvalidJobParametersException {
		
		try {
			fileId = Long.parseLong(parameters.get(0));
			
			if (!DataFileDB.fileExists(dataSource, fileId)) {
				throw new InvalidJobParametersException("The data file " + fileId + " does not exist");
			}
		} catch (Exception e) {
			throw new InvalidJobParametersException("An unexpected error occurred: " + e.getMessage());
		}
	}
	
	/**
	 * Log the fact that the job has been started in the appropriate locations.
	 * Initially this is just in the job manager, but it can be extended by other classes
	 * @throws MissingParamException If any of the parameters to the underlying commands are missing
	 * @throws DatabaseException If an error occurs while updating the database
	 * @throws NoSuchJobException If the job has disappeared.
	 */
	protected void logStarted() throws MissingParamException, DatabaseException, NoSuchJobException {
		System.out.println("Running job " + id);
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);

			JobManager.startJob(conn, id);
			DataFileDB.setJobStatus(conn, fileId, 0);
			
			conn.commit();
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("An error occurred while retrieving a database connection", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Logs a job error to the appropriate locations
	 * @param error The error
	 */
	protected void logError(Throwable error) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			JobManager.errorJob(conn, id, error);
			DataFileDB.setJobStatus(conn, fileId, FileInfo.STATUS_CODE_ERROR);
			
			conn.commit();
		} catch (Exception e) {
			DatabaseUtils.rollBack(conn);
			e.printStackTrace();
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}

	/**
	 * Set the progress for the job, as a percentage
	 * @param progress The progress
	 * @throws BadProgressException If the progress is not between 0 and 100
	 * @throws NoSuchJobException If the job is not in the database
	 * @throws DatabaseException If an error occurs while updating the database
	 */
	protected void setProgress(double progress) throws MissingParamException, BadProgressException, NoSuchJobException, DatabaseException {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			JobManager.setProgress(conn, id, progress);
			DataFileDB.setJobStatus(conn, fileId, (int) Math.floor(progress));
			
			conn.commit();
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("An error occurred while retrieving a database connection", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
}
