package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class TrimFlushingJob extends FileJob {
	
	public TrimFlushingJob(ResourceManager resourceManager, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	public void execute() throws JobFailedException {
		
		reset();
		Connection conn = null;


		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);

			// Queue up the Data Reduction job
			User owner = JobManager.getJobOwner(dataSource, id);
			JobManager.addJob(conn, owner, FileInfo.JOB_CLASS_REDUCTION, parameters);
			DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_REDUCTION);
	
			conn.commit();

		} catch (Exception e) {
			DatabaseUtils.rollBack(conn);
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	private void reset() {
		// Clear all previous "Ignore" flags from gas_standards_data and raw_data
	}
}
