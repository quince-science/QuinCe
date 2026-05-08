package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetMeasurements;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValueCollector;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValueCollectorFactory;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.jobs.NextJobInfo;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * The background job to perform data reduction on a data file.
 */
public class DataReductionJob extends DataSetJob {

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Data Reduction";

  /**
   * Constructor for a data reduction job to be run on a specific data file. The
   * job record must already have been created in the database.
   *
   * @param resourceManager
   *          The QuinCe resource manager
   * @param config
   *          The application configuration
   * @param jobId
   *          The job's database ID
   * @param parameters
   *          The job parameters. These will be ignored.
   * @throws MissingParamException
   *           If any constructor parameters are missing
   * @throws InvalidJobParametersException
   *           If any of the parameters are invalid. Because parameters are
   *           ignored for this job, this exception will not be thrown.
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If the job cannot be found in the database
   */
  public DataReductionJob(ResourceManager resourceManager, Properties config,
    long jobId, User owner, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {

    super(resourceManager, config, jobId, owner, properties);
  }

  @Override
  protected NextJobInfo execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      reset(conn);
      DataSet dataSet = getDataset(conn);
      Instrument instrument = getInstrument(conn);
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      // Clear messages before executing job
      dataSet.clearMessages();
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);

      conn.setAutoCommit(false);

      @SuppressWarnings("unchecked")
      Collection<SensorValue> rawSensorValues = (Collection<SensorValue>) getTransferData(
        SENSOR_VALUES);
      if (null == rawSensorValues) {
        rawSensorValues = DataSetDataDB.getRawSensorValues(conn, dataSet);
      }

      DatasetSensorValues allSensorValues = new DatasetSensorValues(conn,
        dataSet, false, false, rawSensorValues);

      // Get all the measurements grouped by run type
      DatasetMeasurements allMeasurements = DataSetDataDB
        .getMeasurementsByRunType(conn, dataSet);

      CalibrationSet calculationCoefficients = CalculationCoefficientDB
        .getInstance().getCalibrationSet(conn, dataSet);

      ArrayList<DataReductionRecord> dataReductionRecords = new ArrayList<DataReductionRecord>();

      // First we calculate measurement values for all measurements
      for (Measurement measurement : allMeasurements.getOrderedMeasurements()) {

        // Work out which variables this measurement is relevant for.
        Set<Variable> variablesToProcess = new TreeSet<Variable>();

        // Get the combinations of Variable/Run Type for the measurement
        for (Map.Entry<Long, String> runTypeEntry : measurement.getRunTypes()
          .entrySet()) {

          // See if this run type is for the GENERIC variable - this is a value
          // from the Run Type column which determines which variable(s) it
          // belongs to
          if (runTypeEntry.getKey() == Measurement.RUN_TYPE_DEFINES_VARIABLE) {

            for (Variable variable : instrument.getVariables()) {
              if (instrument.isRunTypeForVariable(variable,
                runTypeEntry.getValue())) {

                variablesToProcess.add(variable);
              }
            }
          } else {
            // The run type entry contains the variable ID so we can add it
            // directly.
            variablesToProcess
              .add(sensorConfig.getInstrumentVariable(runTypeEntry.getKey()));
          }
        }

        dataReductionRecords.ensureCapacity(
          dataReductionRecords.size() + variablesToProcess.size());

        /*
         * A store of one of the values we will calculate. This is used later on
         * for final adjustments to the Measurement object.
         */
        Variable usedVariable = null;

        // Loop through each variable
        for (Variable variable : variablesToProcess) {

          MeasurementValueCollector measurementValueCollector = MeasurementValueCollectorFactory
            .getCollector(variable);

          Collection<MeasurementValue> measurementValues = measurementValueCollector
            .collectMeasurementValues(instrument, dataSet, variable,
              allMeasurements, allSensorValues, conn, measurement);

          // Otherwise store the measurement values for processing.
          measurementValues.forEach(mv -> {
            if (null != mv) {
              if (!measurement.hasMeasurementValue(mv.getSensorType())) {
                measurement.setMeasurementValue(mv);
              }
            }
          });

          DataSetDataDB.storeMeasurementValues(conn, measurement);

          // Store this variable for use below
          if (null == usedVariable) {
            usedVariable = variable;
          }
        }

        /*
         * Finally we adjust the measurement time. The original measurement time
         * was the time of the value from the core sensor type. We apply an
         * offset to the first sensor group, since this is the point of first
         * acquisition of data relevant to the measurement and therefore closest
         * to the real time for the measurement. We can use any of the
         * insturment's variables for this purpose.
         */
        if (instrument.getBasis() == Instrument.BASIS_TIME
          && null != usedVariable) {

          SensorType coreSensorType = usedVariable.getCoreSensorType();
          SensorAssignment coreAssignment = instrument.getSensorAssignments()
            .get(coreSensorType).first();
          TimeCoordinate offsetMeasurementTime = dataSet.getSensorOffsets()
            .offsetToFirstGroup((TimeCoordinate) measurement.getCoordinate(),
              coreAssignment, allSensorValues);
          measurement.setCoordinate(offsetMeasurementTime);
          DataSetDataDB.updateMeasurementCoordinate(conn, measurement);
        }
      }

