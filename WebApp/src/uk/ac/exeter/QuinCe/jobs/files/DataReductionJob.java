package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DateColumnGroupedSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
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
 * The background job to perform data reduction on a data file.
 *
 * @author Steve Jones
 */
public class DataReductionJob extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Data Reduction";

  /**
   * Constructor for a data reduction job to be run on a specific data file.
   * The job record must already have been created in the database.
   *
   * @param resourceManager The QuinCe resource manager
   * @param config The application configuration
   * @param jobId The job's database ID
   * @param parameters The job parameters. These will be ignored.
   * @throws MissingParamException If any constructor parameters are missing
   * @throws InvalidJobParametersException If any of the parameters are invalid. Because parameters are ignored for this job, this exception will not be thrown.
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the job cannot be found in the database
   */
  public DataReductionJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;
    Connection statusConn = null;
    DataSet dataSet = null;
    Instrument instrument = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      dataSet = DataSetDB.getDataSet(conn,
          Long.parseLong(parameters.get(ID_PARAM)));

      instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(),
        ResourceManager.getInstance().getSensorsConfiguration(),
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      // Clear messages before executing job
      dataSet.clearMessages();
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);

      statusConn = dataSource.getConnection();
      DataSetDB.updateDataSet(statusConn, dataSet);
      statusConn.close();

      // Get all the sensor values for the dataset, ordered by date and then
      // grouped by sensor type
      DateColumnGroupedSensorValues groupedSensorValues =
        DataSetDataDB.getSensorValuesByDateAndColumn(conn,
          instrument, dataSet.getId());

      // Get all the measurement records
      List<Measurement> allMeasurements =
          DataSetDataDB.getMeasurements(conn, instrument, dataSet.getId());

      // Get the most recent calibration data from before the dataset start
      CalibrationSet calibrationSet =
        ExternalStandardDB.getInstance().getMostRecentCalibrations(
        conn, instrument.getDatabaseId(), groupedSensorValues.getFirstTime());

      // Cached data reducer instances
      Map<InstrumentVariable, DataReducer> reducers = new HashMap<InstrumentVariable, DataReducer>();

      // Storage of values to be written to the database
      List<CalculationValue> calculationValuesToStore = new ArrayList<CalculationValue>();
      List<DataReductionRecord> dataReductionRecords = new ArrayList<DataReductionRecord>(allMeasurements.size());

      // Process each measurement individually
      for (Measurement measurement : allMeasurements) {

        // Only process true measurements (not internal calibrations etc
        if (isVariableMeasurement(instrument, measurement)) {

          // Get the value to be used in calculation for each sensor type
          Map<SensorType, CalculationValue> calculationValues =
              new HashMap<SensorType, CalculationValue>();


          // TODO This will have to be more intelligent - allowing
          // retrieval of values before and after the measurement time
          // for interpolation etc.
          //
          // Get all the sensor values for the measurement time
          Map<SensorType, List<SensorValue>> values =
            groupedSensorValues.get(measurement.getTime());


          // Loop through each sensor type
          for (SensorType sensorType : values.keySet()) {

            if (!sensorType.isSystemType()) {
              List<SensorValue> sensorValues = values.get(sensorType);

              calculationValues.put(
                sensorType, CalculationValue.get(measurement, sensorType, sensorValues));
            }
          }

          DataReducer reducer = reducers.get(measurement.getVariable());
          if (null == reducer) {
            reducer = DataReducerFactory.getReducer(conn, instrument,
                measurement.getVariable(), calibrationSet, allMeasurements,
                groupedSensorValues);

            reducers.put(measurement.getVariable(), reducer);
          }

          DataReductionRecord dataReductionRecord = reducer.performDataReduction(
            instrument, measurement, calculationValues);

          calculationValuesToStore.addAll(calculationValues.values());
          dataReductionRecords.add(dataReductionRecord);
        }
      }

      DataSetDataDB.storeDataReduction(conn, calculationValuesToStore, dataReductionRecords);


      // If the thread was interrupted, undo everything
      if (thread.isInterrupted()) {
        conn.rollback();

        // Requeue the data reduction job
        JobManager.requeueJob(conn, id);
      } else {

        // Set up the Auto QC job
        dataSet.setStatus(DataSet.STATUS_USER_QC);
        DataSetDB.updateDataSet(conn, dataSet);
      }

      conn.commit();
    } catch (Exception e) {
      DatabaseUtils.rollBack(conn);

      try {
        if (dataSet != null
            && dataSet.getId() != DatabaseUtils.NO_DATABASE_RECORD) {
          // Change dataset status to Error, and append an error message
          StringBuffer message = new StringBuffer();
          message.append(getJobName());
          message.append(" - error: ");
          message.append(e.getMessage());
          dataSet.addMessage(message.toString(),
              ExceptionUtils.getStackTrace(e));
          dataSet.setStatus(DataSet.STATUS_ERROR);

          DataSetDB.updateDataSet(conn, dataSet);
          conn.commit();
        }
      } catch (Exception e1) {
        e.printStackTrace();
      }

      throw new JobFailedException(id, e);
    } finally {
      DatabaseUtils.closeConnection(conn, statusConn);
    }
  }

  /**
   * Removes any previously calculated data reduction results from the database
   * @throws JobFailedException If an error occurs
   */
  protected void reset() throws JobFailedException {
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {

    String datasetIdString = parameters.get(ID_PARAM);
    if (null == datasetIdString) {
      throw new InvalidJobParametersException(ID_PARAM + "is missing");
    }

    try {
      Long.parseLong(datasetIdString);
    } catch (NumberFormatException e) {
      throw new InvalidJobParametersException(ID_PARAM + "is not numeric");
    }
  }

  @Override
  public String getJobName() {
    return jobName;
  }

  /**
   * Determines whether or not a measurement
   * @param instrument
   * @param measurement
   * @return
   */
  private boolean isVariableMeasurement(Instrument instrument,
      Measurement measurement) {

    RunTypeCategory runTypeCategory =
        instrument.getRunTypeCategory(measurement.getRunType());

    return runTypeCategory.isMeasurementType() &&
        runTypeCategory.getDescription().equals(measurement.getVariable().getName());
  }
}
