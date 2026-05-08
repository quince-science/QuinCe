package uk.ac.exeter.QuinCe.data.Files;

import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * {@link DataFile} instance for Argo data.
 */
public class ArgoDataFile extends DataFile {

  /**
   * The first cycle number in the file.
   */
  private int startCycle = -1;

  /**
   * The last cycle number in the file.
   */
  private int endCycle = -1;

  /**
   * Constructor for a new file that is not yet in the database.
   *
   * @param instrument
   * @param fileDefinition
   * @param filename
   * @param contents
   * @throws MissingParamException
   * @throws DataFileException
   */
  public ArgoDataFile(Instrument instrument, FileDefinition fileDefinition,
    String filename, FileContents contents)
    throws MissingParamException, DataFileException {

    super(instrument, fileDefinition, filename, contents);
  }

  /**
   * Constructor from a file retrieved from the database.
   *
   * @param id
   * @param instrument
   * @param fileDefinition
   * @param filename
   * @param recordCount
   * @param properties
   */
  public ArgoDataFile(long id, Instrument instrument,
    FileDefinition fileDefinition, String filename, String startCycle,
    String endCycle, int recordCount, Properties properties) {

    super(id, instrument, fileDefinition, filename, recordCount, properties);
    this.startCycle = Integer.parseInt(startCycle);
    this.endCycle = Integer.parseInt(endCycle);
  }

  @Override
  public int compareTo(DataFile o) {
    if (!(o instanceof ArgoDataFile)) {
      throw new IllegalArgumentException(
        "Cannot compare DataFile of different type");
    }

    return startCycle - ((ArgoDataFile) o).startCycle;
  }

  @Override
  public String getStartString() throws DataFileException {
    if (startCycle == -1) {
      extractCycleRange();
    }

    return String.valueOf(startCycle);
  }

  @Override
  public String getEndString() throws DataFileException {
    if (endCycle == -1) {
      extractCycleRange();
    }

    return String.valueOf(endCycle);
  }

  @Override
  public TreeSet<DataFile> getOverlappingFiles(TreeSet<DataFile> allFiles) {
    TreeSet<DataFile> result = new TreeSet<DataFile>();

    for (DataFile file : allFiles) {
      if (!(file instanceof ArgoDataFile)) {
        throw new IllegalArgumentException("Not an ArgoDataFile");
      }

      ArgoDataFile castFile = (ArgoDataFile) file;

      if (castFile.endCycle >= this.startCycle
        && castFile.startCycle <= this.startCycle) {
        result.add(castFile);
      }
    }

    return result;
  }

  @Override
  public boolean hasFundametalProcessingIssue() {
    return false;
  }

  @Override
  public String getFundamentalProcessingIssueItem() {
    return "unknown";
  }

  /**
   * Extract the start and end cycle number from the file.
   *
   * <p>
   * We assume that the file is arranged in order of ascending cycle number,
   * since that's how we've written the netCDF to CSV converter.
   * </p>
   *
   * @throws DataFileException
   */
  private void extractCycleRange() throws DataFileException {

    startCycle = -1;
    endCycle = -1;

    try {

      SensorType cycleSensorType = ResourceManager.getInstance()
        .getSensorsConfiguration().getSensorType("Cycle Number");

      int cycleColumn = getInstrument().getSensorAssignments()
        .get(cycleSensorType).first().getColumn();

      int firstLine = getFirstDataLine();
      int lastLine = getContentLineCount();

      // Search for the start cycle
      int searchLine = firstLine;
      while (startCycle == -1 && searchLine <= lastLine) {
        String cycle = fileDefinition
          .extractFields(getContents().get(searchLine)).get(cycleColumn);

        if (!StringUtils.isEmpty(cycle)) {
          startCycle = Integer.parseInt(cycle);
          // The while statement will break now.
        }
        searchLine++;
      }

      // Now the end cycle
      searchLine = lastLine - 1;
      while (endCycle == -1 && searchLine >= firstLine) {
        String cycle = fileDefinition
          .extractFields(getContents().get(searchLine)).get(cycleColumn);

        if (!StringUtils.isEmpty(cycle)) {
          endCycle = Integer.parseInt(cycle);
        }

        searchLine--;
      }
    } catch (Exception e) {
      throw new DataFileException("Error extracting cycle number info", e);
    }
  }

  public static List<ArgoDataFile> filter(List<ArgoDataFile> allFiles,
    int start, int end) {

    return allFiles.stream()
      .filter(f -> f.endCycle >= start && f.startCycle <= end).toList();
  }
}
