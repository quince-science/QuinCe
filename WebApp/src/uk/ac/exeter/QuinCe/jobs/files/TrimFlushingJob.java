package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.joda.time.DateTime;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.Calculation.TrimFlushingRecord;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class TrimFlushingJob extends FileJob {
	
	public TrimFlushingJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	public void executeFileJob(JobThread thread) throws JobFailedException {
		
		// Note that this job ignores interrupt calls!
		
		Connection conn = null;

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			reset(conn);
			
			Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);
			int preFlush = instrument.getPreFlushingTime();
			int postFlush = instrument.getPostFlushingTime();
			
			List<TrimFlushingRecord> trimFlushingRecords = RawDataDB.getTrimFlushingRecords(conn, fileId);

			long currentRunType = -1;
			int currentIndex = -1;
			DateTime preFlushLimit = null;

			
			while (currentIndex < trimFlushingRecords.size() - 1) {
				currentIndex++;
				TrimFlushingRecord record = trimFlushingRecords.get(currentIndex);
				if (record.getRunTypeId() != currentRunType) {
					
					if (currentIndex > 0 && postFlush > 0) {
						DateTime postFlushingLimit = trimFlushingRecords.get(currentIndex - 1).getDateTime().minusSeconds(postFlush);
						processPostFlushing(conn, trimFlushingRecords, currentIndex - 1, postFlushingLimit, currentRunType);
					}
					
					currentRunType = record.getRunTypeId();
					preFlushLimit = record.getDateTime().plusSeconds(preFlush);
				}
				
				// See if we're in the pre-flushing period
				if (preFlush > 0) {
					if (record.checkPreFlushing(preFlushLimit)) {
						RawDataDB.setGasStandardIgnoreFlag(conn, fileId, record);
						QCDB.setQCFlag(conn, fileId, record.getRow(), Flag.IGNORED, new FlushingQCMessage(record.getRow()));
					}
				}

				if (currentIndex % 100 == 0) {
					setProgress((double) currentIndex / (double) trimFlushingRecords.size() * 100.0);
				}
			}
			
			// Process the post-flushing for the final run type
			if (postFlush > 0) {
				DateTime postFlushingLimit = trimFlushingRecords.get(currentIndex).getDateTime().minusSeconds(postFlush);
				processPostFlushing(conn, trimFlushingRecords, currentIndex, postFlushingLimit, currentRunType);
			}
			
			// Queue up the Data Reduction job
			try {
				User owner = JobManager.getJobOwner(dataSource, id);
				JobManager.addJob(conn, owner, FileInfo.getJobClass(FileInfo.JOB_CODE_REDUCTION), parameters);
				DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_REDUCTION);
			} catch (RecordNotFoundException e) {
				// This means the file has been marked for deletion. No action is required.
			}
	
			conn.commit();

		} catch (Exception e) {
			DatabaseUtils.rollBack(conn);
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	private void processPostFlushing(Connection conn, List<TrimFlushingRecord> records, int finalIndex, DateTime postFlushLimit, long runTypeId) throws DatabaseException, MessageException {

		boolean finished = false;
		int currentIndex = finalIndex;
		
		while (!finished) {
			TrimFlushingRecord record = records.get(currentIndex);

			// If we've gone to a different run type, stop
			if (record.getRunTypeId() != runTypeId) {
				finished = true;
			} else {
				// If we're still in the flushing period, keep going
				finished = !record.checkPostFlushing(postFlushLimit);
				if (!finished) {
					// Record the ignored flag
					RawDataDB.setGasStandardIgnoreFlag(conn, fileId, record);
					QCDB.setQCFlag(conn, fileId, record.getRow(), Flag.IGNORED, new FlushingQCMessage(record.getRow()));
					
					// If we've hit the first record, stop
					if (currentIndex == 0) {
						finished = true;
					}
				}
			}
			
			currentIndex--;
		}
	}
	
	private void reset(Connection conn) throws DatabaseException {
		RawDataDB.clearGasStandardIgnoreFlags(conn, fileId);
		QCDB.resetQCFlagsByWoceFlag(conn, fileId, Flag.IGNORED, FlushingQCMessage.MESSAGE_TEXT);
	}
	
	@Override
	protected String getFinishState() {
		// Since we ignore interrupts, we always return FINISHED
		return FINISHED_STATUS;
	}
}
