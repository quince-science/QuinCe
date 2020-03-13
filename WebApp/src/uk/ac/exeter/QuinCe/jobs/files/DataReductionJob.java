package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.api.nrt.MakeNrtDataset;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValueStub;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * The background job to perform data reduction on a data file.
 *
 * @author Steve Jones
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
    long jobId, Map<String, String> parameters) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      reset(conn);
      DataSet dataSet = getDataset(conn);
      Instrument instrument = getInstrument(conn);
      DataSetDB.updateDataSet(conn, dataSet);

      conn.setAutoCommit(false);

      // Clear messages before executing job
      dataSet.clearMessages();
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);

      // Load all the sensor values for this dataset
      Map<Long, SearchableSensorValuesList> allSensorValues = DataSetDataDB
        .getSensorValuesByColumn(conn, dataSet.getId());

      TreeSet<Measurement> measurements = DataSetDataDB.getMeasurements(conn,
        instrument, dataSet.getId());

      for (Measurement measurement : measurements) {

        Map<SensorType, List<MeasurementValue>> measurementSensorValues = new HashMap<SensorType, List<MeasurementValue>>();

        for (InstrumentVariable variable : instrument.getVariables()) {
          if (instrument.isRunTypeForVariable(variable,
            measurement.getRunType())) {

            getSensorValuesForMeasurement(measurement, instrument,
              variable.getAllSensorTypes(true), allSensorValues,
              measurementSensorValues);

            DataSetDataDB.storeMeasurementValues(conn,
              measurementSensorValues.values());
          }
        }
      }

      /*
       * // Get all the sensor values for the dataset, ordered by date and then
       * // grouped by sensor type DateColumnGroupedSensorValues
       * groupedSensorValues = DataSetDataDB
       * .getSensorValuesByDateAndColumn(conn, instrument, dataSet.getId());
       *
       * // Get all the measurement records List<Measurement> allMeasurements =
       * DataSetDataDB.getMeasurements(conn, instrument, dataSet.getId());
       *
       * // Get the most recent calibration data from before the dataset start
       * CalibrationSet calibrationSet = null;
       *
       * if (instrument.hasInternalCalibrations()) { calibrationSet =
       * ExternalStandardDB.getInstance() .getMostRecentCalibrations(conn,
       * instrument.getDatabaseId(), groupedSensorValues.getFirstTime()); }
       *
       * // Cached data reducer instances Map<InstrumentVariable, DataReducer>
       * reducers = new HashMap<InstrumentVariable, DataReducer>();
       *
       * // Storage of values to be written to the database
       * List<CalculationValue> calculationValuesToStore = new
       * ArrayList<CalculationValue>(); List<DataReductionRecord>
       * dataReductionRecords = new ArrayList<DataReductionRecord>(
       * allMeasurements.size());
       *
       * // Process each measurement individually for (Measurement measurement :
       * allMeasurements) {
       *
       * // Only process true measurements (not internal calibrations etc if
       * (isVariableMeasurement(instrument, measurement)) {
       *
       * // Get the value to be used in calculation for each sensor type
       * Map<SensorType, CalculationValue> calculationValues = new
       * HashMap<SensorType, CalculationValue>();
       *
       * // TODO This will have to be more intelligent - allowing // retrieval
       * of values before and after the measurement time // for interpolation
       * etc. // // Get all the sensor values for the measurement time
       * Map<SensorType, List<SensorValue>> values = groupedSensorValues
       * .get(measurement.getTime());
       *
       * // Loop through each sensor type for (SensorType sensorType :
       * values.keySet()) {
       *
       * if (!sensorType.isSystemType()) { List<SensorValue> sensorValues =
       * values.get(sensorType);
       *
       * calculationValues.put(sensorType, CalculationValue.get(measurement,
       * sensorType, sensorValues)); } }
       *
       * DataReducer reducer = reducers.get(measurement.getVariable()); if (null
       * == reducer) {
       *
       * Map<String, Float> variableAttributes = InstrumentDB
       * .getVariableAttributes(conn, instrument.getDatabaseId(),
       * measurement.getVariable().getId());
       *
       * reducer = DataReducerFactory.getReducer(conn, instrument,
       * measurement.getVariable(), dataSet.isNrt(), variableAttributes,
       * calibrationSet, allMeasurements, groupedSensorValues);
       *
       * reducers.put(measurement.getVariable(), reducer); }
       *
       * DataReductionRecord dataReductionRecord = reducer
       * .performDataReduction(instrument, measurement, calculationValues);
       *
       * calculationValuesToStore.addAll(calculationValues.values());
       * dataReductionRecords.add(dataReductionRecord);
       *
       * } }
       */
      // DataSetDataDB.storeDataReduction(conn, calculationValuesToStore,
      // dataReductionRecords);

      // If the thread was interrupted, undo everything
      if (thread.isInterrupted()) {
        conn.rollback();

        // Requeue the data reduction job
        JobManager.requeueJob(conn, id);
      } else {

        if (dataSet.isNrt()) {
          dataSet.setStatus(DataSet.STATUS_READY_FOR_EXPORT);
        } else {
          if (DataSetDataDB.getFlagsRequired(dataSource, dataSet.getId()) > 0) {
            dataSet.setStatus(DataSet.STATUS_USER_QC);
          } else {
            dataSet.setStatus(DataSet.STATUS_READY_FOR_SUBMISSION);
          }
        }

        // Set the dataset status
        DataSetDB.updateDataSet(conn, dataSet);

        // Remake the NRT dataset if necessary (and we haven't just processed
        // it!)
        if (instrument.getNrt() && !dataSet.isNrt()) {
          MakeNrtDataset.createNrtDataset(conn, instrument);
        }
      }

      conn.commit();
    } catch (Exception e) {
      DatabaseUtils.rollBack(conn);

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
        e.printStackTrace();
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

  private void getSensorValuesForMeasurement(Measurement measurement,
    Instrument instrument, List<SensorType> sensorTypes,
    Map<Long, SearchableSensorValuesList> allSensorValues,
    Map<SensorType, List<MeasurementValue>> measurementSensorValues) {

    for (SensorType sensorType : sensorTypes) {
      // If we've already loaded the sensor types, don't bother doing it again
      if (!measurementSensorValues.containsKey(sensorType)) {

        measurementSensorValues.put(sensorType,
          new ArrayList<MeasurementValue>());

        for (long columnId : instrument.getSensorAssignments()
          .getColumnIds(sensorType)) {

          SearchableSensorValuesList columnValues = allSensorValues
            .get(columnId);

          MeasurementValueStub stub = new MeasurementValueStub(measurement,
            columnId);

          MeasurementValue measurementValue = columnValues
            .getMeasurementValue(measurement, stub);

          measurementSensorValues.get(sensorType).add(measurementValue);
        }
      }
    }
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
      DataSetDataDB.deleteMeasurementValues(conn, getDataset(conn).getId());
      DataSetDB.setDatasetStatus(conn, getDataset(conn).getId(),
        DataSet.STATUS_WAITING);
    } catch (Exception e) {
      throw new JobFailedException(id, "Error while resetting dataset", e);
    }
  }
}
