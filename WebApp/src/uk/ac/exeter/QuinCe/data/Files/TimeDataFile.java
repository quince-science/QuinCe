package uk.ac.exeter.QuinCe.data.Files;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.MissingDateTimeException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.TimeRange;

public class TimeDataFile extends DataFile implements TimeRange {

  public static final String TIME_OFFSET_PROP = "timeOffset";

  /**
   * The date in the file header
   */
  private LocalDateTime headerDate = null;

  /**
   * Indicates whether or not we have already extracted the header date.
   *
   * <p>
   * This will be set if we have examined the data file and determined that the
   * header date is not required.
   * </p>
   */
  private boolean headerDateExtracted = false;

  /**
   * The date/time of the first record in the file
   */
  private LocalDateTime startDate = null;

  /**
   * The date/time of the last record in the file
   */
  private LocalDateTime endDate = null;

  /**
   * Run types in this file not defined in the file definition
   */
  private Set<RunTypeAssignment> missingRunTypes = new HashSet<RunTypeAssignment>();

  /**
   * Create a DataFile with the specified definition and contents
   *
   * @param fileStore
   *          The location of the file store
   * @param fileDefinition
   *          The file format definition
   * @param filename
   *          The file name
   * @throws MissingParamException
   *           If any fields are null
   */
  public TimeDataFile(Instrument instrument, FileDefinition fileDefinition,
    String filename, FileContents contents)
    throws MissingParamException, DataFileException {

    super(instrument, fileDefinition, filename, contents);
  }

  public TimeDataFile(long id, Instrument instrument,
    FileDefinition fileDefinition, String filename, String start, String end,
    int recordCount, Properties properties) {

    super(id, instrument, fileDefinition, filename, recordCount, properties);
    this.startDate = DateTimeUtils.longToDate(Long.parseLong(start));
    this.endDate = DateTimeUtils.longToDate(Long.parseLong(end));
  }

  @Override
  protected Properties defaultProperties() {
    Properties result = new Properties();
    result.setProperty(TIME_OFFSET_PROP, "0");
    return result;
  }

  /**
   * Get the time of the first record in the file. Time offset will not be
   * applied. Lines with invalid/missing dates are ignored.
   *
   * @return The date, or null if the date cannot be retrieved
   */
  public LocalDateTime getRawStartTime() {
    if (null == startDate) {
      try {
        LocalDateTime foundDate = null;
        int searchLine = getFirstDataLine() - 1;
        int lastLine = getContentLineCount() - 1;

        while (null == foundDate && searchLine <= lastLine) {
          searchLine++;

          try {
            foundDate = getRawTime(searchLine);
          } catch (Exception e) {
            // Ignore errors and try the next line
          }
        }

        if (null != foundDate) {
          startDate = foundDate;
        } else {
          addMessage("No valid dates in file");
        }

      } catch (DataFileException e) {
        addMessage("Unable to extract data from file");
      }
    }

    return startDate;
  }

  public LocalDateTime getStart() {
    return getRawStartTime();
  }

  /**
   * Get the time of the last record in the file. Time offset will not be
   * applied.
   *
   * @return The date, or null if the date cannot be retrieved
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  public LocalDateTime getRawEndTime() {
    if (null == endDate) {
      try {
        LocalDateTime foundDate = null;
        int firstLine = getFirstDataLine();
        int searchLine = getContentLineCount();

        while (null == foundDate && searchLine >= firstLine) {
          searchLine--;

          try {
            foundDate = getRawTime(searchLine);
          } catch (Exception e) {
            // Ignore errors and try the next line
          }
        }

        if (null != foundDate) {
          endDate = foundDate;
        } else {
          addMessage("No valid dates in file");
        }

      } catch (DataFileException e) {
        addMessage("Unable to extract data from file");
      }
    }

    return endDate;
  }

  public LocalDateTime getEnd() {
    return getRawEndTime();
  }

  public LocalDateTime getOffsetStartTime() {
    return applyTimeOffset(getRawStartTime());
  }

  public LocalDateTime getOffsetEndTime() {
    return applyTimeOffset(getRawEndTime());
  }

  public LocalDateTime getStartTime(boolean applyOffset) {
    return applyOffset ? getOffsetStartTime() : getRawStartTime();
  }

  public LocalDateTime getEndTime(boolean applyOffset) {
    return applyOffset ? getOffsetEndTime() : getRawEndTime();
  }

  /**
   * Get the time of a line in the file, without the define offset applied
   *
   * @param line
   *          The line
   * @return The time
   * @throws DataFileException
   *           If any date/time fields are empty
   * @throws MissingDateTimeException
   */
  public LocalDateTime getRawTime(int line) throws DataFileException,
    DateTimeSpecificationException, MissingDateTimeException {
    return getRawTime(fileDefinition.extractFields(getContents().get(line)));
  }

