package uk.ac.exeter.QuinCe.web.datasets.SensorOffsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.SensorOffset;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

@ManagedBean
@SessionScoped
public class SensorOffsetsBean extends BaseManagedBean {

  private static final String NAV_DATASET_LIST = "dataset_list";

  private static final String NAV_OFFSETS = "sensor_offsets";

  /**
   * The ID of the data set being processed
   */
  private long datasetId;

  /**
   * The data set being processed
   */
  private DataSet dataset;

  /**
   * The instrument for the dataset
   */
  private Instrument instrument;

  /**
   * The sensor values for the columns used to link sensor groups.
   */
  private Map<String, List<SensorValue>> sensorValues;

  /**
   * The ID of the group whose offsets are being edited.
   *
   * <p>
   * This is the second of the two groups being linked together.
   * </p>
   */
  private int currentPair;

  private long offsetFirst;

  private long offsetSecond;

  private long deleteTime;

  boolean dirty = false;

  /**
   * Initialise the bean with basic required information.
   *
   * <p>
   * Full data is loaded when the page is loaded using {@link #loadData()}.
   * </p>
   *
   * @return The navigation to the main bean page.
   */
  public String start() {
    try {
      reset();

      // Load preliminary data
      dataset = DataSetDB.getDataSet(getDataSource(), datasetId);

      // Get the instrument
      instrument = InstrumentDB.getInstrument(getDataSource(),
        dataset.getInstrumentId());

      currentPair = instrument.getSensorGroups().getGroupPairs().get(0).getId();
    } catch (Exception e) {
      return internalError(e);
    }

    return NAV_OFFSETS;
  }

  /**
   * Reset all bean data.
   */
  private void reset() {
    currentPair = 0;
    sensorValues = null;
    instrument = null;
    dataset = null;
  }

  public String finish()
    throws MissingParamException, DatabaseException, RecordNotFoundException {
    DataSetDB.updateDataSet(getDataSource(), dataset);
    return NAV_DATASET_LIST;
  }

  /**
   * Get the dataset ID
   *
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Set the dataset ID
   *
   * @param datasetId
   *          The dataset ID
   */
  public void setDatasetId(long datasetId) {
    this.datasetId = datasetId;
  }

  public DataSet getDataset() {
    return dataset;
  }

  public void loadData() throws Exception {
    try {
      // Get the sensor values for the link columns
      Set<SensorAssignment> linkColumns = instrument.getSensorGroups()
        .getLinkColumns();

      sensorValues = new HashMap<String, List<SensorValue>>(linkColumns.size());

      for (SensorAssignment column : linkColumns) {
        sensorValues.put(column.getSensorName(),
          DataSetDataDB.getSensorValuesForColumns(getDataSource(),
            dataset.getId(), Arrays.asList(column.getDatabaseId())));
      }

      preparePageData();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public void preparePageData() {

  }

  public int getCurrentPair() {
    return currentPair;
  }

  public void setCurrentPair(int currentPair) {
    this.currentPair = currentPair;
  }

  public Instrument getInstrument() {
    return instrument;
  }

  public String getTimeSeriesData() {
    String result = null;

    if (null != sensorValues) {
      SensorGroupPair pair = getCurrentPairObject();

      String firstName = pair.first().getNextLinkName();
      String secondName = pair.second().getPreviousLinkName();

      TimeSeriesPlotData plotData = new TimeSeriesPlotData(firstName,
        sensorValues.get(firstName), secondName, sensorValues.get(secondName));

      result = plotData.getCSV();
    }

    return result;
  }

  public String getFirstName() {
    return instrument.getSensorGroups().getGroupPair(currentPair).first()
      .getNextLinkName();
  }

  public String getSecondName() {
    return instrument.getSensorGroups().getGroupPair(currentPair).second()
      .getPrevLinkName();
  }

  public List<SensorOffset> getOffsetsList() {
    return new ArrayList<SensorOffset>(
      dataset.getSensorOffsets().getOffsets(getCurrentPairObject()));
  }

  private SensorGroupPair getCurrentPairObject() {
    return instrument.getSensorGroups().getGroupPair(currentPair);
  }

  public long getOffsetFirst() {
    return offsetFirst;
  }

  public long getOffsetSecond() {
    return offsetSecond;
  }

  public void setOffsetFirst(long offsetFirst) {
    this.offsetFirst = offsetFirst;
  }

  public void setOffsetSecond(long offsetSecond) {
    this.offsetSecond = offsetSecond;
  }

  public void addOffset() {
    dataset.getSensorOffsets().addOffset(getCurrentPairObject(),
      DateTimeUtils.longToDate(offsetSecond), offsetSecond - offsetFirst);
    dirty = true;
  }

  public long getDeleteTime() {
    return deleteTime;
  }

  public void setDeleteTime(long deleteTime) {
    this.deleteTime = deleteTime;
  }

  public void deleteOffset() {
    dataset.getSensorOffsets().deleteOffset(getCurrentPairObject(),
      DateTimeUtils.longToDate(deleteTime));
    dirty = true;
  }
}
