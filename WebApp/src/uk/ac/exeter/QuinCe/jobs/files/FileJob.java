package uk.ac.exeter.QuinCe.jobs.files;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

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
	
	public static final String FILE_ID_KEY = "FILE_ID";
	
	/**
	 * The ID of the data file
	 */
	protected long fileId;
	
	/**
	 * The instrument that the data file is associated with
	 */
	protected Instrument instrument = null;
	
	/**
	 * Constructs a job object that will operate on a data file
	 * The parameters must contain a single entry that is the database ID
	 * of the file to be processed. Any other parameters will be ignored.
	 * 
	 * @param resourceManager The system resource manager
	 * @param config The application configuration
	 * @param jobId The id of the job in the database
	 * @param parameters The job parameters, containing the file ID
	 * @throws InvalidJobParametersException If the parameters are not valid for the job
	 * @throws RecordNotFoundException 
	 * @throws DatabaseException 
	 */
	public FileJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
		
		// The file ID and instrument are extracted in the validateParameters method 
	}

	/**
	 * Check that the supplied file ID exists.
	 * It's not strictly a parameter, but it's a handy place to do it.
	 */
	protected void validateParameters() throws InvalidJobParametersException {
		if (!parameters.containsKey(FILE_ID_KEY)) {
			throw new InvalidJobParametersException("Data file not specified");
		}

		try {
			fileId = Long.parseLong(parameters.get(FILE_ID_KEY));
			instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);

			if (!DataFileDB.fileExists(dataSource, fileId)) {
				throw new InvalidJobParametersException("The data file " + fileId + " does not exist");
			}
		} catch (Exception e) {
			throw new InvalidJobParametersException("An unexpected error occurred: " + e.getMessage());
		}
	}
	
	@Override
	protected final void execute(JobThread thread) throws JobFailedException {
		try {
			boolean deleteFlag = DataFileDB.getDeleteFlag(dataSource, fileId);
			if (deleteFlag) {
				setFinishState(KILLED_STATUS);
			} else {
				executeFileJob(thread);
			}
		} catch (JobFailedException e) {
			throw e;
		} catch (Exception e) {
			throw new JobFailedException(id, "Error while performing checks for file job", e);
		}
	}
	
	protected abstract void executeFileJob(JobThread thread) throws JobFailedException;
}
