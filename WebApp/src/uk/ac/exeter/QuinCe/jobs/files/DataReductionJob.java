package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MissingValueMessage;
import uk.ac.exeter.QuinCe.EquilibratorPco2.EquilibratorPco2Calculator;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecordFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculatorException;
import uk.ac.exeter.QuinCe.data.Calculation.DataReductionCalculator;
import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
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
    CalculationDB calculationDB = CalculationDBFactory.getCalculationDB();
    DataSet dataSet = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      dataSet = DataSetDB.getDataSet(conn,
          Long.parseLong(parameters.get(ID_PARAM)));

      // Clear messages before executing job
      dataSet.clearMessages();
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(dataSource.getConnection(), dataSet);

      List<DataSetRawDataRecord> measurements = DataSetDataDB.getMeasurements(conn, dataSet);
      CalibrationDataSet calibrationRecords = CalibrationDataDB.getCalibrationRecords(conn, dataSet);
      CalibrationSet externalStandards = ExternalStandardDB.getInstance().getStandardsSet(conn, dataSet.getInstrumentId(), measurements.get(0).getDate());

      if (!externalStandards.isComplete()) {
        throw new JobFailedException(id, "No complete set of external standards available");
      }

      // TODO This will loop through all available calculators
      DataReductionCalculator calculator = new EquilibratorPco2Calculator(externalStandards, calibrationRecords);

      for (DataSetRawDataRecord measurement : measurements) {
        try {
          Map<String, Double> calculatedValues = calculator.performDataReduction(measurement);
          calculationDB.storeCalculationValues(conn, measurement.getId(), calculatedValues);
        } catch (CalculatorException e) {
          CalculationRecord record = CalculationRecordFactory.makeCalculationRecord(dataSet.getId(), measurement.getId());
          record.loadData(conn);
          record.addMessage(new MissingValueMessage(record.getLineNumber(), Message.NO_COLUMN_INDEX, e.getMessage(), Flag.FATAL));
          CalculationDBFactory.getCalculationDB().storeQC(conn, record);
          calculationDB.storeCalculationValues(conn, measurement.getId(), record.generateNullCalculationRecords());
        }
      }

      // If the thread was interrupted, undo everything
      if (thread.isInterrupted()) {
        conn.rollback();

        // Requeue the data reduction job
        JobManager.requeueJob(conn, id);
        conn.commit();
      } else {

        // Set up the Auto QC job
        dataSet.setStatus(DataSet.STATUS_AUTO_QC);
        DataSetDB.updateDataSet(conn, dataSet);
        Map<String, String> jobParams = new HashMap<String, String>();
        jobParams.put(AutoQCJob.ID_PARAM, String.valueOf(Long.parseLong(parameters.get(ID_PARAM))));
        jobParams.put(AutoQCJob.PARAM_ROUTINES_CONFIG, ResourceManager.QC_ROUTINES_CONFIG);
        JobManager.addJob(dataSource, JobManager.getJobOwner(dataSource, id), AutoQCJob.class.getCanonicalName(), jobParams);

        conn.commit();
      }

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
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Removes any previously calculated data reduction results from the database
   * @throws JobFailedException If an error occurs
   */
  protected void reset() throws JobFailedException {

    Connection conn = null;
    CalculationDB calculationDB = CalculationDBFactory.getCalculationDB();

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);
      DataSet dataSet = DataSetDB.getDataSet(conn, Long.parseLong(parameters.get(ID_PARAM)));
      List<DataSetRawDataRecord> measurements = DataSetDataDB.getMeasurements(conn, dataSet);
      for (DataSetRawDataRecord measurement : measurements) {
        calculationDB.clearCalculationValues(conn, measurement.getId());
      }
    } catch (Exception e) {
      DatabaseUtils.rollBack(conn);
      throw new JobFailedException(id, e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
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
}
