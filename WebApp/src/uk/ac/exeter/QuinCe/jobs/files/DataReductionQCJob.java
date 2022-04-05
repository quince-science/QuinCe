package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class DataReductionQCJob extends DataSetJob {

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Data Reduction QC";

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
  public DataReductionQCJob(ResourceManager resourceManager, Properties config,
    long jobId, Properties parameters) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {

    super(resourceManager, config, jobId, parameters);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      DataSet dataSet = getDataset(conn);
      Instrument instrument = getInstrument(conn);

      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION_QC);

      DataReductionQCRoutinesConfiguration config = resourceManager
        .getDataReductionQCRoutinesConfiguration();

      // Load all the sensor values for this dataset
      DatasetSensorValues allSensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataSet.getId(), false, false);

      List<Measurement> measurements = DataSetDataDB.getMeasurements(conn,
        dataSet.getId());

      Map<Long, Map<Variable, ReadOnlyDataReductionRecord>> records = DataSetDataDB
        .getDataReductionData(conn, instrument, dataSet);

      FlaggedItems flaggedItems = new FlaggedItems();

      for (Variable var : instrument.getVariables()) {

        TreeMap<Measurement, ReadOnlyDataReductionRecord> variableRecords = new TreeMap<Measurement, ReadOnlyDataReductionRecord>();
        for (Measurement measurement : measurements) {
          Map<Variable, ReadOnlyDataReductionRecord> measurementRecords = records
            .get(measurement.getId());

          if (null != measurementRecords
            && measurementRecords.containsKey(var)) {
            variableRecords.put(measurement, measurementRecords.get(var));
          }
        }

        Class<? extends DataReducer> reducer = DataReducerFactory
          .getReducerClass(var.getName());

        List<DataReductionQCRoutine> routines = config.getRoutines(reducer);
        if (null != routines) {
          for (DataReductionQCRoutine routine : routines) {
            routine.qc(conn, instrument, dataSet, var, variableRecords,
              allSensorValues, flaggedItems);
          }
        }
      }

      DataSetDataDB.storeSensorValues(conn, flaggedItems.getSensorValues());
      DataSetDataDB.storeDataReductionQC(conn,
        flaggedItems.getDataReductionRecords());

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
    } catch (Exception e) {
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

}
