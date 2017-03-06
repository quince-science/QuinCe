package uk.ac.exeter.QuinCe.jobs.files;

import java.util.List;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A version of the Job class specifically for jobs operating on data files.
 * 
 * @author Steve Jones
 * @see Job
 */
public abstract class FileJob extends Job {
	
	/**
	 * The ID of the data file
	 */
	protected long fileId;
	
	/**
	 * The instrument to which the data file being processed belongs
	 */
	protected Instrument instrument = null;
	
	/**
	 * Construct a Job to run on a data file.
	 * 
	 * <p>
	 *   The first entry in the {@code parameters} list must be the
	 *   database ID of the file to be processed.
	 * </p>
	 *  
	 * @param resourceManager The system resource manager
	 * @param config The application configuration
	 * @param jobId The job's database ID
	 * @param parameters The job parameters
	 * @throws MissingParamException If any constructor parameters are missing
	 * @throws InvalidJobParametersException If the supplied job parameters are invalid
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any referenced database records are missing
	 */
	public FileJob(ResourceManager resourceManager, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		// File jobs have no parameters.
		super(resourceManager, config, jobId, parameters);
		instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);
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
