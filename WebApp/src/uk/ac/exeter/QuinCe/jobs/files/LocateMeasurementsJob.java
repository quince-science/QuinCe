package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
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
 * Identifies individual measurements in a dataset and stores them in the
 * database. Followed by the ChooseSensorValues job, which picks the
 * SensorValues to use for that measurement
 *
 * @author Steve Jones
 *
 */
// TODO The detailed selection operations are not yet implemented.
public class LocateMeasurementsJob extends DataSetJob {

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Locate Measurements";

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
  public LocateMeasurementsJob(ResourceManager resourceManager,
    Properties config, long jobId, Properties properties)
    throws MissingParamException, InvalidJobParametersException,
    DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, properties);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {
    Connection conn = null;

    try {
      conn = dataSource.getConnection();

      DataSet dataSet = getDataset(conn);
      Instrument instrument = getInstrument(conn);

      reset(conn);
      conn.setAutoCommit(false);

      // Get the run types for the dataset
      TreeMap<LocalDateTime, String> runTypes = null;

      if (instrument.hasRunTypes()) {
        runTypes = new TreeMap<LocalDateTime, String>();

        List<SensorValue> runTypeSensorValues = DataSetDataDB
          .getSensorValuesForColumns(conn, dataSet.getId(),
            instrument.getSensorAssignments().getRunTypeColumnIDs());

        for (SensorValue value : runTypeSensorValues) {
          runTypes.put(value.getTime(), value.getValue());
        }
      }

      // Get all the times that the core sensor for one of the instrument's
      // variables has a value.
      List<Variable> variables = instrument.getVariables();

      Set<Long> measurementColumnIds = new HashSet<Long>(variables.size());

      for (Variable variable : variables) {
        SensorType coreSensorType = variable.getCoreSensorType();
        if (null != coreSensorType) {
          List<Long> columns = instrument.getSensorAssignments()
            .getColumnIds(coreSensorType);
          measurementColumnIds.addAll(columns);
        } else {

          // If there's no core sensor type, use any of the sensor values
          for (SensorType sensorType : variable.getAllSensorTypes(false)) {
            List<Long> columns = instrument.getSensorAssignments()
              .getColumnIds(sensorType);
            measurementColumnIds.addAll(columns);
          }
        }
      }

      List<SensorValue> sensorValues = DataSetDataDB.getSensorValuesForColumns(
        conn, dataSet.getId(), new ArrayList<Long>(measurementColumnIds));

      // Now log all the times as new measurements, with the run type from the
      // same time or immediately before.
      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.size());

      ArrayList<LocalDateTime> runTypeTimes = null;

      if (null != runTypes) {
        runTypeTimes = new ArrayList<LocalDateTime>(runTypes.keySet());
      }

      int currentRunTypeTime = 0;

      for (SensorValue sensorValue : sensorValues) {
        if (null != sensorValue.getValue()) {
          LocalDateTime measurementTime = sensorValue.getTime();

          // Get the run type for this measurement
          String runType = null;
          if (null != runTypes) {

            // Find the run type immediately before or at the same time as the
            // measurement
            if (runTypeTimes.get(currentRunTypeTime).isAfter(measurementTime)) {
              // There is no run type for this measurement. This isn't allowed!
              throw new JobFailedException(id,
                "No run type available in Dataset " + dataSet.getId()
                  + " at time " + measurementTime.toString());
            } else {
              while (currentRunTypeTime < runTypeTimes.size() - 1
                && runTypeTimes.get(currentRunTypeTime)
                  .isBefore(measurementTime)) {
                currentRunTypeTime++;
              }

              runType = runTypes.get(runTypeTimes.get(currentRunTypeTime));
            }
          }

          measurements
            .add(new Measurement(dataSet.getId(), measurementTime, runType));
        }
      }

      DataSetDataDB.storeMeasurements(conn, measurements);

      // Trigger the Build Measurements job
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);
      Properties jobProperties = new Properties();
      jobProperties.setProperty(LocateMeasurementsJob.ID_PARAM,
        String.valueOf(Long.parseLong(properties.getProperty(ID_PARAM))));
      JobManager.addJob(dataSource, JobManager.getJobOwner(dataSource, id),
        DataReductionJob.class.getCanonicalName(), jobProperties);

      conn.commit();
    } catch (

    Exception e) {
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
