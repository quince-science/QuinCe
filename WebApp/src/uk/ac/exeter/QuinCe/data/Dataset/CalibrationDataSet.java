package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a set of calibration data records for a data set. This is
 * a standard list with some extra functions for data reduction routines
 * @author Steve Jones
 *
 */
@Deprecated
public class CalibrationDataSet {

  /**
   * Parameter to indicate searches must find the record before a date
   */
  public static final int BEFORE = -1;

  /**
   * Parameter to indicate searches must find the record after a date
   */
  public static final int AFTER = 1;

  /**
   * The calibration records grouped by run type
   */
  private Map<String, List<DataSetRawDataRecord>> records;

  /**
   * A cache for records found by the {{@link #getSurroundingCalibrations(LocalDateTime, String)} method
   */
  private Map<String, List<DataSetRawDataRecord>> foundRecords;

  /**
   * Indicates whether or not records have been added to this data set
   */
  private boolean hasData = false;

  /**
   * Basic constructor
   */
  protected CalibrationDataSet() {
    records = new HashMap<String, List<DataSetRawDataRecord>>();
    foundRecords = new HashMap<String, List<DataSetRawDataRecord>>();
  }

  /**
   * Add a measurement to the data set
   * @param measurement The measurement
   */
  protected void add(DataSetRawDataRecord measurement) {
    String runType = measurement.getRunType().toLowerCase();
    if (!records.containsKey(runType)) {
      records.put(runType, new ArrayList<DataSetRawDataRecord>());
    }

    records.get(runType).add(measurement);
    hasData = true;
  }

  /**
   * Indicates whether or not records have been added to the data set
   * @return {@code true} if there are records; {@code false} if not
   */
  public boolean hasData() {
    return hasData;
  }

  /**
   * Get the calibration record that relates to the specified calibration target, and
   * which was recorded immediately before the specified date
   * @param recordDate The date
   * @param calibrationTarget The calibration target
   * @return The matching record
   */
/*  public DataSetRawDataRecord getCalibrationBefore(LocalDateTime recordDate, String calibrationTarget) {
    return searchForClosest(records.get(calibrationTarget.toLowerCase()), recordDate, BEFORE);
  }
*/
  /**
   * Get the calibration record that relates to the specified calibration target, and
   * which was recorded immediately before the specified date
   * @param recordDate The date
   * @param calibrationTarget The calibration target
   * @return The matching record
   */
/*  public DataSetRawDataRecord getCalibrationAfter(LocalDateTime recordDate, String calibrationTarget) {
    return searchForClosest(records.get(calibrationTarget.toLowerCase()), recordDate, AFTER);
  }
*/

  public List<DataSetRawDataRecord> getSurroundingCalibrations(LocalDateTime date, String target) {
    List<DataSetRawDataRecord> result = null;

    String key = target.toLowerCase();
    boolean needSearch = false;

    if (!foundRecords.containsKey(key)) {
      needSearch = true;
    } else {
      // See if the cached records encompass the new date
      List<DataSetRawDataRecord> records = foundRecords.get(key);

      boolean firstRecordBefore = (null == records.get(0) || records.get(0).getDate().isBefore(date));
      boolean lastRecordAfter = (null == records.get(1) || records.get(1).getDate().isAfter(date));

      if (firstRecordBefore && lastRecordAfter) {
        result = records;
      } else {
        needSearch = true;
      }
    }

    if (needSearch) {
      result = findSurroundingCalibrations(date, key);
      foundRecords.put(key, result);
    }

    return result;
  }

  /**
   * Search for the calibration records either side of a given date for a given calibration target
   * Returns a list of two records (before and after) with the relevant record set to null if there
   * is no before/after record
   * @param date The date
   * @param target The calibration target
   * @return The surrounding records
   */
  private List<DataSetRawDataRecord> findSurroundingCalibrations(LocalDateTime date, String target) {
    List<DataSetRawDataRecord> result = new ArrayList<DataSetRawDataRecord>(2);

    List<DataSetRawDataRecord> calibrationRecords = records.get(target);
    if (calibrationRecords.get(0).getDate().isAfter(date)) {
      // All calibrations are after the target date
      result.add(null);
      result.add(calibrationRecords.get(0));
    } else if (calibrationRecords.get(calibrationRecords.size() - 1).getDate().isBefore(date)) {
      // All calibrations are before the target date
      result.add(calibrationRecords.get(calibrationRecords.size() - 1));
      result.add(null);
    } else {
      // TODO This should be a binary search (issue #1032)
      for (int i = 0; i < calibrationRecords.size(); i++) {
        if (calibrationRecords.get(i).getDate().isAfter(date)) {
          result.add(calibrationRecords.get(i - 1));
          result.add(calibrationRecords.get(i));
        }
      }
    }

    return result;
  }
}