  public LocalDateTime getOffsetTime(List<String> line)
    throws DataFileException, DateTimeSpecificationException,
    MissingDateTimeException {

    return applyTimeOffset(getRawTime(line));
  }

  public LocalDateTime getRawTime(List<String> line)
    throws DateTimeSpecificationException, MissingDateTimeException,
    DataFileException {

    return fileDefinition.getDateTimeSpecification()
      .getDateTime(getHeaderDate(), line);
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does
   * not contain run types
   *
   * @param line
   *          The line
   * @return The run type for the line
   * @throws DataFileException
   *           If the data cannot be extracted
   * @throws FileDefinitionException
   *           If the run types are invalid
   */
  public String getRunType(int line)
    throws DataFileException, FileDefinitionException {
    String runType = null;

    if (fileDefinition.hasRunTypes()) {
      runType = fileDefinition.getRunType(getContents().get(line), true)
        .getRunName();
    }

    return runType;
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does
   * not contain run types
   *
   * @param line
   *          The line
   * @return The run type for the line
   * @throws DataFileException
   *           If the data cannot be extracted
   * @throws FileDefinitionException
   *           If the run types are invalid
   */
  public RunTypeCategory getRunTypeCategory(int line)
    throws DataFileException, FileDefinitionException {
    RunTypeCategory runType = null;

    if (fileDefinition.hasRunTypes()) {
      runType = fileDefinition.getRunTypeCategory(getContents().get(line));
    }

    return runType;
  }

  /**
   * Calculate the mean length (in seconds) of collection of DataFiles.
   *
   * <p>
   * The length of each file is calculated as {@code endDate - startDate}, so
   * files with a single line will be zero seconds long.
   * </p>
   *
   * @param files
   *          The files.
   * @return The mean file length.
   */
  public static double getMeanFileLength(Collection<TimeDataFile> files) {
    MeanCalculator mean = new MeanCalculator();
    files.forEach(
      f -> mean.add(DateTimeUtils.secondsBetween(f.startDate, f.endDate)));
    return mean.mean();
  }

  /**
   * Determines whether or not this file overlaps the specified time period.
   *
   * <p>
   * The file's time offset is applied before the check.
   * </p>
   *
   * @param start
   *          The start of the period to check.
   * @param end
   *          The end of the period to check.
   * @return {@code true} if this file overlaps the time period; {@code false}
   *         if it does not.
   */
  public boolean overlaps(LocalDateTime start, LocalDateTime end) {
    return DateTimeUtils.overlap(getOffsetStartTime(), getOffsetEndTime(),
      start, end);
  }

  /**
   * Determines whether or not this file overlaps another file.
   *
   * <p>
   * The file's time offset is applied before the check.
   * </p>
   *
   * @param other
   *          The file to check against this file.
   * @return {@code true} if both files overlap; {@code false} if they do not.
   */
  public boolean overlaps(TimeDataFile other) {
    return DateTimeUtils.overlap(getOffsetStartTime(), getOffsetEndTime(),
      other.getOffsetStartTime(), other.getOffsetEndTime());
  }

  public void setTimeOffset(int seconds) {
    properties.setProperty(TIME_OFFSET_PROP, String.valueOf(seconds));
  }

  public int getTimeOffset() {
    return Integer.parseInt(properties.getProperty(TIME_OFFSET_PROP));
  }

  public boolean hasTimeOffset() {
    return getTimeOffset() != 0;
  }

  private LocalDateTime applyTimeOffset(LocalDateTime rawTime) {
    return rawTime.plusSeconds(getTimeOffset());
  }

  /**
   * Determine whether the supplied {@link Collection} of {@link DataFile}s
   * contains at least one period where there is an overlapping file for all the
   * {@link Instrument}'s {@link FileDefinitions} within the specified time
   * period.
   *
   * <p>
   * This method can tell us whether the set of {@link DataFile}s can be used to
   * create a {@link DataSet} covering the specified period.
   * </p>
   *
   * @param instrument
   *          The {@link Instrument} whose files are being examined.
   * @param files
   *          The files.
   * @param start
   *          The start time of the required period.
   * @param end
   *          The end time of the required period.
   * @return
   */
  public static boolean hasConcurrentFiles(Instrument instrument,
    TreeSet<DataFile> files, LocalDateTime start, LocalDateTime end) {

    boolean result = false;

    // If there's only one FileDefinition, we can just find any file with data
    // between the start and end.
    if (instrument.getFileDefinitions().size() == 1) {

      for (DataFile file : files) {
        if (!(file instanceof TimeDataFile)) {
          throw new IllegalArgumentException(
            "File " + file.getFilename() + " is of the wrong type");
        }

        TimeDataFile castFile = (TimeDataFile) file;
        if (castFile.overlaps(start, end)) {
          result = true;
          break;
        } else if (castFile.getOffsetStartTime().isAfter(end)) {
          // We can stop now - we've gone beyond the end time
          break;
        }
      }
    } else {
      // Build a Map of the files that encompass the required period
      // grouped by FileDefinition
      HashMap<FileDefinition, List<TimeDataFile>> map = new HashMap<FileDefinition, List<TimeDataFile>>();

      List<FileDefinition> fileDefinitions = instrument.getFileDefinitions();

      fileDefinitions.forEach(fd -> map.put(fd, new ArrayList<TimeDataFile>()));

      for (DataFile file : files) {
        if (!(file instanceof TimeDataFile)) {
          throw new IllegalArgumentException(
            "File " + file.getFilename() + " is of the wrong type");
        }

        TimeDataFile castFile = (TimeDataFile) file;
        if (castFile.overlaps(start, end)) {
          map.get(castFile.getFileDefinition()).add(castFile);
        }
      }

      for (TimeDataFile checkFile : map.get(fileDefinitions.get(0))) {

        boolean setComplete = true;

        for (int i = 1; i < instrument.getFileDefinitions().size(); i++) {
          boolean overlapFound = false;

          for (TimeDataFile defFile : map.get(fileDefinitions.get(i))) {
            if (checkFile.overlaps(defFile)) {
              overlapFound = true;
              break;
            }
          }

          if (!overlapFound) {
            setComplete = false;
            break;
          }
        }

        if (setComplete) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  private LocalDateTime getHeaderDate() throws DataFileException {
    if (!headerDateExtracted) {
      DateTimeSpecification dateTimeSpec = fileDefinition
        .getDateTimeSpecification();

      DateTimeColumnAssignment assignment = null;

      if (dateTimeSpec.isAssigned(DateTimeSpecification.HOURS_FROM_START)) {
        assignment = dateTimeSpec
          .getAssignment(DateTimeSpecification.HOURS_FROM_START);
      } else if (dateTimeSpec
        .isAssigned(DateTimeSpecification.SECONDS_FROM_START)) {
        assignment = dateTimeSpec
          .getAssignment(DateTimeSpecification.SECONDS_FROM_START);
      }

      if (null != assignment) {
        try {
          HighlightedString matchedLine = fileDefinition.getHeaderLine(
            getContents().get(), assignment.getPrefix(),
            assignment.getSuffix());
          headerDate = LocalDateTime.parse(matchedLine.getHighlightedPortion(),
            assignment.getFormatter());
        } catch (Exception e) {
          addMessage(
            "Could not extract file start date from header: " + e.getMessage());
          throw new DataFileException(getDatabaseId(), -1,
            "Could not extract file start date from header");
        }
      }

      headerDateExtracted = true;
    }

    return headerDate;
  }

  @Override
  public void validateWorker() throws DataFileException {
    int firstDataLine = getFirstDataLine();
    if (firstDataLine > -1) {

      // For each line in the file, check that:
      // (a) The date/time is monotonic in the file (bad date/times are ignored)
      // (b) Has the correct number of columns (for Run Types that aren't
      // IGNORED)
      // (c) The Run Type is recognised

      LocalDateTime lastDateTime = null;
      for (int lineNumber = firstDataLine; lineNumber < getContentLineCount(); lineNumber++) {
        String line = getContents().get(lineNumber);

        try {
          LocalDateTime dateTime = fileDefinition.getDateTimeSpecification()
            .getDateTime(headerDate, fileDefinition.extractFields(line));
          if (null != lastDateTime) {
            if (dateTime.compareTo(lastDateTime) <= 0) {
              addMessage(lineNumber, "Date/Time is not monotonic");
            }
          }
        } catch (MissingDateTimeException | DateTimeSpecificationException e) {
          // We don't mind bad dates in the file -
          // we'll just ignore those lines
        }

        // Check the run type
        if (fileDefinition.hasRunTypes()) {
          String runType = fileDefinition.getRunTypeValue(line);

          if (!fileDefinition.runTypeAssigned(runType)) {

            boolean alreadyProcessed = missingRunTypes.stream()
              .filter(mrt -> mrt.getRunName().equals(runType.toLowerCase()))
              .findAny().isPresent();

            if (!alreadyProcessed) {

              RunTypeAssignment guessedAssignment;

              if (null == previousInstruments) {
                try {
                  previousInstruments = getPreviousInstruments();
                } catch (Exception e) {
                  // Log the error, but continue.
                  // It's not important enough to break the workflow.
                  ExceptionUtils.printStackTrace(e);
                  previousInstruments = new ArrayList<Instrument>();
                }
              }

              RunTypeAssignment previousAssignment = RunTypeAssignments
                .getPreviousRunTypeAssignment(runType, previousInstruments);

              if (null != previousAssignment) {
                guessedAssignment = previousAssignment;
              } else {
                // Guess from presets
                RunTypeAssignment presetAssignment = RunTypeAssignments
                  .getPresetAssignment(instrument.getVariables(), runType,
                    fileDefinition.getRunTypes());
                if (null != presetAssignment) {
                  guessedAssignment = presetAssignment;
                } else {
                  guessedAssignment = new RunTypeAssignment(runType,
                    RunTypeCategory.IGNORED);
                }
              }

              missingRunTypes.add(guessedAssignment);
            }
          }
        }
      }
    }
  }

  /**
   * Get the list of run type values with the specified value excluded. This
   * list will include all the run types from the stored file definition plus
   * any missing run types (except that specified as the exclusion).
   *
   * @param exclusion
   *          The value to exclude from the list
   * @return The list of run types without the excluded value
   */
  public List<String> getRunTypeValuesWithExclusion(String exclusion) {
    List<String> runTypeValues = fileDefinition.getRunTypeValues();
    for (RunTypeAssignment runTypeAssignment : missingRunTypes) {
      if (!runTypeAssignment.getRunName().equals(exclusion)) {
        runTypeValues.add(runTypeAssignment.getRunName());
      }
    }

    return runTypeValues;
  }

  public List<RunTypeAssignment> getMissingRunTypes() {
    List<RunTypeAssignment> list = new ArrayList<>(missingRunTypes);
    Collections.sort(list);
    return list;
  }

  @Override
  public String getStartString() {
    return String.valueOf(DateTimeUtils.dateToLong(getRawStartTime()));
  }

  @Override
  public String getStartDisplayString() {
    StringBuilder result = new StringBuilder(
      DateTimeUtils.toIsoDate(getRawStartTime()));
    addOffsetInfo(result);
    return result.toString();
  }

  private void addOffsetInfo(StringBuilder string) {
    if (hasTimeOffset()) {
      string.append(" (");
      int offset = getTimeOffset();
      string.append(offset >= 0 ? '+' : '-');
      string.append(')');
    }
  }

  @Override
  public String getEndString() {
    return String.valueOf(DateTimeUtils.dateToLong(getRawEndTime()));
  }

  @Override
  public String getEndDisplayString() {
    StringBuilder result = new StringBuilder(
      DateTimeUtils.toIsoDate(getRawEndTime()));
    addOffsetInfo(result);
    return result.toString();
  }

  @Override
  public TreeSet<DataFile> getOverlappingFiles(TreeSet<DataFile> allFiles) {
    if (getRawEndTime().isBefore(getRawStartTime())) {
      throw new IllegalArgumentException("End must be >= start date");
    }

    TreeSet<DataFile> result = new TreeSet<DataFile>();

    for (DataFile file : allFiles) {
      if (!(file instanceof TimeDataFile)) {
        throw new IllegalArgumentException("Not a TimeDataFile");
      }

      TimeDataFile castFile = (TimeDataFile) file;

      if (castFile.getRawEndTime().isAfter(getRawStartTime())
        && castFile.getRawStartTime().isBefore(getRawEndTime())) {
        result.add(castFile);
      }
    }

    return result;
  }

  @Override
  public int compareTo(DataFile o) {
    if (!(o instanceof TimeDataFile)) {
      throw new IllegalArgumentException(
        "Cannot compare DataFile of different type");
    }

    return getRawStartTime().compareTo(((TimeDataFile) o).getRawStartTime());
  }

  @Override
  public Properties getExportProperties() {
    Properties props = new Properties();
    properties.setProperty(TIME_OFFSET_PROP, String.valueOf(getTimeOffset()));
    return props;
  }

  public static List<TimeDataFile> filter(List<TimeDataFile> allFiles,
    LocalDateTime start, LocalDateTime end, boolean applyOffset) {

    return allFiles.stream()
      .filter(f -> f.getEndTime(applyOffset).isAfter(start)
        && f.getStartTime(applyOffset).isBefore(end))
      .toList();
  }

  @Override
  public boolean hasFundametalProcessingIssue() {
    return null == getRawStartTime() || null == getRawEndTime();
  }

  @Override
  public String getFundamentalProcessingIssueItem() {
    return "date";
  }
}
