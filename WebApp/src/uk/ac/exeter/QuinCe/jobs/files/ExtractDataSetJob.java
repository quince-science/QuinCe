package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.lang3.exception.ExceptionUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Job to extract the data for a data set from the uploaded data files
 *
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
    long jobId, Map<String, String> parameters) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Get the new data set from the database
      dataSet = DataSetDB.getDataSet(conn,
        Long.parseLong(parameters.get(ID_PARAM)));
      dataSet.setStatus(DataSet.STATUS_DATA_EXTRACTION);
      DataSetDB.updateDataSet(conn, dataSet);

      Instrument instrument = InstrumentDB.getInstrument(conn,
        dataSet.getInstrumentId(),
        ResourceManager.getInstance().getSensorsConfiguration(),
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      // Delete any existing NRT dataset, unless we're processing it.
      // The odds are that the new dataset will replace it
      if (instrument.getNrt() && !dataSet.isNrt()) {
        DataSetDB.deleteNrtDataSet(conn, dataSet.getInstrumentId());
      }

      // Reset the data set and all associated data
      reset(conn);
      conn.commit();

      List<DataFile> files = DataFileDB.getDataFiles(conn,
        ResourceManager.getInstance().getConfig(),
        dataSet.getSourceFiles(conn));

      TreeSet<SensorValue> sensorValues = new TreeSet<SensorValue>();

      // We want to store when run types begin and end
      RunTypePeriods runTypePeriods = new RunTypePeriods();

      // Collect the true start and end times of the dataset based on the
      // actual data
      LocalDateTime realStartTime = null;
      LocalDateTime realEndTime = dataSet.getEnd();

      // Collect the data bounds
      double minLon = Double.MAX_VALUE;
      double maxLon = Double.MIN_VALUE;
      double minLat = Double.MAX_VALUE;
      double maxLat = Double.MIN_VALUE;

      for (DataFile file : files) {
        FileDefinition fileDefinition = file.getFileDefinition();

        int currentLine = file.getFirstDataLine();
        while (currentLine < file.getContentLineCount()) {

          List<String> line = file.getLine(currentLine);
          LocalDateTime time = file.getDate(line);

          if ((time.equals(dataSet.getStart())
            || time.isAfter(dataSet.getStart()))
            && (time.isBefore(dataSet.getEnd())
              || time.isEqual(dataSet.getEnd()))) {

            if (null == realStartTime && null != time) {
              realStartTime = time;
            }

            realEndTime = time;

            // We only process a line if it has a valid position
            boolean positionOK = true;
            double longitude = 0;
            double latitude = 0;

            // Position
            if (null != fileDefinition.getLongitudeSpecification()) {
              try {
                longitude = file.getLongitude(line);
                if (longitude < minLon) {
                  minLon = longitude;
                }
                if (longitude > maxLon) {
                  maxLon = longitude;
                }
              } catch (PositionException e) {
                System.out.println("File " + file.getDatabaseId() + ", Line "
                  + currentLine + ": PositionException: " + e.getMessage());
                positionOK = false;
              }
            }

            if (null != fileDefinition.getLongitudeSpecification()) {
              try {
                latitude = file.getLatitude(line);
                if (latitude < minLat) {
                  minLat = latitude;
                }
                if (latitude > maxLat) {
                  maxLat = latitude;
                }
              } catch (PositionException e) {
                System.out.println("File " + file.getDatabaseId() + ", Line "
                  + currentLine + ": PositionException: " + e.getMessage());
                positionOK = false;
              }
            }

            if (positionOK) {
              // Write the position values
              sensorValues.add(new SensorValue(dataSet.getId(),
                FileDefinition.LONGITUDE_COLUMN_ID, time,
                String.valueOf(longitude)));

              sensorValues.add(new SensorValue(dataSet.getId(),
                FileDefinition.LATITUDE_COLUMN_ID, time,
                String.valueOf(latitude)));

              // Assigned columns
              for (Entry<SensorType, List<SensorAssignment>> entry : instrument
                .getSensorAssignments().entrySet()) {

                for (SensorAssignment assignment : entry.getValue()) {
                  if (assignment.getDataFile()
                    .equals(fileDefinition.getFileDescription())) {

                    // For run types, follow all aliases
                    if (entry.getKey()
                      .equals(SensorType.RUN_TYPE_SENSOR_TYPE)) {
                      String runType = file.getFileDefinition()
                        .getRunType(line, true).getRunName();

                      sensorValues.add(new SensorValue(dataSet.getId(),
                        assignment.getDatabaseId(), time, runType));

                      runTypePeriods.add(runType, time);
                    } else {
                      sensorValues.add(new SensorValue(dataSet.getId(),
                        assignment.getDatabaseId(), time,
                        file.getStringValue(line, assignment.getColumn(),
                          assignment.getMissingValue())));
                    }
                  }
                }
              }
            }
          }

          currentLine++;
        }
      }

      // The last run type will cover the rest of time
      runTypePeriods.finish();

      // Now flag all the values that are within the instrument's pre-
      // and post-flushing periods (if they're defined)
      if (instrument.getPreFlushingTime() > 0
        || instrument.getPostFlushingTime() > 0) {

        RunTypePeriod currentPeriod = runTypePeriods.get(0);
        int currentPeriodIndex = 0;

        Iterator<SensorValue> valuesIter = sensorValues.iterator();
        while (valuesIter.hasNext()) {
          SensorValue value = valuesIter.next();

          boolean periodFound = false;

          // Make sure we have the correct run type period
          while (!periodFound) {

            // If we have multiple file definitions, it's possible that
            // timestamps
            // in the file where the run type *isn't* defined will fall between
            // run types.
            //
            // In this case, simply use the next known run type. Otherwise we
            // find
            // the run type that the timestamp is in.
            if (value.getTime().isBefore(currentPeriod.start)
              || currentPeriod.encompasses(value.getTime())) {
              periodFound = true;
            } else {
              currentPeriodIndex++;
              currentPeriod = runTypePeriods.get(currentPeriodIndex);
            }
          }

          if (inFlushingPeriod(value.getTime(), currentPeriod, instrument)) {
            value.setUserQC(Flag.FLUSHING, "");
          }
        }
      }

      // Store the remaining values
      if (sensorValues.size() > 0) {
        DataSetDataDB.storeSensorValues(conn, sensorValues);
      }

      // Adjust the Dataset limits to the actual extracted data
      if (null != realStartTime) {
        dataSet.setStart(realStartTime);
      }

      if (null != realEndTime) {
        dataSet.setEnd(realEndTime);
      }

      dataSet.setBounds(minLon, minLat, maxLon, maxLat);

      // Trigger the Auto QC job
      dataSet.setStatus(DataSet.STATUS_AUTO_QC);
      DataSetDB.updateDataSet(conn, dataSet);
      Map<String, String> jobParams = new HashMap<String, String>();
      jobParams.put(AutoQCJob.ID_PARAM,
        String.valueOf(Long.parseLong(parameters.get(ID_PARAM))));
      JobManager.addJob(dataSource, JobManager.getJobOwner(dataSource, id),
        AutoQCJob.class.getCanonicalName(), jobParams);

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

  private boolean inFlushingPeriod(LocalDateTime time,
    RunTypePeriod runTypePeriod, Instrument instrument) {

    boolean result = false;

    if (instrument.getPreFlushingTime() > 0
      && DateTimeUtils.secondsBetween(runTypePeriod.start, time) <= instrument
        .getPreFlushingTime()) {

      result = true;
    } else if (instrument.getPostFlushingTime() > 0
      && DateTimeUtils.secondsBetween(time, runTypePeriod.end) <= instrument
        .getPostFlushingTime()) {

      result = true;
    }

    return result;
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {
    // TODO Auto-generated method stub
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
  private void reset(Connection conn) throws MissingParamException,
    InvalidDataSetStatusException, DatabaseException, RecordNotFoundException {

    DataSetDataDB.deleteSensorValues(conn, dataSet.getId());
    DataSetDB.setDatasetStatus(conn, dataSet.getId(), DataSet.STATUS_WAITING);
  }

  @Override
  public String getJobName() {
    return jobName;
  }

  private class RunTypePeriod {

    private String runType;

    private LocalDateTime start;

    private LocalDateTime end;

    private RunTypePeriod(String runType, LocalDateTime start) {
      this.runType = runType;
      this.start = start;
      this.end = start;
    }

    private boolean encompasses(LocalDateTime time) {
      boolean result = false;
      if (start.equals(end)) {
        result = time.equals(start);
      } else {
        boolean afterStart = false;
        boolean beforeEnd = false;

        if (time.equals(start) || time.isAfter(start)) {
          afterStart = true;
        }

        if (time.equals(end) || time.isBefore(end)) {
          beforeEnd = true;
        }

        result = afterStart && beforeEnd;
      }

      return result;
    }
  }

  private class RunTypePeriods extends ArrayList<RunTypePeriod> {

    private RunTypePeriods() {
      super();
    }

    private void add(String runType, LocalDateTime time) {

      if (size() == 0) {
        add(new RunTypePeriod(runType, time));
      } else {
        RunTypePeriod currentPeriod = get(size() - 1);
        if (!currentPeriod.runType.equals(runType)) {
          add(new RunTypePeriod(runType, time));
        } else {
          currentPeriod.end = time;
        }
      }
    }

    /**
     * Signal that the last run type has been found
     */
    private void finish() {
      if (size() > 0) {
        get(size() - 1).end = LocalDateTime.MAX;
      }
    }
  }
}
