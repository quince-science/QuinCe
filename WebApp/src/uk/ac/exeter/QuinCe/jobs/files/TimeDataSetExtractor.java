package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.NewSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriod;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.TimeDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.TimeDataFile;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.SensorCalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.TimeRange;
import uk.ac.exeter.QuinCe.utils.TimeRangeBuilder;

public class TimeDataSetExtractor extends DataSetExtractor {

  /**
   * Name of the job, used for reporting
   */
  private static final String JOB_NAME = "Time Basis Dataset Extraction";

  public void extract(Connection conn, Instrument instrument, DataSet dataSet)
    throws Exception {

    TimeDataSet castDataSet = (TimeDataSet) dataSet;

    // If the new dataset overlaps the NRT dataset, mark it for deletion.
    // It will get removed and recreated by the NRT scripts outside QuinCe
    TimeDataSet nrtDataset = (TimeDataSet) DataSetDB.getNrtDataSet(conn,
      dataSet.getInstrumentId());
    if (null != nrtDataset && TimeDataSet.overlap(nrtDataset, castDataSet)) {
      DataSetDB.setNrtDatasetStatus(conn, instrument, DataSet.STATUS_REPROCESS);
    }

    List<TimeDataFile> allFiles = new ArrayList<DataFile>(
      DataFileDB.getFiles(conn, instrument)).stream().map(f -> (TimeDataFile) f)
      .toList();

    List<TimeDataFile> potentialFiles = TimeDataFile.filter(allFiles,
      castDataSet.getStartTime(), castDataSet.getEndTime(), true);

    usedFiles = new HashSet<DataFile>(potentialFiles.size());
    sensorValues = new NewSensorValues(dataSet);

    if (!dataSet.fixedPosition()) {
      geoBounds = new GeoBounds();
    }

    // We want to store when run types begin and end
    RunTypePeriods runTypePeriods = new RunTypePeriods();

    CalibrationSet sensorCalibrations = SensorCalibrationDB.getInstance()
      .getCalibrationSet(conn, castDataSet);

    // Adjust the DataSet bounds to the latest start date and earliest end
    // date of each file definition, if the dataset range is beyond them
    Map<FileDefinition, TimeRangeBuilder> fileDefinitionRanges = new HashMap<FileDefinition, TimeRangeBuilder>();
    instrument.getFileDefinitions()
      .forEach(fd -> fileDefinitionRanges.put(fd, new TimeRangeBuilder()));

    for (DataFile file : potentialFiles) {
      if (!(file instanceof TimeDataFile)) {
        throw new IllegalArgumentException("File of wrong type");
      }

      TimeDataFile castFile = (TimeDataFile) file;

      fileDefinitionRanges.get(castFile.getFileDefinition()).add(castFile);
    }

    LocalDateTime filesLatestStart = TimeRange
      .getLatestStart(fileDefinitionRanges.values(), 3600);
    if (filesLatestStart.isAfter(castDataSet.getStartTime())) {
      castDataSet.setStartTime(filesLatestStart);
    }

    LocalDateTime filesEarliestEnd = TimeRange
      .getEarliestEnd(fileDefinitionRanges.values(), 3600);
    if (filesEarliestEnd.isBefore(castDataSet.getEndTime())) {
      castDataSet.setEndTime(filesEarliestEnd);
    }

    for (DataFile file : potentialFiles) {
      FileDefinition fileDefinition = file.getFileDefinition();
      int currentLine = file.getFirstDataLine();
      while (currentLine < file.getContentLineCount()) {

        try {
          List<String> line = file.getLine(currentLine);

          // Check the number of columns on the line
          boolean checkColumnCount = true;

          if (fileDefinition.hasRunTypes()) {
            try {
              RunTypeCategory runType = null;

              RunTypeAssignment runTypeAssignment = fileDefinition
                .getRunType(line, true);

              if (null != runTypeAssignment) {
                runType = runTypeAssignment.getCategory();
              }

              if (null != runType && runType.equals(RunTypeCategory.IGNORED)) {
                checkColumnCount = false;
              }
            } catch (FileDefinitionException e) {
              dataSet.addProcessingMessage(JOB_NAME, file, currentLine, e);
              if (e instanceof MissingRunTypeException) {
                dataSet.addProcessingMessage(JOB_NAME, file, currentLine,
                  "Unrecognised Run Type");
              }
            }
          }

          if (checkColumnCount
            && line.size() != fileDefinition.getColumnCount()) {
            dataSet.addProcessingMessage(JOB_NAME, file, currentLine,
              "Incorrect number of columns");
          }

          LocalDateTime time = ((TimeDataFile) file).getOffsetTime(line);
          TimeCoordinate coordinate = new TimeCoordinate(dataSet.getId(), time);

          if ((time.equals(castDataSet.getStartTime())
            || time.isAfter(castDataSet.getStartTime()))
            && (time.isBefore(castDataSet.getEndTime())
              || time.isEqual(castDataSet.getEndTime()))) {

            // We're using this file
            usedFiles.add(file);

            if (!dataSet.fixedPosition() && fileDefinition.hasPosition()) {
              extractLongitude(dataSet, file, currentLine, line, coordinate);
              extractLatitude(dataSet, file, currentLine, line, coordinate);
            }

            // Assigned columns
            for (Entry<SensorType, TreeSet<SensorAssignment>> entry : instrument
              .getSensorAssignments().entrySet()) {

              for (SensorAssignment assignment : entry.getValue()) {
                if (assignment.getDataFile()
                  .equals(fileDefinition.getFileDescription())) {

                  // For run types, follow all aliases
                  if (entry.getKey().equals(SensorType.RUN_TYPE_SENSOR_TYPE)) {

                    RunTypeAssignment runTypeValue = file.getFileDefinition()
                      .getRunType(line, true);

                    if (null != runTypeValue) {
                      String runType = runTypeValue.getRunName();

                      sensorValues.create(assignment.getDatabaseId(),
                        coordinate, runType);

                      runTypePeriods.add(runType, time);
                    }
                  } else {

                    // Create the SensorValue object
                    String fieldValue = null;

                    fieldValue = file.getStringValue(JOB_NAME, dataSet,
                      currentLine, line, assignment.getColumn(),
                      assignment.getMissingValue());

                    if (null != fieldValue) {
                      SensorValue sensorValue = sensorValues.create(
                        assignment.getDatabaseId(), coordinate, fieldValue);

                      // Apply calibration if required
                      Calibration sensorCalibration = sensorCalibrations
                        .getCalibrations(time)
                        .get(String.valueOf(assignment.getDatabaseId()));

                      if (null != sensorCalibration) {
                        sensorValue.calibrateValue(sensorCalibration);
                      }
                    }
                  }
                }
              }
            }
          }
        } catch (Throwable e) {
          // Log the error but continue with the next line
          dataSet.addProcessingMessage(JOB_NAME, file, currentLine, e);
        }

        currentLine++;
      }
    }

    // The last run type will cover the rest of time
    runTypePeriods.finish();

    // Now flag all the values that have internal calibrations and are within
    // the instrument's pre- and post-flushing periods (if they're defined),
    // or are in an INGORED run type
    if (runTypePeriods.size() > 0) {
      RunTypePeriod currentPeriod = runTypePeriods.get(0);
      int currentPeriodIndex = 0;

      Iterator<SensorValue> valuesIter = sensorValues.iterator();
      while (valuesIter.hasNext()) {
        SensorValue value = valuesIter.next();
        SensorType sensorType = instrument.getSensorAssignments()
          .getSensorTypeForDBColumn(value.getColumnId());

        if (sensorType.hasInternalCalibration()) {
          boolean periodFound = false;

          // Make sure we have the correct run type period
          while (!periodFound) {

            // If we have multiple file definitions, it's possible that
            // timestamps in the file where the run type *isn't* defined will
            // fall between run types.
            //
            // In this case, simply use the next known run type. Otherwise we
            // find the run type that the timestamp is in.
            if (((TimeCoordinate) value.getCoordinate())
              .isBefore(currentPeriod.getStart())
              || currentPeriod.encompasses(
                ((TimeCoordinate) value.getCoordinate()).getTime())) {
              periodFound = true;
            } else {
              currentPeriodIndex++;
              currentPeriod = runTypePeriods.get(currentPeriodIndex);
            }
          }

          // If the current period is an IGNORE run type, remove the value.
          // We can only tell this for "Generic" instruments, ie those with a
          // Run Type column
          if (instrument
            .getRunTypeCategory(Measurement.RUN_TYPE_DEFINES_VARIABLE,
              currentPeriod.getRunType())
            .equals(RunTypeCategory.IGNORED)) {
            valuesIter.remove();
          } else if (inFlushingPeriod(value.getCoordinate(), currentPeriod,
            instrument)) {

            // Flag flushing values
            value.setUserQC(FlagScheme.FLUSHING_FLAG, "");
          }
        }
      }
    }
  }

  private boolean inFlushingPeriod(Coordinate coordinate,
    RunTypePeriod runTypePeriod, Instrument instrument)
    throws MissingRunTypeException, RunTypeCategoryException {

    int flushingTime = instrument.getFlushingTime(runTypePeriod.getRunType());
    return (flushingTime > 0
      && DateTimeUtils.secondsBetween(runTypePeriod.getStart(),
        coordinate.getTime()) <= flushingTime);
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }
}
