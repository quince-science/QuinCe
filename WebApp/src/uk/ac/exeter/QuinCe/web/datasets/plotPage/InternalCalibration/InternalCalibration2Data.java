package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypeSensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Data;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableColumn;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageDataStructure;

public class InternalCalibration2Data extends PlotPage2Data {

  /**
   * The dataset whose data is represented.
   */
  private final DataSet dataset;

  /**
   * The instrument that the dataset belongs to.
   */
  private final Instrument instrument;

  private ColumnHeading defaultYAxis1 = null;

  private ColumnHeading defaultYAxis2 = null;

  private SimplePlotPageDataStructure dataStructure = null;

  /**
   * Construct the data object.
   *
   * <p>
   * Initially the object is empty. The data will be loaded by the
   * {@link #load(DataSource)} method.
   * </p>
   *
   * @param instrument
   *          The instrument that the dataset belongs to.
   * @param dataset
   *          The dataset.
   * @param dataSource
   *          A data source.
   * @throws SQLException
   * @throws Exception
   *           If the data cannot be loaded.
   */
  protected InternalCalibration2Data(DataSource dataSource,
    Instrument instrument, DataSet dataset) throws SQLException {
    this.instrument = instrument;
    this.dataset = dataset;
  }

  @Override
  protected void loadDataAction() throws Exception {

    List<RunTypeSensorValue> sensorValues = DataSetDataDB
      .getInternalCalibrationSensorValues(conn, instrument, dataset.getId());

    dataStructure = new SimplePlotPageDataStructure(getColumnHeadingsList());

    for (RunTypeSensorValue value : sensorValues) {
      long columnId = makeColumnId(value.getRunType(), value.getColumnId());
      dataStructure.add(value.getTime(), getColumnHeading(columnId), value,
        false);
    }
  }

  @Override
  protected void buildColumnHeadings()
    throws MissingParamException, DatabaseException, RecordNotFoundException {

    columnHeadings = new LinkedHashMap<String, List<ColumnHeading>>();

    CalibrationSet calibrations = ExternalStandardDB.getInstance()
      .getStandardsSet(conn, instrument.getDatabaseId(), dataset.getStart());

    // Time
    List<ColumnHeading> rootColumns = new ArrayList<ColumnHeading>(1);
    rootColumns.add(new ColumnHeading(FileDefinition.TIME_COLUMN_ID,
      FileDefinition.TIME_COLUMN_NAME, false, false));

    columnHeadings.put(ROOT_FIELD_GROUP, rootColumns);

    List<String> runTypes = instrument
      .getRunTypes(RunTypeCategory.INTERNAL_CALIBRATION_TYPE);

    int columnCount = 0;

    // Each SensorType with internal calibrations goes in its own group
    for (SensorType sensorType : instrument.getSensorAssignments().keySet()) {
      if (sensorType.hasInternalCalibration()) {

        List<ColumnHeading> sensorTypeColumns = new ArrayList<ColumnHeading>();

        List<SensorAssignment> assignments = instrument.getSensorAssignments()
          .get(sensorType);

        // Each sensor assigned to that SensorType becomes a set of columns,
        // one for each calibration run type
        for (SensorAssignment assignment : assignments) {
          for (String runType : runTypes) {

            Double calibrationValue = calibrations.getCalibrationValue(runType,
              sensorType.getName());

            long columnId = makeColumnId(runType, assignment);
            String columnName = runType + ":" + assignment.getSensorName();

            ColumnHeading heading = new ColumnHeading(columnId, columnName,
              true, true, calibrationValue);
            sensorTypeColumns.add(heading);
            columnCount++;
            if (columnCount == 1) {
              defaultYAxis1 = heading;
            } else if (columnCount == 2) {
              defaultYAxis2 = heading;
            }
          }
        }

        columnHeadings.put(sensorType.getName(), sensorTypeColumns);
      }
    }
  }

  /**
   * Generate a unique column ID for a run type/sensor assignment combination.
   *
   * <p>
   * Calculated as the hash code of the run type plus the assignment's database
   * ID.
   * </p>
   *
   * @param runType
   *          The run type
   * @param assignment
   *          The sensor assignment
   * @return The column ID
   */
  private long makeColumnId(String runType, SensorAssignment assignment) {
    return makeColumnId(runType, assignment.getDatabaseId());
  }

  /**
   * Generate a unique column ID for a run type and column ID.
   *
   * <p>
   * Calculated as the hash code of the run type plus the column ID.
   * </p>
   *
   * @param runType
   *          The run type
   * @param assignment
   *          The sensor assignment
   * @return The column ID
   */
  private long makeColumnId(String runType, long columnId) {
    return (runType + columnId).hashCode();
  }

  @Override
  public LinkedHashMap<String, List<ColumnHeading>> getExtendedColumnHeadings() {
    return getColumnHeadings();
  }

  @Override
  public int size() {
    return null == dataStructure ? 0 : dataStructure.size();
  }

  @Override
  protected List<Long> getRowIDs() {
    List<Long> result = null;

    if (null != dataStructure) {
      result = dataStructure.getRowIds();
    }

    return result;
  }

  @Override
  protected List<PlotPageTableRecord> generateTableDataRecords(int start,
    int length) {

    return dataStructure.generateTableDataRecords(start, length);
  }

  @Override
  protected TreeMap<LocalDateTime, PlotPageTableColumn> getColumnValues(
    ColumnHeading column) throws Exception {

    return dataStructure.getColumnValues(column);
  }

  @Override
  protected ColumnHeading getDefaultYAxis1() {
    return defaultYAxis1;
  }

  @Override
  protected ColumnHeading getDefaultYAxis2() {
    return defaultYAxis2;
  }

  private List<SensorValue> getSelectedSensorValues() {
    return dataStructure.getSensorValues(selectedColumn,
      DateTimeUtils.longsToDates(selectedRows));
  }

  protected void applyFlag(Flag flag, String message)
    throws MissingParamException, DatabaseException {

    List<SensorValue> sensorValues = getSelectedSensorValues();

    sensorValues.forEach(v -> v.setUserQC(flag, message));

    // Store the updated sensor values
    DataSetDataDB.storeSensorValues(conn, sensorValues);

    initPlots();
  }
}
