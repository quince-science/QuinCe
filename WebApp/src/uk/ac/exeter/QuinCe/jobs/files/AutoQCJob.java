package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QCRoutines.config.RoutinesConfig;
import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.routines.Routine;
import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.FileInfo;
import uk.ac.exeter.QuinCe.data.QC.QCDB;
import uk.ac.exeter.QuinCe.data.QC.QCRecord;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.jobs.NoSuchJobException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * <p>
 *   This {@link Job} class runs a set of QC routines on the records for a given data file.
 * </p>
 * 
 * <p>
 *   This job can be run more than once during file processing, since certain QC routines
 *   are more suited to different stages. For example, certain QC failures will mean that
 *   data reduction cannot be completed (e.g. missing required values), while others merely
 *   indicate that the calculated values may not be trustworthy. The job's parameters
 *   will indicate where the calculations are in a file's processing, and which job
 *   should be run next.
 * </p>
 * 
 * <p>
 *   Records that have already had their WOCE flag to {@link Flag#FATAL}, {@link Flag#BAD} or {@link Flag#IGNORED}
 *   are excluded from the QC checks.
 * </p>
 * 
 * <p>
 *   Once the QC has been completed, the QC Flag and QC Message are set. The QC flag will be set to {@link Flag#GOOD},
 *   {@link Flag#QUESTIONABLE} or {@link Flag#BAD}. In the latter two cases, the QC Message will contain details of the fault(s)
 *   that were found.
 * </p>
 * 
 * <p>
 *   If the QC flag was set to {@link Flag#GOOD}, the WOCE flag for the record will be set to {@link Flag#ASSUMED_GOOD},
 *   to indicate that the software will assume that the record is good unless the user overrides it. Otherwise
 *   the WOCE Flag will be set to {@link Flag#NEEDED}. The user will be required to manually choose a value for the WOCE
 *   Flag, either by accepting the suggestion from the QC job, or overriding the flag and choosing their own. The WOCE
 *   Comment will default to being identical to the QC Message, but this can also be changed if required.
 * </p>
 * 
 * <p>
 *   If the {@code AutoQCJob} has been run before, some WOCE Flags and Comments will have already been set by the user.
 *   These will be dealt with as follows: 
 * </p>
 * 
 * <ul>
 *   <li>
 *     If the QC results have not changed from their previous value, the WOCE flags are kept unchanged.
 *   </li>
 *   <li>
 *     If the QC results are different, the WOCE flag is replaced with {@link Flag#ASSUMED_GOOD} or {@link Flag#NEEDED}
 *     as described above. The user will have to re-examine these records and set the WOCE Flag and Comment once more.
 *   </li>
 * </ul>
 * 
 * @author Steve Jones
 * @see Flag
 * @see Message
 */
public class AutoQCJob extends FileJob {

	/**
	 * The name of the job parameter that contains the path to the configuration
	 * for the QC Routines
	 * @see RoutinesConfig
	 */
	private static final String PARAM_ROUTINES_CONFIG = "ROUTINES_CONFIG";
	
	/**
	 * The job code for the job that must be executed after the QC has completed.
	 * @see FileInfo
	 */
	private static final String PARAM_NEXT_JOB_CODE = "NEXT_CODE";
	
	/**
	 * The job code for the job that must be executed if the QC is interrupted
	 * @see FileInfo
	 */
	private static final String PARAM_INTERRUPTED_JOB_CODE = "INTERRUPTED_CODE";
	
	/**
	 * Constructor that allows the {@link JobManager} to create an instance of this job.
	 * @param resourceManager The application's resource manager
	 * @param config The application configuration
	 * @param jobId The database ID of the job
	 * @param parameters The job parameters
	 * @throws MissingParamException If any parameters are missing
	 * @throws InvalidJobParametersException If any of the job parameters are invalid
	 * @throws DatabaseException If a database occurs
	 * @throws RecordNotFoundException If any required database records are missing
	 * @see JobManager#getNextJob(ResourceManager, Properties)
	 */
	public AutoQCJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	/**
	 * Runs the configured QC routines on the file specified in the job parameters.
	 * @param thread The thread that is running this job
	 * @see FileJob#FILE_ID_KEY
	 */
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
			List<Routine> routines = RoutinesConfig.getInstance(parameters.get(PARAM_ROUTINES_CONFIG)).getRoutines();
			
			for (Routine routine : routines) {
				Map<String, String> dynamicParameters = null;

				List<String> requiredParameters = routine.getRequiredDynamicParameters();
				if (requiredParameters.size() > 0) {
					dynamicParameters = new HashMap<String, String>();
					
					for (String parameter : requiredParameters) {
						if (parameter.equals("instrument.min_water_flow")) {
							dynamicParameters.put(parameter, String.valueOf(instrument.getMinimumWaterFlow()));
						}
					}
				}
				
				routine.processRecords((List<DataRecord>) qcRecords, dynamicParameters);
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
						if (qcRecord.getQCFlag().equals(Flag.FATAL)) {
							qcRecord.setWoceFlag(Flag.FATAL);
						} else {
							qcRecord.setWoceFlag(Flag.NEEDED);
						}
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

				// Queue up the fallback job
				try {
					int interruptedJobCode = Integer.parseInt(parameters.get(PARAM_INTERRUPTED_JOB_CODE));
					Map<String, String> interruptedJobParams = getJobParameters(interruptedJobCode, fileId);
					
					User owner = JobManager.getJobOwner(dataSource, id);
					JobManager.addJob(conn, owner, FileInfo.getJobClass(interruptedJobCode), interruptedJobParams);
					DataFileDB.setCurrentJob(conn, fileId, interruptedJobCode);
					conn.commit();
				} catch (NoSuchJobException e) {
					// This means the file has been marked for deletion. No action is required.
				}
			} else {
				// Commit all the records
				conn.commit();
				
				// Queue up the next job
				int nextJobCode = Integer.parseInt(parameters.get(PARAM_NEXT_JOB_CODE));
				if (nextJobCode == FileInfo.JOB_CODE_USER_QC) {
					DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_USER_QC);
				} else {
					Map<String, String> nextJobParams = getJobParameters(nextJobCode, fileId);
					User owner = JobManager.getJobOwner(dataSource, id);
					JobManager.addJob(conn, owner, FileInfo.getJobClass(nextJobCode), nextJobParams);
					DataFileDB.setCurrentJob(conn, fileId, nextJobCode);
				}
				
				conn.commit();
			}
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Create a set of parameters for the AutoQC job.
	 * 
	 * <p>
	 *   The job's file id is passed in to the method. The routines
	 *   and job parameters are set according to the {@code jobCode},
	 *   which indicates the current state of processing the data file.
	 * </p>
	 * 
	 * @param jobCode The current job code
	 * @param fileId The data file's database ID
	 * @return The parameters for the job
	 * @see FileJob#FILE_ID_KEY
	 * @see #PARAM_ROUTINES_CONFIG
	 * @see #PARAM_NEXT_JOB_CODE
	 * @see #PARAM_INTERRUPTED_JOB_CODE
	 */
	protected static Map<String, String> getJobParameters(int jobCode, long fileId) {
		Map<String, String> parameters = new HashMap<String, String>(4);
		parameters.put(FILE_ID_KEY, String.valueOf(fileId));
		
		switch (jobCode) {
		case FileInfo.JOB_CODE_INITIAL_CHECK: {
			parameters.put(PARAM_ROUTINES_CONFIG, ResourceManager.INITIAL_CHECK_ROUTINES_CONFIG);
			parameters.put(PARAM_INTERRUPTED_JOB_CODE, String.valueOf(FileInfo.JOB_CODE_INITIAL_CHECK));
			parameters.put(PARAM_NEXT_JOB_CODE, String.valueOf(FileInfo.JOB_CODE_TRIM_FLUSHING));
			break;
		}
		case FileInfo.JOB_CODE_AUTO_QC: {
			parameters.put(PARAM_ROUTINES_CONFIG, ResourceManager.QC_ROUTINES_CONFIG);
			parameters.put(PARAM_INTERRUPTED_JOB_CODE, String.valueOf(FileInfo.JOB_CODE_REDUCTION));
			parameters.put(PARAM_NEXT_JOB_CODE, String.valueOf(FileInfo.JOB_CODE_USER_QC));
			break;
		}
		}
		
		return parameters;
	}
}
