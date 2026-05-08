package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.NewSensorValues;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.jobs.NextJobInfo;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Job to extract the data for a data set from the uploaded data files
 */
public class ExtractDataSetJob extends DataSetJob {

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Dataset Extraction";

  /**
   * Initialise the job object so it is ready to run
   *
   * @param resourceManager
   *          The system resource manager
   * @param config
   *          The application configuration
   * @param jobId
   *          The id of the job in the database
   * @param parameters
   *          The job parameters, containing the file ID
   * @throws InvalidJobParametersException
   *           If the parameters are not valid for the job
   * @throws MissingParamException
   *           If any of the parameters are invalid
   * @throws RecordNotFoundException
   *           If the job record cannot be found in the database
   * @throws DatabaseException
   *           If a database error occurs
   */
  public ExtractDataSetJob(ResourceManager resourceManager, Properties config,
    long jobId, User owner, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, owner, properties);
  }

  @Override
  protected NextJobInfo execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Clear any existing data for the DataSet
      resetDataset(conn);
      conn.commit();

      // Get the new data set from the database and set its status
      DataSet dataSet = getDataset(conn);
      dataSet.setStatus(DataSet.STATUS_DATA_EXTRACTION);
      DataSetDB.updateDataSet(conn, dataSet);
      conn.commit();

      // Process the dataset
      DataSetExtractor extractor;

      Instrument instrument = getInstrument(conn);
      switch (instrument.getBasis()) {
      case Instrument.BASIS_TIME: {
        extractor = new TimeDataSetExtractor();
        break;
      }
      case Instrument.BASIS_ARGO:
        extractor = new ArgoDataSetExtractor();
        break;
      default: {
        throw new JobException(
          "Unrecognised instrument basis " + instrument.getBasis());
      }
      }

      extractor.extract(conn, instrument, dataSet);

      // Store the used files
      DataSetDB.storeDatasetFiles(conn, dataSet, extractor.getUsedFiles());

      // Store the extracted values
      NewSensorValues sensorValues = extractor.getSensorValues();

      if (sensorValues.size() > 0) {
        DataSetDataDB.storeNewSensorValues(conn, sensorValues);
      }

      conn.commit();
      conn.setAutoCommit(true);

      dataSet.setBounds(extractor.getGeoBounds());

      // Trigger the Auto QC job
      dataSet.setStatus(DataSet.STATUS_SENSOR_QC);
      DataSetDB.updateDataSet(conn, dataSet);

      Properties jobProperties = new Properties();
      jobProperties.setProperty(DataSetJob.ID_PARAM, String
        .valueOf(Long.parseLong(properties.getProperty(DataSetJob.ID_PARAM))));
      NextJobInfo nextJob = new NextJobInfo(AutoQCJob.class.getCanonicalName(),
        jobProperties);
      nextJob.putTransferData(SENSOR_VALUES, sensorValues.toSet());
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
      try {
        if (!conn.getAutoCommit()) {
          conn.setAutoCommit(true);
        }
      } catch (SQLException e) {
        // NOOP
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
  protected void resetDataset(Connection conn) throws JobFailedException {

    try {
      long datasetId = getDataset(conn).getId();
      DataSetDataDB.deleteDataReduction(conn, datasetId);
      DataSetDataDB.deleteMeasurements(conn, datasetId);
      DataSetDataDB.deleteSensorValues(conn, datasetId);
    } catch (Exception e) {
      throw new JobFailedException(id, "Error while resetting dataset", e);
    }
  }
}
