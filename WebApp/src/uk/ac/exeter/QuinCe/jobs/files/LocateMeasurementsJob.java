package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementLocator;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
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
 * Identifies individual measurements in a dataset and stores them in the
 * database.
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
    Properties config, long jobId, User owner, Properties properties)
    throws MissingParamException, InvalidJobParametersException,
    DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, owner, properties);
  }

  @Override
  protected NextJobInfo execute(JobThread thread) throws JobFailedException {
    Connection conn = null;

    try {
      conn = dataSource.getConnection();

      DataSet dataSet = getDataset(conn);
      Instrument instrument = getInstrument(conn);

      reset(conn);
      conn.setAutoCommit(false);

      @SuppressWarnings("unchecked")
      Collection<SensorValue> rawSensorValues = (Collection<SensorValue>) getTransferData(
        SENSOR_VALUES);
      if (null == rawSensorValues) {
        rawSensorValues = DataSetDataDB.getRawSensorValues(conn, dataSet);
      }

      DatasetSensorValues sensorValues = new DatasetSensorValues(conn, dataSet,
        false, true, rawSensorValues);

      // Work out which measurement locators we need to use
      Set<MeasurementLocator> measurementLocators = new HashSet<MeasurementLocator>();

      instrument.getVariables().stream()
        .map(MeasurementLocator::getMeasurementLocator).filter(Objects::nonNull)
        .forEach(measurementLocators::add);

      if (measurementLocators.size() == 0) {
        throw new JobFailedException(id,
          "No measurement locators found for instrument variables");
      }

      // Now locate the measurements
      Map<Coordinate, Measurement> measurements = new HashMap<Coordinate, Measurement>();

      for (MeasurementLocator locator : measurementLocators) {
        addMeasurements(measurements,
          locator.locateMeasurements(conn, instrument, dataSet, sensorValues));
      }

      DataSetDataDB.storeMeasurements(conn, measurements.values());

      // Trigger the Build Measurements job
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);
      conn.commit();

      Properties jobProperties = new Properties();
      jobProperties.setProperty(DataSetJob.ID_PARAM, String
        .valueOf(Long.parseLong(properties.getProperty(DataSetJob.ID_PARAM))));
      NextJobInfo nextJob = new NextJobInfo(
        DataReductionJob.class.getCanonicalName(), jobProperties);
      nextJob.putTransferData(SENSOR_VALUES, rawSensorValues);
      return nextJob;
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

  private void addMeasurements(Map<Coordinate, Measurement> target,
    List<Measurement> newMeasurements) {

    newMeasurements.forEach(m -> {
      if (target.containsKey(m.getCoordinate())) {
        target.get(m.getCoordinate()).addRunTypes(m);
      } else {
        target.put(m.getCoordinate(), m);
      }
    });

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
