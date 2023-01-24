package uk.ac.exeter.QuinCe.web.datasets.plotPage.Position;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageDataException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQCData;

public class PositionQCData extends ManualQCData {

  private PlotPageColumnHeading timeHeading;

  private PlotPageColumnHeading longitudeHeading;

  private PlotPageColumnHeading latitudeHeading;

  protected PositionQCData(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SQLException {
    super(dataSource, instrument, dataset);
  }

  @Override
  public void loadDataAction() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      sensorValues = DataSetDataDB.getPositionSensorValues(conn, instrument,
        dataset.getId());
    }

    // Build the row IDs
    rowIDs = sensorValues.getPositionTimes().stream()
      .map(t -> DateTimeUtils.dateToLong(t)).collect(Collectors.toList());
  }

  @Override
  protected void buildColumnHeadings() {

    columnHeadings = new LinkedHashMap<String, List<PlotPageColumnHeading>>();
    extendedColumnHeadings = new LinkedHashMap<String, List<PlotPageColumnHeading>>();

    // Time
    List<PlotPageColumnHeading> rootColumns = new ArrayList<PlotPageColumnHeading>(
      1);

    timeHeading = new PlotPageColumnHeading(FileDefinition.TIME_COLUMN_HEADING,
      false, false, false);

    rootColumns.add(timeHeading);

    columnHeadings.put(ROOT_FIELD_GROUP, rootColumns);
    extendedColumnHeadings.put(ROOT_FIELD_GROUP, rootColumns);

    // Position - combined for view, separated for extended columns
    List<PlotPageColumnHeading> sensorColumns = new ArrayList<PlotPageColumnHeading>(
      1);
    List<PlotPageColumnHeading> extendedSensorColumns = new ArrayList<PlotPageColumnHeading>(
      2);

    sensorColumns
      .add(new PlotPageColumnHeading(FileDefinition.LONGITUDE_COLUMN_ID,
        "Position", "Position", "POSITION", null, true, false, true, false));

    longitudeHeading = new PlotPageColumnHeading(
      FileDefinition.LONGITUDE_COLUMN_HEADING, false, true, false);

    extendedSensorColumns.add(longitudeHeading);

    latitudeHeading = new PlotPageColumnHeading(
      FileDefinition.LATITUDE_COLUMN_HEADING, false, true, false,
      FileDefinition.LONGITUDE_COLUMN_ID);

    extendedSensorColumns.add(latitudeHeading);

    columnHeadings.put(SENSORS_FIELD_GROUP, sensorColumns);
    extendedColumnHeadings.put(SENSORS_FIELD_GROUP, extendedSensorColumns);
  }

  protected PlotPageColumnHeading getDefaultXAxis1() throws Exception {
    return longitudeHeading;
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis1() throws Exception {
    return latitudeHeading;
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis2() throws Exception {
    return latitudeHeading;
  }

  @Override
  public List<PlotPageTableRecord> generateTableDataRecords(int start,
    int length) {

    List<PlotPageTableRecord> records = new ArrayList<PlotPageTableRecord>(
      length);

    try {

      List<LocalDateTime> times = sensorValues.getPositionTimes();

      // Make sure we don't fall off the end of the dataset
      int lastRecord = start + length;
      if (lastRecord > times.size()) {
        lastRecord = times.size();
      }

      for (int i = start; i < lastRecord; i++) {
        PlotPageTableRecord record = new PlotPageTableRecord(times.get(i));

        // Timestamp
        record.addColumn(times.get(i));

        PlotPageTableValue longitude = sensorValues
          .getRawPositionTableValue(SensorType.LONGITUDE_ID, times.get(i));
        PlotPageTableValue latitude = sensorValues
          .getRawPositionTableValue(SensorType.LATITUDE_ID, times.get(i));

        // The lon/lat can be null if the instrument has a fixed position
        if (null != longitude && null != latitude
          && null != longitude.getValue() && null != latitude.getValue()) {

          StringBuilder positionString = new StringBuilder();
          if (null != longitude.getValue() && null != latitude.getValue()) {
            positionString
              .append(StringUtils.formatNumber(longitude.getValue()));
            positionString.append(" | ");
            positionString
              .append(StringUtils.formatNumber(latitude.getValue()));
          }

          record.addColumn(positionString.toString(), longitude.getQcFlag(),
            longitude.getQcMessage(sensorValues, false),
            longitude.getFlagNeeded(), longitude.getType());
        } else {
          // Empty position column
          record.addColumn("", Flag.GOOD, null, false,
            PlotPageTableValue.NAN_TYPE);
        }

        records.add(record);
      }
    } catch (Exception e) {
      error("Error loading table data", e);
    }

    return records;
  }

  @Override
  protected List<LocalDateTime> getDataTimes() {
    return sensorValues.getPositionTimes();
  }

  @Override
  protected TreeMap<LocalDateTime, PlotPageTableValue> getPositionValues(
    long columnId) throws PlotPageDataException, PositionException {
    return getPositionValuesAction(columnId, false);
  }

  @Override
  public Map<Long, Integer> getNeedsFlagCounts() {
    return null == sensorValues ? null
      : sensorValues.getPositionNeedsFlagCounts();
  }

  @Override
  protected PlotPageColumnHeading getDefaultMap2Column() throws Exception {
    return longitudeHeading;
  }
}
