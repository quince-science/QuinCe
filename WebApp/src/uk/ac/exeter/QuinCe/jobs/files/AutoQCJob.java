package uk.ac.exeter.QuinCe.jobs.files;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.Routine;
import uk.ac.exeter.QCRoutines.config.RoutinesConfig;
import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class AutoQCJob extends FileJob {

	public AutoQCJob(DataSource dataSource, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(dataSource, config, jobId, parameters);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void execute() throws JobFailedException {
		
		reset();
	
		try {
			List<? extends DataRecord> qcRecords = QCDB.getPreQCRecords(dataSource, fileId, instrument);
			List<Routine> routines = RoutinesConfig.getInstance().getRoutines();
			
			for (Routine routine : routines) {
				routine.processRecords((List<DataRecord>) qcRecords);
			}
			
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		}
	}

	private void reset() throws JobFailedException {
		try {
			QCDB.clearQCData(dataSource, fileId);
		} catch(DatabaseException e) {
			throw new JobFailedException(id, e);
		}

	}
}
