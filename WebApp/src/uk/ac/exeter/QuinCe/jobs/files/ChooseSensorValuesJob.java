package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DateColumnGroupedSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
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
 * Job to locate the SensorValues to use for each measurement extracted
 * in the LocateMeasurementsJob. At the moment this simply finds one
 * SensorValue of each type with the same timestamp as the measurement.
 * In future it can get more sophisticated.
 *
 * @author Steve Jones
 *
 */
public class ChooseSensorValuesJob extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Choose Sensor Values";

  /**
   * The data set being processed by the job
   */
  private DataSet dataSet = null;

  /**
   * The instrument to which the data set belongs
   */
  private Instrument instrument = null;

  /**
   * Constructor that allows the {@link JobManager} to create an instance of this job.
   * @param resourceManager The application's resource manager
   * @param config The application configuration
   * @param jobId The database ID of the job
   * @param parameters The job parameters
   * @throws MissingParamException If any parameters are missing
   * @throws InvalidJobParametersException If any of the job parameters are invalid
   * @throws DatabaseException If a database occurs
   * @throws RecordNotFoundException If any required database records are missing
   * @see JobManager#getNextJob(ResourceManager, Properties)
   */
  public ChooseSensorValuesJob(ResourceManager resourceManager, Properties config,
    long jobId, Map<String, String> parameters)
      throws MissingParamException, InvalidJobParametersException,
        DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Get the data set from the database
      dataSet = DataSetDB.getDataSet(conn, Long.parseLong(parameters.get(ID_PARAM)));

      SensorsConfiguration sensorConfig =
        ResourceManager.getInstance().getSensorsConfiguration();

      instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(),
        sensorConfig,
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      List<Measurement> measurements =
        DataSetDataDB.getMeasurements(conn, instrument, dataSet.getId());

      List<MeasurementValue> measurementValues =
        new ArrayList<MeasurementValue>();

      // Get all the sensor values for the dataset, ordered by date and then
      // grouped by sensor type
      DateColumnGroupedSensorValues groupedSensorValues =
        DataSetDataDB.getSensorValuesByDateAndColumn(conn,
          instrument, dataSet.getId());

      // Loop through each measurement and grab the relevant sensor values
      for (Measurement measurement : measurements) {

        // Get all the sensor values for the measurement time
        Map<SensorType, List<SensorValue>> values =
          groupedSensorValues.get(measurement.getTime());

        // Get one sensor value for each sensor type.
        // TODO This will need to check averages, fallbacks
        //      and QC flags in future
        for (Map.Entry<SensorType, List<SensorValue>> entry :
          values.entrySet()) {

          // We want sensor values from every diagnostic and each of the
          // required sensor types
          //
          // For the moment we just grab the first available value from
          // each SensorType. This will be made more sophisticated in future
          if (entry.getKey().isDiagnostic() ||
            sensorConfig.requiredForVariable(entry.getKey(),
              measurement.getVariable())) {

            measurementValues.add(new MeasurementValue(measurement,
              entry.getValue().get(0)));
          }
        }
      }

      DataSetDataDB.storeMeasurementValues(conn, measurementValues);

      // Trigger the Build Measurements job
      /*
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);
      Map<String, String> jobParams = new HashMap<String, String>();
      jobParams.put(LocateMeasurementsJob.ID_PARAM, String.valueOf(Long.parseLong(parameters.get(ID_PARAM))));
      JobManager.addJob(dataSource, JobManager.getJobOwner(dataSource, id), ChooseSensorValuesJob.class.getCanonicalName(), jobParams);
      */

      conn.commit();
    } catch (Exception e) {
      e.printStackTrace();
      DatabaseUtils.rollBack(conn);
      try {
        // Set the dataset to Error status
        dataSet.setStatus(DataSet.STATUS_ERROR);
        // And add a (friendly) message...
        StringBuffer message = new StringBuffer();
        message.append(getJobName());
        message.append(" - error: ");
        message.append(e.getMessage());
        dataSet.addMessage(message.toString(), ExceptionUtils.getStackTrace(e));
        DataSetDB.updateDataSet(conn, dataSet);
        conn.commit();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
      throw new JobFailedException(id, e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {
    // TODO Auto-generated method stub

  }

  @Override
  public String getJobName() {
    return jobName;
  }
}