      // Now run all the data reducers
      for (Variable variable : instrument.getVariables()) {

        DataReducer reducer = DataReducerFactory.getReducer(variable,
          dataSet.getAllProperties(), calculationCoefficients);

        reducer.preprocess(conn, instrument, dataSet,
          allMeasurements.getOrderedMeasurements());

        for (Measurement measurement : allMeasurements
          .getOrderedMeasurements()) {

          if (instrument.isRunTypeForVariable(variable,
            measurement.getRunType(variable))
            || instrument.isRunTypeForVariable(variable,
              measurement.getRunType(Measurement.RUN_TYPE_DEFINES_VARIABLE))) {

            DataReductionRecord dataReductionRecord = reducer
              .performDataReduction(instrument, measurement, allSensorValues,
                conn);

            dataReductionRecords.add(dataReductionRecord);
          }
        }
      }

      DataSetDataDB.storeDataReduction(conn, dataReductionRecords);

      NextJobInfo nextJob = null;

      // If the thread was interrupted, undo everything
      if (thread.isInterrupted()) {
        conn.rollback();

        // Requeue the data reduction job
        JobManager.requeueJob(conn, id);
      } else {
        // Set the dataset status
        dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION_QC);
        dataSet.setProcessingVersion();
        DataSetDB.updateDataSet(conn, dataSet);

        Properties jobParams = new Properties();
        jobParams.put(DataSetJob.ID_PARAM, String.valueOf(
          Long.parseLong(properties.getProperty(DataSetJob.ID_PARAM))));
        nextJob = new NextJobInfo(DataReductionQCJob.class.getCanonicalName(),
          jobParams);
        nextJob.putTransferData(SENSOR_VALUES, rawSensorValues);
      }

      conn.commit();
      return nextJob;
    } catch (

    Exception e) {
      DatabaseUtils.rollBack(conn);
      ExceptionUtils.printStackTrace(e);
      try {
        // Change dataset status to Error, and append an error message
        StringBuffer message = new StringBuffer();
        message.append(getJobName());
        message.append(" - error: ");
        message.append(e.getMessage());
        getDataset(conn).addMessage(message.toString(),
          ExceptionUtils.getStackTrace(e));
        getDataset(conn).setStatus(DataSet.STATUS_ERROR);

        DataSetDB.updateDataSet(conn, getDataset(conn));
        conn.commit();
      } catch (Exception e1) {
        ExceptionUtils.printStackTrace(e1);
      }

      throw new JobFailedException(id, e);
    } finally {
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
      DataSetDB.setDatasetStatus(conn, getDataset(conn).getId(),
        DataSet.STATUS_DATA_REDUCTION);
    } catch (Exception e) {
      throw new JobFailedException(id, "Error while resetting dataset", e);
    }
  }
}
