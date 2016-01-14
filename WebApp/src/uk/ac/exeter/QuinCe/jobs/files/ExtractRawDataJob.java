package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
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
		
		reset();
		Connection conn = null;
		
		try {
			RawDataFile inData = DataFileDB.getRawDataFile(dataSource, config, fileId);
			List<List<String>> data = inData.getContents();

			Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);
			
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			int lineNumber = 0;
			for (List<String> line : data) {
				lineNumber++;
				RawDataDB.storeRawData(conn, instrument, fileId, lineNumber, line);
				if (lineNumber % 100 == 0) {
					//setProgress((double) lineNumber / (double) data.size() * 100.0);
				}
			}
			
			conn.commit();
		} catch (Exception e) {
			DatabaseUtils.rollBack(conn);
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	protected void reset() throws JobFailedException {
		try {
			RawDataDB.clearRawData(dataSource, fileId);
		} catch(DatabaseException e) {
			throw new JobFailedException(id, e);
		}
	}
}
