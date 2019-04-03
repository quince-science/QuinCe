package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.QCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.Routine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
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
 *   This {@link Job} class runs a set of QC routines on the sensor values for
 *   a given data set.
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
 *   If the user QC flag is anything other than {@link Flag#ASSUMED_GOOD} or {@link Flag#NEEDED}, it will not be checked.
 * </p>
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
   * Name of the job, used for reporting
   */
  private final String jobName = "Automatic Quality Control";

  /**
   * The data set being processed by the job
   */
  private DataSet dataSet = null;

  /**
   * The instrument to which the data set belongs
   */
  private Instrument instrument = null;

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
  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Get the data set from the database
      dataSet = DataSetDB.getDataSet(conn, Long.parseLong(parameters.get(ID_PARAM)));

      instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(),
        ResourceManager.getInstance().getSensorsConfiguration(),
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      QCRoutinesConfiguration qcRoutinesConfig =
        ResourceManager.getInstance().getQCRoutinesConfiguration();

      // Get the sensor values grouped by data file column
      Map<Long, List<SensorValue>> sensorValues =
        DataSetDataDB.getSensorValuesByColumn(conn, dataSet.getId());

      // Run the routines for each column
      for (Map.Entry<Long, List<SensorValue>> entry : sensorValues.entrySet()) {
        SensorType sensorType = sensorAssignments.getSensorTypeForDBColumn(entry.getKey());
        for (Routine routine : qcRoutinesConfig.getRoutines(sensorType)) {
          routine.qcValues(entry.getValue());
        }
      }

      // Store all the sensor values
      List<SensorValue> allValues = new ArrayList<SensorValue>();
      sensorValues.values().forEach(allValues::addAll);
      DataSetDataDB.storeSensorValues(conn, allValues);

      conn.commit();

    } catch (Exception e) {
      e.printStackTrace();
      DatabaseUtils.rollBack(conn);
      try {
        // Set the dataset to Error status
        dataSet.setStatus(DataSet.STATUS_ERROR);
        // And add a (friendly) message...
        StringBuffer message = new StringBuffer();
        message.append(getJobName());
        message.append(" - error: ");
        message.append(e.getMessage());
        dataSet.addMessage(message.toString(), ExceptionUtils.getStackTrace(e));
        DataSetDB.updateDataSet(conn, dataSet);
        conn.commit();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
      throw new JobFailedException(id, e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {
    // TODO Auto-generated method stub
  }

  @Override
  public String getJobName() {
    return jobName;
  }
}
