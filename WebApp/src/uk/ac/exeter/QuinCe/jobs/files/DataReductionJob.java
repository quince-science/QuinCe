package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.api.nrt.MakeNrtDataset;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.MeasurementValues;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
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

      // Clear messages before executing job
      dataSet.clearMessages();
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);

      conn.setAutoCommit(false);

      // Load all the sensor values for this dataset
      DatasetSensorValues allSensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataSet.getId(), false);

      // The prefix used for SensorValue searches
      String searchIdPrefix = "DataReductionJob_" + dataSet.getId();

      // Get all the measurements grouped by run type
      Map<String, ArrayList<Measurement>> allMeasurements = DataSetDataDB
        .getMeasurementsByRunType(conn, instrument, dataSet.getId());

      // Cache of data reducers
      Map<InstrumentVariable, DataReducer> reducers = new HashMap<InstrumentVariable, DataReducer>();

      ArrayList<DataReductionRecord> dataReductionRecords = new ArrayList<DataReductionRecord>();

      // Loop through each run type
      for (String runType : allMeasurements.keySet()) {

        // Loop through each variable
        for (InstrumentVariable variable : instrument.getVariables()) {

          // Process each measurement
          dataReductionRecords.ensureCapacity(
            dataReductionRecords.size() + allMeasurements.get(runType).size());

          for (Measurement measurement : allMeasurements.get(runType)) {

            // If the run type is applicable to this variable, perform the data
            // reduction
            if (instrument.isRunTypeForVariable(variable, runType)) {

              // Get all the sensor values
              MeasurementValues measurementSensorValues = new MeasurementValues(
                instrument, measurement, searchIdPrefix);

              for (SensorType sensorType : variable.getAllSensorTypes(true)) {
                measurementSensorValues.loadSensorValues(allSensorValues,
                  sensorType);
              }

              // If any of the core sensor values are linked to this measurement
              // are in a flushing period, then we don't perform the data
              // reduction
              boolean flushing = false;

              for (MeasurementValue measurementValue : measurementSensorValues
                .get(variable.getCoreSensorType())) {
                if (measurementValue.isFlushing(allSensorValues)) {
                  flushing = true;
                  break;
                }
              }

              if (!flushing) {

                // Store the measurement values in the database
                DataSetDataDB.storeMeasurementValues(conn,
                  measurementSensorValues.values());

                // Get the data reducer for this variable and perform data
                // reduction
                DataReducer reducer = reducers.get(variable);
                if (null == reducer) {
                  Map<String, Float> variableAttributes = InstrumentDB
                    .getVariableAttributes(conn, instrument.getDatabaseId(),
                      variable.getId());
                  reducer = DataReducerFactory.getReducer(conn, instrument,
                    variable, variableAttributes);
                  reducers.put(variable, reducer);
                }

                DataReductionRecord dataReductionRecord = reducer
                  .performDataReduction(instrument, measurement,
                    measurementSensorValues, allMeasurements, allSensorValues,
                    conn);

                dataReductionRecords.add(dataReductionRecord);
              }

            }
          }
        }
      }

      // Ideally we wouldn't need this. When the MeasurementValues objects
      // are destroyed then it should clean up the searches that it created, but
      // there is no reliable destructor mechanism in Java 8.
      //
      // In Java 9 there is the Cleaner mechanism that can handle this.
      // TODO Implement in Java 9. GitHub issue #1653
      allSensorValues.destroySearchesWithPrefix(searchIdPrefix);

      DataSetDataDB.storeDataReduction(conn, dataReductionRecords);

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
      e.printStackTrace();
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
        DataSet.STATUS_WAITING);
    } catch (Exception e) {
      throw new JobFailedException(id, "Error while resetting dataset", e);
    }
  }
}
