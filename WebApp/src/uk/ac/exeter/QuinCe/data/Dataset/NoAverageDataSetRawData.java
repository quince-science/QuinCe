package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileLine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Instance of the {@link DataSetRawData} class for an instrument
 * that performs no measurement averaging
 *
 * @author Steve Jones
 *
 */
@Deprecated
public class NoAverageDataSetRawData extends DataSetRawData {

  /**
   * The maximum allowable gap between matching lines, in seconds
   */
  private static final long MAX_DIFFERENCE = 30;

  /**
   * Constructor for the parent {@link DataSetRawData} class
   * @param dataSource A data source
   * @param dataSet The data set
   * @param instrument The instrument to which the data set belongs
   * @throws RecordNotFoundException If no data files are found within the data set
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws DataFileException If the data cannot be extracted from the files
   */
  public NoAverageDataSetRawData(DataSource dataSource, DataSet dataSet, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException, DataFileException {
    super(dataSource, dataSet, instrument);
  }

  @Override
  protected boolean lineSelectionsMatch(int file1, int file2) throws DataSetException {

    boolean match = false;

    try {
      List<Integer> file1Rows = selectedRows.get(file1);
      List<Integer> file2Rows = selectedRows.get(file2);

      if (null != file1Rows && null != file2Rows) {
        LocalDateTime file1Date = getLine(file1, file1Rows.get(0)).getDate();
        LocalDateTime file2Date = getLine(file2, file2Rows.get(0)).getDate();

        match = Math.abs(ChronoUnit.SECONDS.between(file1Date, file2Date)) <= MAX_DIFFERENCE;
      }
    } catch (Exception e) {
      throw new DataSetException(e);
    }

    return match;
  }

  @Override
  protected boolean selectNextRow(int fileIndex) throws DataSetException {

    boolean selected = false;

    try {
      int selectedLine = -1;
      DataFileLine otherLine = getOtherSelectedLine(fileIndex);

      // If there are no selected rows in other files, simply select
      // the next row here.
      if (null == otherLine) {
        selectedLine = linePositions.get(fileIndex);

        while (!selected) {
          selectedLine = getNextLine(fileIndex);
           if (selectedLine == EOF_VALUE) {
             break;
           } else {
             DataFileLine line = getLine(fileIndex, selectedLine);
             if (!line.isIgnored()) {
               selected = true;
             }
           }
        }
      } else {
        LocalDateTime otherDate = otherLine.getDate();
        long selectedRowDifference = Long.MAX_VALUE;
        int currentLine = getNextLine(fileIndex);

        while (!selected) {
          if (currentLine >= getFileSize(fileIndex) || currentLine == EOF_VALUE) {
            // We've gone off the end of the file, so stop
            break;
          }

          DataFileLine line = getLine(fileIndex, currentLine);
          if (line.isIgnored()) {
            currentLine = getNextLine(fileIndex);
          } else {
            LocalDateTime lineDate = line.getDate();

            long lineDifference = Math.abs(ChronoUnit.SECONDS.between(lineDate, otherDate));

            if (lineDifference == 0) {
              // We can't get any closer, so use this line
              selected = true;
              selectedLine = currentLine;
            } else if (lineDate.isBefore(otherDate)) {

              // If we've already selected a row, this must by definition
              // be closer to the target. Store it and see if there's any closer lines
              if (selectedLine != EOF_VALUE) {
                selectedLine = currentLine;
                selectedRowDifference = lineDifference;
                currentLine = getNextLine(fileIndex, currentLine);
              } else if (lineDifference > MAX_DIFFERENCE) {
                // If this line is before the other line but outside the limit, keep looking
                currentLine = getNextLine(fileIndex, currentLine);
              } else {
                // We are within the limit, but they may be other
                // lines that are closer. Select this row, and try the next one.
                selectedLine = currentLine;
                selectedRowDifference = lineDifference;
                currentLine = getNextLine(fileIndex, currentLine);
              }

            } else { // The line is after the target

              // If a line has been selected, it must be before the
              // target date. See if this line is closer
              if (selectedLine != -1) {
                if (lineDifference < selectedRowDifference) {
                  // This line is closer, so use it
                  selectedLine = currentLine;
                  selected = true;
                } else {
                  // Use the previously found row
                  selected = true;
                }
              } else {
                // Select this row
                selectedLine = currentLine;
                selected = true;
              }
            }
          }
        }
      }

      if (selected) {
        List<Integer> rowSelection = new ArrayList<Integer>(1);
        rowSelection.add(selectedLine);
        selectedRows.set(fileIndex, rowSelection);
      } else {
        selectedRows.set(fileIndex, null);
      }

      linePositions.set(fileIndex, selectedLine);
    } catch (Exception e) {
      throw new DataSetException(e);
    }

    return selected;
  }

  @Override
  protected LocalDateTime getSelectedTime() throws DataFileException, DateTimeSpecificationException {
    return getLine(getCoreFileIndex(), selectedRows.get(getCoreFileIndex()).get(0)).getDate();
  }

  @Override
  protected double getSelectedLongitude() throws DataFileException, PositionException {
    return getLine(getPositionFileIndex(), selectedRows.get(getPositionFileIndex()).get(0)).getLongitude();
  }

  @Override
  protected double getSelectedLatitude() throws DataFileException, PositionException {
    return getLine(getPositionFileIndex(), selectedRows.get(getPositionFileIndex()).get(0)).getLatitude();
  }

  @Override
  protected Double calculateValue(List<Double> rowValues) {
    return rowValues.get(0);
  }

}
