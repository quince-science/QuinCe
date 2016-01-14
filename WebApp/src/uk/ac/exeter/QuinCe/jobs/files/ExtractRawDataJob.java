package uk.ac.exeter.QuinCe.jobs.files;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class ExtractRawDataJob extends FileJob {

	/**
	 * Creates the job
	 * @param dataSource A datasource
	 * @param config The application configuration
	 * @param jobId The job's database ID
	 * @param fileId The data file ID
	 * @throws MissingParamException If any parameters are missing
	 * @throws InvalidJobParametersException If the parameters are invalid
	 */
	public ExtractRawDataJob(DataSource dataSource, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException {
		super(dataSource, config, jobId, parameters);
	}

	@Override
	protected void execute() throws JobFailedException {
		try {
			RawDataFile inData = DataFileDB.getRawDataFile(dataSource, config, fileId);
			List<List<String>> data = inData.getContents();
			
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		}
	}
}
