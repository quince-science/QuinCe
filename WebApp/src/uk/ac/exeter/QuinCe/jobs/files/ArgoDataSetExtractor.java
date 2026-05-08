package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.ArgoCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.ArgoDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.data.Dataset.NewSensorValues;
import uk.ac.exeter.QuinCe.data.Files.ArgoDataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ArgoDataSetExtractor extends DataSetExtractor {

  private static final String JOB_NAME = "Argo Dataset Extraction";

  @Override
  public void extract(Connection conn, Instrument instrument, DataSet dataSet)
    throws Exception {

    ArgoDataSet castDataset = (ArgoDataSet) dataSet;

    List<ArgoDataFile> allFiles = new ArrayList<DataFile>(
      DataFileDB.getFiles(conn, instrument)).stream().map(f -> (ArgoDataFile) f)
      .toList();

    List<ArgoDataFile> potentialFiles = ArgoDataFile.filter(allFiles,
      castDataset.getStartCycle(), castDataset.getEndCycle());

    usedFiles = new HashSet<DataFile>(potentialFiles.size());
    sensorValues = new NewSensorValues(dataSet);
    geoBounds = new GeoBounds();

    // Fixed columns
    SensorType cycleNumberSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Cycle Number");
    int cycleNumberColumn = instrument.getSensorAssignments()
      .get(cycleNumberSensorType).first().getColumn();

    SensorType profileSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Profile");
    int profileColumn = instrument.getSensorAssignments().get(profileSensorType)
      .first().getColumn();

    SensorType directionSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Direction");
    int directionColumn = instrument.getSensorAssignments()
      .get(directionSensorType).first().getColumn();

    SensorType levelSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Level");
    int levelColumn = instrument.getSensorAssignments().get(levelSensorType)
      .first().getColumn();

    SensorType pressureSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Pressure (Depth)");
    int pressureColumn = instrument.getSensorAssignments()
      .get(pressureSensorType).first().getColumn();

    SensorType sourceFileSensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType("Source File");
    int sourceFileColumn = instrument.getSensorAssignments()
      .get(sourceFileSensorType).first().getColumn();

    // Columns for variables and diagnostics
    Set<SensorAssignment> dataColumns = new TreeSet<SensorAssignment>();

    for (Variable variable : instrument.getVariables()) {
      for (SensorType sensorType : variable.getAllSensorTypes(false)) {
        dataColumns
          .add(instrument.getSensorAssignments().get(sensorType).first());
      }
    }

    dataColumns
      .addAll(instrument.getSensorAssignments().getDiagnosticSensors());

    // Process files
    for (DataFile file : potentialFiles) {
      int currentLine = file.getFirstDataLine();
      while (currentLine < file.getContentLineCount()) {

        List<String> line = file.getLine(currentLine);

        try {
          LocalDateTime timestamp = file.getFileDefinition()
            .getDateTimeSpecification().getDateTime(null, line);

          // Update the dataset's time properties
          dataSet.adjustTimeRange(timestamp);

          // Make Coordinate for line
          int cycleNumber = Integer.parseInt(line.get(cycleNumberColumn));

          // Only process the line if it's in the cycle range
          if (cycleNumber >= castDataset.getStartCycle()
            && cycleNumber <= castDataset.getEndCycle()) {

            usedFiles.add(file);

            int profile = Integer.parseInt(line.get(profileColumn));

            char direction;
            String directionString = line.get(directionColumn);
            if (directionString.toLowerCase().equals("d")) {
              direction = 'D';
            } else if (directionString.toLowerCase().equals("a")) {
              direction = 'A';
            } else {
              throw new IllegalArgumentException(
                "Invalid direction " + directionString);
            }

            int level = Integer.parseInt(line.get(levelColumn));
            double pressure = Double.parseDouble(line.get(pressureColumn));
            String sourceFile = line.get(sourceFileColumn);

            ArgoCoordinate coordinate = new ArgoCoordinate(dataSet.getId(),
              cycleNumber, profile, direction, level, pressure, sourceFile,
              timestamp);

            extractLongitude(dataSet, file, currentLine, line, coordinate);
            extractLatitude(dataSet, file, currentLine, line, coordinate);

            // Extract values for variables and diagnostics
            for (SensorAssignment assignment : dataColumns) {
              String fieldValue = file.getStringValue(JOB_NAME, dataSet,
                currentLine, line, assignment.getColumn(),
                assignment.getMissingValue());

              if (null != fieldValue) {
                sensorValues.create(assignment.getDatabaseId(), coordinate,
                  fieldValue);
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
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }
}
