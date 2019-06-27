package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.QCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.Routine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * <p>
 *   This {@link Job} class runs a set of QC routines on the sensor values for
 *   a given data set.
 * </p>
 *
 * <p>
 *   Once the QC has been completed, the QC Flag and QC Message are set. The QC flag will be set to {@link Flag#GOOD},
 *   {@link Flag#QUESTIONABLE} or {@link Flag#BAD}. In the latter two cases, the QC Message will contain details of the fault(s)
 *   that were found.
 * </p>
 *
 * <p>
 *   If the QC flag was set to {@link Flag#GOOD}, the WOCE flag for the record will be set to {@link Flag#ASSUMED_GOOD},
 *   to indicate that the software will assume that the record is good unless the user overrides it. Otherwise
 *   the WOCE Flag will be set to {@link Flag#NEEDED}. The user will be required to manually choose a value for the WOCE
 *   Flag, either by accepting the suggestion from the QC job, or overriding the flag and choosing their own. The WOCE
 *   Comment will default to being identical to the QC Message, but this can also be changed if required.
 * </p>
 *
 * <p>
 *   If the {@code AutoQCJob} has been run before, some WOCE Flags and Comments will have already been set by the user.
 *   If the user QC flag is anything other than {@link Flag#ASSUMED_GOOD} or {@link Flag#NEEDED}, it will not be checked.
 * </p>
 *
 * @author Steve Jones
 * @see Flag
 * @see Message
 */
public class AutoQCJob extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Automatic Quality Control";

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
  public AutoQCJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);
  }

  /**
   * Runs the configured QC routines on the file specified in the job parameters.
   * @param thread The thread that is running this job
   * @see FileJob#FILE_ID_KEY
   */
  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    // After automatic QC, all measurements must be recalculated.
    // Therefore before we start, destroy any existing measurements
    // in the data set
    clearMeasurements();

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Get the data set from the database
      dataSet = DataSetDB.getDataSet(conn, Long.parseLong(parameters.get(ID_PARAM)));

      instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(),
        ResourceManager.getInstance().getSensorsConfiguration(),
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      QCRoutinesConfiguration qcRoutinesConfig =
        ResourceManager.getInstance().getQCRoutinesConfiguration();

      // Get the sensor values grouped by data file column
      Map<Long, List<SensorValue>> sensorValues =
        DataSetDataDB.getSensorValuesByColumn(conn, dataSet.getId());


      // Run the routines for each column
      for (Map.Entry<Long, List<SensorValue>> entry : sensorValues.entrySet()) {
        SensorType sensorType = sensorAssignments.getSensorTypeForDBColumn(entry.getKey());

        List<SensorValue> values = entry.getValue();
        SensorValue.clearAutoQC(values);


        // If this sensor uses internal calibration,
        // see if there are run types for that variable. If so, filter the sensor values
        if (sensorType.hasInternalCalibration()) {
          // TODO This whole section is ugly as hell. Make it less so, as Picard never said.

          List<InstrumentVariable> sensorVariables = instrument.getSensorVariables(sensorType);
          List<String> measurementRunTypes = null;

          for (InstrumentVariable variable : sensorVariables) {
            Map<Long, List<String>> variableRunTypes = instrument.getVariableRunTypes(variable);
            if (variableRunTypes.size() > 0 && variableRunTypes.containsKey(variable.getId())) {
              measurementRunTypes = variableRunTypes.get(variable.getId());
            }
          }

          if (null != measurementRunTypes) {
            List<SensorAssignment> runTypeColumns = sensorAssignments.get(SensorType.RUN_TYPE_SENSOR_TYPE);

            // Only one of the run type columns can contain run types for the variable
            long runTypeColumnID = -1;

            for (SensorAssignment runTypeColumn : runTypeColumns) {
              if (SensorValue.contains(sensorValues.get(runTypeColumn.getDatabaseId()), measurementRunTypes.get(0))) {
                runTypeColumnID = runTypeColumn.getDatabaseId();
              }
            }

            if (runTypeColumnID == -1) {
              throw new JobException("Cannot find column containing run type '" + measurementRunTypes.get(0));
            }

            List<SensorValue> runTypes = sensorValues.get(runTypeColumnID);
            int currentRunTypeIndex = 0;
            SensorValue currentRunType = runTypes.get(0);

            List<SensorValue> filteredValues = new ArrayList<SensorValue>(values.size());
            for (SensorValue testValue : values) {

              if (currentRunType.getTime().isAfter(testValue.getTime())) {
                // There is no run type for the current test value, so skip it
                continue;
              } else {
                boolean currentRunTypeFound = false;
                while (!currentRunTypeFound) {
                  if (currentRunType.getTime().equals(testValue.getTime())) {
                    currentRunTypeFound = true;
                  } else if (currentRunTypeIndex < runTypes.size() -1 && runTypes.get(currentRunTypeIndex + 1).getTime().isBefore(testValue.getTime())) {
                    currentRunTypeIndex++;
                    currentRunType = runTypes.get(currentRunTypeIndex);
                  } else {
                    currentRunTypeFound = true;
                  }
                }
              }

              if (measurementRunTypes.contains(currentRunType.getValue())) {
                filteredValues.add(testValue);
              } else {
                testValue.setUserQC(Flag.NO_QC, null);
              }
            }

            values = filteredValues;
          }
        }

        for (Routine routine : qcRoutinesConfig.getRoutines(sensorType)) {
          routine.qcValues(values);
        }
      }

      // Store all the sensor values
      List<SensorValue> allValues = new ArrayList<SensorValue>();
      sensorValues.values().forEach(allValues::addAll);
      DataSetDataDB.storeSensorValues(conn, allValues);

      // Trigger the Build Measurements job
      dataSet.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(conn, dataSet);
      Map<String, String> jobParams = new HashMap<String, String>();
      jobParams.put(LocateMeasurementsJob.ID_PARAM, String.valueOf(Long.parseLong(parameters.get(ID_PARAM))));
      JobManager.addJob(dataSource, JobManager.getJobOwner(dataSource, id), LocateMeasurementsJob.class.getCanonicalName(), jobParams);

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

  private void clearMeasurements() throws JobFailedException {
    try {
      DataSetDataDB.deleteMeasurements(
        dataSource, Long.parseLong(parameters.get(ID_PARAM)));
    } catch (Exception e) {
      throw new JobFailedException(Long.parseLong(parameters.get(ID_PARAM)),
        "Failed to clear previous measurement data", e);
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
