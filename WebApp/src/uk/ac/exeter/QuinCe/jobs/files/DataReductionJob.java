package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.api.nrt.MakeNrtDataset;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetMeasurements;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValueCalculatorFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
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
    long jobId, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {

    super(resourceManager, config, jobId, properties);
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

      // Get all the measurements grouped by run type
      DatasetMeasurements allMeasurements = DataSetDataDB
        .getMeasurementsByRunType(conn, instrument, dataSet.getId());

      // Cache of data reducers
      Map<Variable, DataReducer> reducers = new HashMap<Variable, DataReducer>();

      ArrayList<DataReductionRecord> dataReductionRecords = new ArrayList<DataReductionRecord>();

      // Loop through each run type
      for (String runType : allMeasurements.getRunTypes()) {

        // Loop through each variable
        for (Variable variable : instrument.getVariables()) {

          // Process each measurement
          dataReductionRecords.ensureCapacity(dataReductionRecords.size()
            + allMeasurements.getMeasurements(runType).size());

          for (Measurement measurement : allMeasurements
            .getMeasurements(runType)) {

            // If the run type is applicable to this variable, perform the data
            // reduction
            if (instrument.isRunTypeForVariable(variable, runType)) {

              // Get all the sensor values for this measurement.
              // This searches all the sensor values for each required sensor
              // type, finding either the sensor value at the same time as the
              // measurement or the values immediately before and after the
              // measurement time.
              // "Immediately" may mean that we try to find a Good value within
              // a reasonable timespan, or we fall back to a Questionable or Bad
              // value.

              for (SensorType sensorType : variable
                .getAllSensorTypes(!dataSet.fixedPosition())) {

                // Create the MeasurementValue for this SensorType if we haven't
                // already done it.
                if (!measurement.hasMeasurementValue(sensorType)) {
                  measurement
                    .setMeasurementValue(MeasurementValueCalculatorFactory
                      .calculateMeasurementValue(instrument, measurement,
                        sensorType, allMeasurements, allSensorValues, conn));
                }
              }

              // If any of the core sensor values are linked to this measurement
              // are empty, this means the measurement isn't actually available
              // (usually because it's in a FLUSHING state). So we don't process
              // it.
              boolean hasCoreValue = true;

              SensorType coreSensorType = variable.getCoreSensorType();
              if (null != coreSensorType) {

                hasCoreValue = false;

                if (null != measurement.getMeasurementValue(coreSensorType)
                  && measurement.getMeasurementValue(coreSensorType)
                    .hasValue()) {
                  hasCoreValue = true;
                }
              }

              if (hasCoreValue) {

                // Store the measurement values in the database
                DataSetDataDB.storeMeasurementValues(conn, measurement);

                // Get the data reducer for this variable and perform data
                // reduction
                DataReducer reducer = reducers.get(variable);
                if (null == reducer) {
                  reducer = DataReducerFactory.getReducer(conn, instrument,
                    variable, dataSet.getAllProperties());
                  reducers.put(variable, reducer);
                }

                DataReductionRecord dataReductionRecord = reducer
                  .performDataReduction(instrument, measurement, conn);

                dataReductionRecords.add(dataReductionRecord);
              }
            }
          }
        }
      }

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
      DataSetDB.setDatasetStatus(conn, getDataset(conn).getId(),
        DataSet.STATUS_WAITING);
    } catch (Exception e) {
      throw new JobFailedException(id, "Error while resetting dataset", e);
    }
  }
}
