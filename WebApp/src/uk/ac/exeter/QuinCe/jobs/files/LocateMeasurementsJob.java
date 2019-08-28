package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
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
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
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
 * Identifies individual measurements in a dataset and stores
 * them in the database. Followed by the ChooseSensorValues
 * job, which picks the SensorValues to use for that measurement
 *
 * @author Steve Jones
 *
 */
// TODO The detailed selection operations are not yet implemented.
public class LocateMeasurementsJob extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Locate Measurements";

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
  public LocateMeasurementsJob(ResourceManager resourceManager, Properties config,
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

      instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(),
        ResourceManager.getInstance().getSensorsConfiguration(),
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      // Get all the sensor values for the dataset, ordered by date and then
      // grouped by sensor type
      DateColumnGroupedSensorValues groupedSensorValues =
        DataSetDataDB.getSensorValuesByDateAndColumn(conn, instrument, dataSet.getId());

      // The list of measurements being built, to be stored in the database
      List<Measurement> measurements = new ArrayList<Measurement>(groupedSensorValues.size());

      // Go through each date in turn
      for (Map.Entry<LocalDateTime, Map<SensorType, List<SensorValue>>> entry :
        groupedSensorValues.entrySet()) {

        // See if there's a core value for each of the instrument's measured
        // variables for this date
        Map<SensorType, List<SensorValue>> sensorTypeGroups = entry.getValue();

        for (InstrumentVariable variable : instrument.getVariables()) {
          if (sensorTypeGroups.containsKey(variable.getCoreSensorType())) {

            // We have a value. Therefore we have a measurement.
            boolean measurementOK = true;

            // Get the Run Type for this measurement
            // We assume there's only one run type
            List<SensorValue> runTypeValues = sensorTypeGroups.get(SensorType.RUN_TYPE_SENSOR_TYPE);
            if (null == runTypeValues) {
              throw new RecordNotFoundException(
                "Missing Run Type for measurement at " + entry.getKey());
            }

            // Ditto for longitude and latitude
            List<SensorValue> longitudeValues = sensorTypeGroups.get(SensorType.LONGITUDE_SENSOR_TYPE);
            if (null == longitudeValues) {
              measurementOK = false;
            }

            List<SensorValue> latitudeValues = sensorTypeGroups.get(SensorType.LATITUDE_SENSOR_TYPE);
            if (null == latitudeValues) {
              measurementOK = false;
            }

            if (measurementOK) {
              // Only store non-ignored run types (assume only one run type)
              String runType = runTypeValues.get(0).getValue();
              double longitude = longitudeValues.get(0).getDoubleValue();
              double latitude = latitudeValues.get(0).getDoubleValue();

              if (!instrument.getRunTypeCategory(runType).equals(RunTypeCategory.IGNORED)) {
                measurements.add(new Measurement(dataSet.getId(), variable,
                  entry.getKey(), longitude, latitude, runType));
              }
            }
          }
        }
      }

      DataSetDataDB.storeMeasurements(conn, measurements);

      // Trigger the Build Measurements job
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);
      Map<String, String> jobParams = new HashMap<String, String>();
      jobParams.put(LocateMeasurementsJob.ID_PARAM, String.valueOf(Long.parseLong(parameters.get(ID_PARAM))));
      JobManager.addJob(dataSource, JobManager.getJobOwner(dataSource, id), DataReductionJob.class.getCanonicalName(), jobParams);

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
