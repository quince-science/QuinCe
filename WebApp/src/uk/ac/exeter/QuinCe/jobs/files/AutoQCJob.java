package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.Routine;
import uk.ac.exeter.QCRoutines.config.RoutinesConfig;
import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QuinCe.data.QCRecord;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
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
	
		Connection conn = null;
		
		try {
			List<? extends DataRecord> qcRecords = QCDB.getQCRecords(dataSource, fileId, instrument);
			List<Routine> routines = RoutinesConfig.getInstance().getRoutines();
			
			for (Routine routine : routines) {
				routine.processRecords((List<DataRecord>) qcRecords);
			}
			
			// Record the messages from the QC in the database
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			for (DataRecord record : qcRecords) {
				
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
			
			conn.commit();
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}

	private void reset() throws JobFailedException {
		// Does nothing
	}
}
