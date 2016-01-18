package uk.ac.exeter.QuinCe.jobs.files;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
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
}
