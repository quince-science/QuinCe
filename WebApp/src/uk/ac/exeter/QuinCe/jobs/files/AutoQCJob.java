package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import uk.ac.exeter.QCRoutines.config.RoutinesConfig;
import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.routines.Routine;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.QCRecord;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class AutoQCJob extends FileJob {

	public AutoQCJob(ResourceManager resourceManager, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void executeFileJob(JobThread thread) throws JobFailedException {
		
		Connection conn = null;
		
		try {
			// This automatically filters out QC records that are already marked IGNORE or BAD
			List<? extends DataRecord> qcRecords = QCDB.getQCRecords(dataSource, resourceManager.getColumnConfig(), fileId, instrument);
			
			// Remove any existing QC flags and messages
			for (DataRecord record : qcRecords) {
				((QCRecord) record).clearQCData();
			}
			List<Routine> routines = RoutinesConfig.getInstance().getRoutines();
			
			for (Routine routine : routines) {
				routine.processRecords((List<DataRecord>) qcRecords);
			}
			
			// Record the messages from the QC in the database
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			for (DataRecord record : qcRecords) {
				
				if (thread.isInterrupted()) {
					break;
				}
				
				QCRecord qcRecord = (QCRecord) record;
				boolean writeRecord = false;
				
				int messageCount = qcRecord.getMessages().size();
				
				Flag previousQCFlag = QCDB.getQCFlag(conn, fileId, qcRecord.getLineNumber());
				if (previousQCFlag.equals(Flag.NOT_SET)) {
					writeRecord = true;
				}

				if (messageCount == 0) {
					if (!previousQCFlag.isGood()) {
						qcRecord.setQCFlag(Flag.GOOD);
						qcRecord.setWoceFlag(Flag.ASSUMED_GOOD);
						qcRecord.setWoceComment(null);
						writeRecord = true;
					} else {
						Flag woceFlag = qcRecord.getWoceFlag();
						if (!woceFlag.equals(Flag.ASSUMED_GOOD) && !woceFlag.equals(Flag.GOOD)) {
							qcRecord.setWoceFlag(Flag.ASSUMED_GOOD);
							qcRecord.setWoceComment(null);
							writeRecord = true;
						}
					}
				} else {
					
					// Compare the QC comment (which is the Rebuild Codes for the
					// messages) with the new rebuild codes. If they're the same,
					// take no action. Otherwise reset the QC & WOCE flags and comments
					boolean messagesMatch = true;
					List<Message> databaseMessages = QCDB.getQCMessages(conn, fileId, qcRecord.getLineNumber());
					if (databaseMessages.size() != qcRecord.getMessages().size()) {
						messagesMatch = false;
					} else {
						for (int i = 0; i < databaseMessages.size() && messagesMatch; i++) {
							Message databaseMessage = databaseMessages.get(i);
							boolean databaseMessageFound = false;
							for (Message recordMessage : qcRecord.getMessages()) {
								databaseMessageFound = recordMessage.equals(databaseMessage);
							}
							
							if (!databaseMessageFound) {
								messagesMatch = false;
							}
						}
					}
					
					if (!messagesMatch) {
						qcRecord.setWoceFlag(Flag.NEEDED);
						qcRecord.setWoceComment(qcRecord.getMessageSummaries());
						writeRecord = true;
					}
				}

				if (writeRecord) {
					QCDB.setQC(conn, fileId, (QCRecord) record);
				}
			}
			
			// If the thread was interrupted, undo everything
			if (thread.isInterrupted()) {
				conn.rollback();

				// Queue up a new data reduction job
				try {
					User owner = JobManager.getJobOwner(dataSource, id);
					JobManager.addJob(conn, owner, FileInfo.JOB_CLASS_REDUCTION, parameters);
					DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_REDUCTION);
					conn.commit();
				} catch (RecordNotFoundException e) {
					// This means the file has been marked for deletion. No action is required.
				}
			} else {
				DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_USER_QC);
				conn.commit();
			}
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
}
