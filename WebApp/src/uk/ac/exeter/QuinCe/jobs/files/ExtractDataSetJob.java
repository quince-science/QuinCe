package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawData;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Job to extract the data for a data set from the uploaded data files
 * @author Steve Jones
 *
 */
public class ExtractDataSetJob extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  /**
   * The data set being processed by the job
   */
  private DataSet dataSet = null;

  /**
   * The instrument to which the data set belongs
   */
  private Instrument instrument = null;

  /**
   * Initialise the job object so it is ready to run
   *
   * @param resourceManager The system resource manager
   * @param config The application configuration
   * @param jobId The id of the job in the database
   * @param parameters The job parameters, containing the file ID
   * @throws InvalidJobParametersException If the parameters are not valid for the job
   * @throws MissingParamException If any of the parameters are invalid
   * @throws RecordNotFoundException If the job record cannot be found in the database
   * @throws DatabaseException If a database error occurs
   */
  public ExtractDataSetJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    // The statements for measurements and calibrations can be re-used for each record
    PreparedStatement storeMeasurementStatement = null;
    PreparedStatement storeCalibrationStatement = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Get the data set from the database
      dataSet = DataSetDB.getDataSet(conn, Long.parseLong(parameters.get(ID_PARAM)));

      // Reset the data set and all associated data
      reset(conn);

      // Set processing status
      DataSetDB.setDatasetStatus(conn, dataSet, DataSet.STATUS_DATA_EXTRACTION);
      conn.commit();

      // Get related data
      instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());

      DataSetRawData rawData = DataSetRawDataFactory.getDataSetRawData(dataSource, dataSet, instrument);

      DataSetRawDataRecord record = rawData.getNextRecord();
      while (null != record) {
        if (record.isMeasurement()) {
          storeMeasurementStatement = DataSetDataDB.storeRecord(conn, record, storeMeasurementStatement);
        } else if (record.isCalibration()) {
          storeCalibrationStatement = CalibrationDataDB.storeCalibrationRecord(conn, record, storeCalibrationStatement);
        }

        // Read the next record
        record = rawData.getNextRecord();
      }

      DataSetDB.setDatasetStatus(conn, dataSet, DataSet.STATUS_WAITING_FOR_CALCULATION);

      conn.commit();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        conn.rollback();
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
      throw new JobFailedException(id, e);
    } finally {
      DatabaseUtils.closeStatements(storeMeasurementStatement, storeCalibrationStatement);
      DatabaseUtils.closeConnection(conn);
    }
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {
    // TODO Auto-generated method stub
  }

  /**
   * Reset the data set processing.
   *
   * Delete all related records and reset the status
   * @throws MissingParamException If any of the parameters are invalid
   * @throws InvalidDataSetStatusException If the method sets an invalid data set status
   * @throws DatabaseException If a database error occurs
   */
  private void reset(Connection conn) throws MissingParamException, InvalidDataSetStatusException, DatabaseException {

    try {
      CalibrationDataDB.deleteDatasetData(conn, dataSet);
      CalculationDBFactory.getCalculationDB().deleteDatasetCalculationData(conn, dataSet);
      DataSetDB.deleteDatasetData(conn, dataSet);
      DataSetDB.setDatasetStatus(conn, dataSet, DataSet.STATUS_WAITING);
      conn.commit();
    } catch (SQLException e) {
      throw new DatabaseException("Error while resetting dataset data", e);
    }
  }
}
