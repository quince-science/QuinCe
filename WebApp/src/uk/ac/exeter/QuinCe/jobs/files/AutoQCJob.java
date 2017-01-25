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
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * <p>This {@link Job} class runs the configured set of QC routines on the records for a given data file.</p>
 * 
 * <p>Records that have already had their WOCE flag to {@code BAD} or {@code IGNORE} are excluded from the QC checks.</p>
 * 
 * <p>Once the QC has been completed, the QC Flag and QC Message are set. The QC flag will be set to {@code GOOD},
 *    {@code QUESTIONABLE} or {@code BAD}. In the latter two cases, the QC Message will contain details of the fault(s)
 *    that were found.
 * </p>
 * <p>If the QC flag was set to {@code GOOD}, the WOCE flag for the record will be set to {@code ASSUMED_GOOD}, to
 *    indicate that the software will assume that the record is good unless the user indicates otherwise. Otherwise,
 *    the WOCE Flag will be set to {@code NEEDS_FLAG}. The user will be required to manually choose a value for the WOCE
 *    Flag, either by accepting the suggestion from the QC job, or overriding the flag and choosing their own. The WOCE
 *    Comment will default to being identical to the QC Message, but this can also be changed if required.
 * </p>
 * 
 * <p>If the {@code AutoQCJob} has been run before, some WOCE Flags and Comments will have already been set by the user.
 *    These will be dealt with as follows: 

 *   <ul>
 *     <li>If the QC results have not changed from their previous value, the WOCE flags are kept unchanged</li>
 *     <li>If the QC results are different, the WOCE flag is replaced with {@code ASSUMED_GOOD} or {@code NEEDS_FLAG}
 *         as described above. The user will have to re-examine these records and set the WOCE Flag and Comment once more.</li>
 *   </ul>
 * </p>
 * 
 * @author Steve Jones
 *
 */
public class AutoQCJob extends FileJob {

	/**
	 * Initialise the job so it is ready to run
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
	public AutoQCJob(ResourceManager resourceManager, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void execute() throws JobFailedException {
		
		reset();
	
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
			
			DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_USER_QC);
			
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
