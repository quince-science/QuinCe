package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QCRoutines.config.RoutinesConfig;
import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QCRoutines.data.InvalidDataException;
import uk.ac.exeter.QCRoutines.data.NoSuchColumnException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.routines.Routine;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecordFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
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
public class AutoQCJob extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  /**
   * The name of the job parameter that contains the path to the configuration
   * for the QC Routines
   * @see RoutinesConfig
   */
  public static final String PARAM_ROUTINES_CONFIG = "ROUTINES_CONFIG";

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Automatic Quality Control";

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
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;
    long datasetId = Long.parseLong(parameters.get(ID_PARAM));
    DataSet dataSet = null;
    try {
      conn = dataSource.getConnection();
      dataSet = DataSetDB.getDataSet(conn, datasetId);
      dataSet.setStatus(DataSet.STATUS_AUTO_QC);
      DataSetDB.updateDataSet(conn, dataSet);
      boolean userQcNeeded = false;

      CalculationDB calculationDB = CalculationDBFactory.getCalculationDB();

      // TODO This should be replaced with something that gets all the records in one query.
      //      This will need changes to how the CalculationRecords are built
      List<Long> measurementIds = DataSetDataDB.getMeasurementIds(conn, datasetId);
      List<? extends DataRecord> records = getRecords(conn, datasetId, measurementIds);

      // Remove any existing automatic QC flags or records
      for (DataRecord record : records) {
        ((CalculationRecord) record).clearAutoQCData();
      }

      List<Routine> routines = RoutinesConfig.getInstance(parameters.get(PARAM_ROUTINES_CONFIG)).getRoutines();

      for (Routine routine : routines) {
        int recordsUpdated = Integer.MAX_VALUE;

        while (recordsUpdated > 0) {
          List<DataRecord> recordsToProcess = new ArrayList<DataRecord>(records.size());
          for (DataRecord record : records) {
            if (!containsMessageFromRoutine(record, routine)) {
              recordsToProcess.add(record);
            }
          }

          routine.processRecords(recordsToProcess, null);
          recordsUpdated = countMessagesFromRoutine(recordsToProcess, routine);
        }
      }

      // Record the messages from the QC in the database
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);
      for (DataRecord record : records) {

        if (thread.isInterrupted()) {
          break;
        }

        CalculationRecord qcRecord = (CalculationRecord) record;
        boolean writeRecord = false;

        int messageCount = qcRecord.getMessages().size();

        Flag previousQCFlag = calculationDB.getAutoQCFlag(conn, qcRecord.getLineNumber());
        if (previousQCFlag.equals(Flag.NOT_SET)) {
          writeRecord = true;
        }

        if (messageCount == 0) {
          if (!previousQCFlag.isGood()) {
            qcRecord.setAutoFlag(Flag.GOOD);
            qcRecord.setUserFlag(Flag.ASSUMED_GOOD);
            qcRecord.setUserMessage(null);
            writeRecord = true;
          } else {
            Flag userFlag = qcRecord.getUserFlag();
            if (!userFlag.equals(Flag.ASSUMED_GOOD) && !userFlag.equals(Flag.GOOD)) {
              qcRecord.setUserFlag(Flag.ASSUMED_GOOD);
              qcRecord.setUserMessage(null);
              writeRecord = true;
            }
          }
        } else {

          // Compare the QC comment (which is the Rebuild Codes for the
          // messages) with the new rebuild codes. If they're the same,
          // take no action. Otherwise reset the QC & WOCE flags and comments
          boolean messagesMatch = true;
          List<Message> databaseMessages = calculationDB.getQCMessages(conn, qcRecord.getLineNumber());
          if (databaseMessages.size() != qcRecord.getMessages().size()) {
            messagesMatch = false;
          } else {
            for (int i = 0; i < databaseMessages.size() && messagesMatch; i++) {
              Message databaseMessage = databaseMessages.get(i);
              boolean databaseMessageFound = false;
              for (Message recordMessage : qcRecord.getMessages()) {
                if (recordMessage.equals(databaseMessage)) {
                  databaseMessageFound = true;
                  break;
                }
              }

              if (!databaseMessageFound) {
                messagesMatch = false;
              }
            }
          }

          if (!messagesMatch) {
            if (qcRecord.getAutoFlag().equals(Flag.FATAL)) {
              qcRecord.setUserFlag(Flag.FATAL);
            } else {
              qcRecord.setUserFlag(Flag.NEEDED);
            }
            qcRecord.setUserMessage(qcRecord.getMessageSummaries());
            writeRecord = true;
          }
        }

        if (writeRecord) {
          calculationDB.storeQC(conn, (CalculationRecord) record);
          if (qcRecord.getUserFlag().equals(Flag.NEEDED)) {
            userQcNeeded = true;
          }
        }
      }

      // If the thread was interrupted, undo everything
      if (thread.isInterrupted()) {
        conn.rollback();
      } else {
        // Commit all the records
        if (dataSet != null) {
          if (dataSet.isNrt()) {
            dataSet.setStatus(DataSet.STATUS_READY_FOR_EXPORT);
          } else {
            if (userQcNeeded) {
              dataSet.setStatus(DataSet.STATUS_USER_QC);
            } else {
              dataSet.setStatus(DataSet.STATUS_READY_FOR_SUBMISSION);
            }
          }

          DataSetDB.updateDataSet(conn, dataSet);
        }
        conn.commit();
      }
    } catch (Exception e) {
      // Revert all changes
      DatabaseUtils.rollBack(conn);

      try {
        // Set the dataset to Error status
        if (dataSet != null) {
          dataSet.setStatus(DataSet.STATUS_ERROR);

          StringBuffer message = new StringBuffer();
          message.append(getJobName());
          message.append(" - error: ");
          message.append(e.getMessage());
          dataSet.addMessage(message.toString(),
              ExceptionUtils.getStackTrace(e));
          DataSetDB.updateDataSet(conn, dataSet);
        }
        conn.commit();
      } catch (Exception e1) {
        e.printStackTrace();
      }

      throw new JobFailedException(id, e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Count the number of records that contain messages from a specified QC routine
   * @param recordsToProcess The records to be counted
   * @param routine The routine
   * @return The number of records with messages from the routine
   */
  private int countMessagesFromRoutine(List<DataRecord> records, Routine routine) {
    int count = 0;

    for (DataRecord record : records) {
      if (containsMessageFromRoutine(record, routine)) {
        count++;
      }
    }

    return count;
  }

  /**
   * Determine whether or not a record contains QC messages from a specified QC routine
   * @param record The record to be checked
   * @param routine The routine
   * @return {@code true} if any messages from the routine exist; {@code false} otherwise
   */
  private boolean containsMessageFromRoutine(DataRecord record, Routine routine) {
    boolean messageFound = false;

    for (Message message : record.getMessages()) {
      if (message.getClass().equals(routine.getMessageClass())) {
        messageFound = true;
        break;
      }
    }

    return messageFound;
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {
    // TODO Auto-generated method stub
  }

  /**
   * Get the calculation records for a set of measurements. QUESTIONABLE, BAD or IGNORED records
   * are not included.
   * @param datasetId The dataset ID
   * @param ids The measurement IDs
   * @return The calculation records
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the record is not in the database
   * @throws InvalidDataException If a field cannot be added to the record
   * @throws MessageException If the automatic QC messages cannot be parsed
   * @throws NoSuchColumnException If the automatic QC messages cannot be parsed
   */
  private List<CalculationRecord> getRecords(Connection conn, long datasetId, List<Long> ids) throws MissingParamException, InvalidDataException, NoSuchColumnException, DatabaseException, RecordNotFoundException, MessageException {
    List<CalculationRecord> records = new ArrayList<CalculationRecord>(ids.size());

    for (long id : ids) {
      CalculationRecord record = CalculationRecordFactory.makeCalculationRecord(datasetId, id);
      record.loadData(conn);

      // Skip any records where the user has explicitly set the flag. We trust their judgement.
      if (!record.getUserFlag().equals(Flag.BAD) && !record.getUserFlag().equals(Flag.QUESTIONABLE) && !record.getUserFlag().equals(Flag.GOOD)) {
        records.add(record);
      }
    }

    return records;
  }

  @Override
  public String getJobName() {
    return jobName;
  }
}
