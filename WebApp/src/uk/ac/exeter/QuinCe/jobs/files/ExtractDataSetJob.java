package uk.ac.exeter.QuinCe.jobs.files;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Job to extract the data for a data set from the uploaded data files
 * @author Steve Jones
 *
 */
public class ExtractDataSetJob extends Job {

	/**
	 * The parameter name for the data set id
	 */
	public static final String ID_PARAM = "id";
	
	/**
	 * Initialise the job object so it is ready to run
	 * 
	 * @param resourceManager The system resource manager
	 * @param config The application configuration
	 * @param jobId The id of the job in the database
	 * @param parameters The job parameters, containing the file ID
	 * @throws InvalidJobParametersException If the parameters are not valid for the job
	 * @throws MissingParamException If any of the parameters are invalid
	 * @throws RecordNotFoundException If the job record cannot be found in the database
	 * @throws DatabaseException If a database error occurs
	 */
	public ExtractDataSetJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	@Override
	protected void execute(JobThread thread) throws JobFailedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void validateParameters() throws InvalidJobParametersException {
		// TODO Auto-generated method stub
		
	}

}
