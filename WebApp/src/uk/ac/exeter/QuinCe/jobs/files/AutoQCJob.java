package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards.ExternalStandardsQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards.ExternalStandardsRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AbstractAutoQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.DiagnosticsQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.PositionQCCascadeRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.PositionQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.QCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.SpeedQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
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
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * <p>
 * This {@link Job} class runs a set of QC routines on the {@link SensorValue}s
 * for a given data set.
 * </p>
 *
 * <p>
 * Once the QC has been completed, an {@link AutoQCResult} is set on the
 * {@link SensorValue}. If the QC result was {@link Flag#GOOD}, the WOCE flag
 * for the record will be set to {@link Flag#ASSUMED_GOOD}, to indicate that the
 * software will assume that the record is good unless the user overrides it.
 * Otherwise the WOCE Flag will be set to {@link Flag#NEEDED}. The user will be
 * required to manually choose a value for the WOCE Flag, either by accepting
 * the suggestion from the QC job, or overriding the flag and choosing their
 * own. The WOCE Comment will default to being identical to the QC Message, but
 * this can also be changed if required.
 * </p>
 *
 * <p>
 * If the {@code AutoQCJob} has been run before, some WOCE Flags and Comments
 * will have already been set by the user. If the user QC flag is anything other
 * than {@link Flag#ASSUMED_GOOD} or {@link Flag#NEEDED}, it will not be
 * checked.
 * </p>
 *
 * @see AutoQCResult
 */
public class AutoQCJob extends DataSetJob {

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Sensor Quality Control";

  /**
   * The Run Type strings for the {@link Instrument} that indicate measurements
   * (as opposed to calibrations or other statuses).
   */
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
   * @param properties
   *          The job parameters
   * @throws InvalidJobParametersException
   *           If any of the job properties are invalid
   * @throws DatabaseException
   *           If a database error occurs
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

      // Get the data set from the database
      DataSet dataSet = getDataset(conn);
      Instrument instrument = getInstrument(conn);

      dataSet.setStatus(DataSet.STATUS_SENSOR_QC);
      DataSetDB.updateDataSet(conn, dataSet);

      conn.setAutoCommit(false);

      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataSet.getId(), true, true);

      RunTypePeriods runTypePeriods = null;
      SensorValuesList runTypeValues = null;

      // This will be populated if the instrument has position data
      DatasetSensorValues positionValues = null;

      if (instrument.hasRunTypes()) {
        measurementRunTypes = instrument.getMeasurementRunTypes();

        // Get all the run type entries from the data set
        TreeSet<SensorAssignment> runTypeColumns = sensorAssignments
          .get(SensorType.RUN_TYPE_SENSOR_TYPE);

        TreeSet<SensorValue> runTypeValuesTemp = new TreeSet<SensorValue>();
        for (SensorAssignment column : runTypeColumns) {
          runTypeValuesTemp.addAll(sensorValues
            .getColumnValues(column.getDatabaseId()).getRawValues());
        }

        runTypeValues = SensorValuesList
          .newFromSensorValueCollection(runTypeValuesTemp, sensorValues);

        // Get the Run Type Periods for the dataset
        runTypePeriods = DataSetDataDB.getRunTypePeriods(conn, instrument,
          dataSet.getId());
      }

      QCRoutinesConfiguration qcRoutinesConfig = ResourceManager.getInstance()
        .getQCRoutinesConfiguration();

      // First run the position QC, unless the instrument has a fixed position.
      // This will potentially set QC flags on all sensor values, and those
      // values will then be skipped by the 'normal' routines later on.
      //
      // Note that this routine uses a different API - the constructor is given
      // all values, and therefore doesn't need to pass any to the actual QC
      // call.
      if (!dataSet.fixedPosition()) {

        positionValues = DataSetDataDB.getPositionSensorValues(conn, instrument,
          dataSet.getId());

        SensorValue.clearAutoQC(positionValues.getAllPositionSensorValues());

        PositionQCRoutine positionQC = new PositionQCRoutine(positionValues);
        positionQC.qc(null, null);

        SpeedQCRoutine speedQC = new SpeedQCRoutine(positionValues);
        speedQC.qc(null, null);
      }

      // Run the auto QC routines for each column
      for (long columnId : sensorValues.getColumnIds()) {

        SensorType sensorType = sensorAssignments
          .getSensorTypeForDBColumn(columnId);

        // Where sensors have internal calibrations, their values need to be //
        // QCed in separate groups.
        Map<String, SensorValuesList> valuesForQC = new HashMap<String, SensorValuesList>();

        if (!sensorType.hasInternalCalibration()) {
          // All the values can be QCed as a single group
          valuesForQC.put("", sensorValues.getColumnValues(columnId));
        } else {
          for (SensorValue value : sensorValues.getColumnValues(columnId)
            .getRawValues()) {

            SensorValuesListValue runType = runTypeValues
              .getValueOnOrBefore(value.getTime());

            if (!valuesForQC.containsKey(runType.getStringValue())) {
              valuesForQC.put(runType.getStringValue(),
                new SensorValuesList(columnId, sensorValues));
            }

            valuesForQC.get(runType.getStringValue()).add(value);
          }
        }

        // QC each group of sensor values in turn
        for (Map.Entry<String, SensorValuesList> values : valuesForQC
          .entrySet()) {

          SensorValue.clearAutoQC(values.getValue());

          List<SensorValue> filteredValues = values.getValue().getRawValues()
            .stream()
            .filter(x -> !(x.getUserQCFlag().equals(Flag.BAD)
              | x.getUserQCFlag().equals(Flag.QUESTIONABLE)))
            .collect(Collectors.toList());

          if (values.getKey().equals("")
            || measurementRunTypes.contains(values.getKey())) {
            // Loop through all routines
            for (AbstractAutoQCRoutine routine : qcRoutinesConfig
              .getRoutines(sensorType)) {

              routine.setSensorType(sensorType);
              ((AutoQCRoutine) routine).qc(filteredValues, runTypePeriods);
            }
          }
        }
      }

      // External Standards routines
      if (instrument.hasInternalCalibrations()) {
        ExternalStandardsRoutinesConfiguration externalStandardsRoutinesConfig = ResourceManager
          .getInstance().getExternalStandardsRoutinesConfiguration();

        for (long columnId : sensorValues.getColumnIds()) {

          SensorType sensorType = sensorAssignments
            .getSensorTypeForDBColumn(columnId);

          CalibrationSet calibrationSet = ExternalStandardDB.getInstance()
            .getCalibrationSet(conn, dataSet);

          if (sensorType.hasInternalCalibration()) {
            for (AbstractAutoQCRoutine routine : externalStandardsRoutinesConfig
              .getRoutines(sensorType)) {

              routine.setSensorType(sensorType);
              ((ExternalStandardsQCRoutine) routine).qc(calibrationSet,
                runTypeValues, sensorValues.getColumnValues(columnId));
            }
          }
        }
      }

      // Diagnostics QC
      DiagnosticsQCRoutine diagnosticsQC = new DiagnosticsQCRoutine();
      diagnosticsQC.run(instrument, sensorValues, runTypePeriods);

      // Cascade position QC to SensorValues
      if (!instrument.fixedPosition()) {
        PositionQCCascadeRoutine positionQCCascade = new PositionQCCascadeRoutine();
        positionQCCascade.run(instrument, sensorValues, runTypePeriods);
      }

      // Send all sensor values to be stored. The storeSensorValues method only
      // writes those values whose 'dirty' flag is set.
      DataSetDataDB.storeSensorValues(conn, sensorValues.getAll());

      if (null != positionValues) {
        DataSetDataDB.storeSensorValues(conn,
          positionValues.getAllPositionSensorValues());
      }

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
      ExceptionUtils.printStackTrace(e);
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
        ExceptionUtils.printStackTrace(e1);
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
   * @param conn
   *          A database connection.
   * @throws JobFailedException
   *           If the reset action fails.
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
