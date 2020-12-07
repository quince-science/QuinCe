package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.PositionQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.QCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.Routine;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
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
 * This {@link Job} class runs a set of QC routines on the sensor values for a
 * given data set.
 * </p>
 *
 * <p>
 * Once the QC has been completed, the QC Flag and QC Message are set. The QC
 * flag will be set to {@link Flag#GOOD}, {@link Flag#QUESTIONABLE} or
 * {@link Flag#BAD}. In the latter two cases, the QC Message will contain
 * details of the fault(s) that were found.
 * </p>
 *
 * <p>
 * If the QC flag was set to {@link Flag#GOOD}, the WOCE flag for the record
 * will be set to {@link Flag#ASSUMED_GOOD}, to indicate that the software will
 * assume that the record is good unless the user overrides it. Otherwise the
 * WOCE Flag will be set to {@link Flag#NEEDED}. The user will be required to
 * manually choose a value for the WOCE Flag, either by accepting the suggestion
 * from the QC job, or overriding the flag and choosing their own. The WOCE
 * Comment will default to being identical to the QC Message, but this can also
 * be changed if required.
 * </p>
 *
 * <p>
 * If the {@code AutoQCJob} has been run before, some WOCE Flags and Comments
 * will have already been set by the user. If the user QC flag is anything other
 * than {@link Flag#ASSUMED_GOOD} or {@link Flag#NEEDED}, it will not be
 * checked.
 * </p>
 *
 * @author Steve Jones
 * @see Flag
 * @see Message
 */
public class AutoQCJob extends DataSetJob {

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Automatic Quality Control";

  private List<String> measurementRunTypes;

  /**
   * Constructor that allows the {@link JobManager} to create an instance of
   * this job.
   *
   * @param resourceManager
   *          The application's resource manager
   * @param config
   *          The application configuration
   * @param jobId
   *          The database ID of the job
   * @param parameters
   *          The job parameters
   * @throws MissingParamException
   *           If any parameters are missing
   * @throws InvalidJobParametersException
   *           If any of the job parameters are invalid
   * @throws DatabaseException
   *           If a database occurs
   * @throws RecordNotFoundException
   *           If any required database records are missing
   * @see JobManager#getNextJob(ResourceManager, Properties)
   */
  public AutoQCJob(ResourceManager resourceManager, Properties config,
    long jobId, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, properties);
  }

  /**
   * Runs the configured QC routines on the file specified in the job
   * parameters.
   *
   * @param thread
   *          The thread that is running this job
   * @see FileJob#FILE_ID_KEY
   */
  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();

      // After automatic QC, all measurements must be recalculated.
      // Therefore before we start, destroy any existing measurements
      // in the data set
      reset(conn);

      conn.setAutoCommit(false);

      // Get the data set from the database
      DataSet dataSet = getDataset(conn);
      Instrument instrument = getInstrument(conn);

      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      measurementRunTypes = instrument.getMeasurementRunTypes();

      QCRoutinesConfiguration qcRoutinesConfig = ResourceManager.getInstance()
        .getQCRoutinesConfiguration();

      // Get the sensor values grouped by data file column
      DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataSet.getId(), true);

      // First run the position QC, unless the instrument has a fixed position.
      // This will potentially set QC flags on all sensor values, and those
      // values will then be skipped by the 'normal' routines later on.
      //
      // Note that this routine uses a different API - the constructor is given
      // all values, and therefore doesn't need to pass any to the actual QC
      // call.

      if (!dataSet.fixedPosition()) {

        List<Long> dataSensorColumnIds = sensorAssignments.getSensorColumnIds();

        PositionQCRoutine positionQC = new PositionQCRoutine(
          sensorValues.getColumnValues(FileDefinition.LONGITUDE_COLUMN_ID),
          sensorValues.getColumnValues(FileDefinition.LATITUDE_COLUMN_ID),
          dataSensorColumnIds, sensorValues);

        positionQC.qcValues(null);
      }

      // Run the routines for each column
      for (long columnId : sensorValues.getColumnIds()) {

        SensorType sensorType = sensorAssignments
          .getSensorTypeForDBColumn(columnId);

        // Where sensors have internal calibrations, their values need to be //
        // QCed in separate groups.
        Map<String, SearchableSensorValuesList> valuesForQC = new HashMap<String, SearchableSensorValuesList>();

        if (!sensorType.hasInternalCalibration()) {
          // All the values can be QCed as a single group
          valuesForQC.put("", sensorValues.getColumnValues(columnId));
        } else {

          // Get all the run type entries from the data set
          List<SensorAssignment> runTypeColumns = sensorAssignments
            .get(SensorType.RUN_TYPE_SENSOR_TYPE);

          TreeSet<SensorValue> runTypeValuesTemp = new TreeSet<SensorValue>();
          for (SensorAssignment column : runTypeColumns) {
            runTypeValuesTemp
              .addAll(sensorValues.getColumnValues(column.getDatabaseId()));
          }

          SearchableSensorValuesList runTypeValues = SearchableSensorValuesList
            .newFromSensorValueCollection(runTypeValuesTemp);

          for (SensorValue value : sensorValues.getColumnValues(columnId)) {

            SensorValue runType = runTypeValues.timeSearch(value.getTime());

            if (!valuesForQC.containsKey(runType.getValue())) {
              valuesForQC.put(runType.getValue(),
                new SearchableSensorValuesList(columnId));
            }

            valuesForQC.get(runType.getValue()).add(value);
          }
        }

        // QC each group of sensor values in turn
        for (Map.Entry<String, SearchableSensorValuesList> values : valuesForQC
          .entrySet()) {

          SensorValue.clearAutoQC(values.getValue());

          List<SensorValue> filteredValues = values.getValue().stream()
            .filter(x -> !(x.getUserQCFlag().equals(Flag.BAD)
              | x.getUserQCFlag().equals(Flag.QUESTIONABLE)))
            .collect(Collectors.toList());

          if (values.getKey().equals("")
            || measurementRunTypes.contains(values.getKey())) {
            // Loop through all
            // routines
            for (Routine routine : qcRoutinesConfig.getRoutines(sensorType)) {
              routine.qcValues(filteredValues);
            }
          }

        }
      }

      // Send all sensor values to be stored. The storeSensorValues method only
      // writes those values whose 'dirty' flag is set.
      DataSetDataDB.storeSensorValues(conn, sensorValues.getAll());

      // Trigger the Build Measurements job
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);
      Properties jobProperties = new Properties();
      jobProperties.setProperty(LocateMeasurementsJob.ID_PARAM,
        String.valueOf(Long.parseLong(properties.getProperty(ID_PARAM))));
      JobManager.addJob(dataSource, JobManager.getJobOwner(dataSource, id),
        LocateMeasurementsJob.class.getCanonicalName(), jobProperties);

      conn.commit();

    } catch (Exception e) {
      e.printStackTrace();
      DatabaseUtils.rollBack(conn);
      try {
        // Set the dataset to Error status
        getDataset(conn).setStatus(DataSet.STATUS_ERROR);
        // And add a (friendly) message...
        StringBuffer message = new StringBuffer();
        message.append(getJobName());
        message.append(" - error: ");
        message.append(e.getMessage());
        getDataset(conn).addMessage(message.toString(),
          ExceptionUtils.getStackTrace(e));
        DataSetDB.updateDataSet(conn, getDataset(conn));
        conn.commit();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
      throw new JobFailedException(id, e);
    } finally {
      if (null != conn) {
        try {
          conn.setAutoCommit(true);
        } catch (SQLException e) {
          throw new JobFailedException(id, e);
        }
      }
      DatabaseUtils.closeConnection(conn);
    }
  }

  @Override
  public String getJobName() {
    return jobName;
  }

  /**
   * Reset the data set processing.
   *
   * Delete all related records and reset the status
   *
   * @throws MissingParamException
   *           If any of the parameters are invalid
   * @throws InvalidDataSetStatusException
   *           If the method sets an invalid data set status
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If the record don't exist
   */
  protected void reset(Connection conn) throws JobFailedException {

    try {
      DataSetDataDB.deleteDataReduction(conn, getDataset(conn).getId());
      DataSetDataDB.deleteMeasurements(conn, getDataset(conn).getId());
      DataSetDB.setDatasetStatus(conn, getDataset(conn).getId(),
        DataSet.STATUS_WAITING);
    } catch (Exception e) {
      throw new JobFailedException(id, "Error while resetting dataset", e);
    }
  }
}
