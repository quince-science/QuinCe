package uk.ac.exeter.QuinCe.web.datasets.SensorOffsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorOffset;
import uk.ac.exeter.QuinCe.data.Dataset.SensorOffsetsException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.DataSetJob;
import uk.ac.exeter.QuinCe.jobs.files.LocateMeasurementsJob;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
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
  private DatasetSensorValues sensorValues;

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

  SensorOffsetsPlotData plotData;

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
      setCurrentInstrumentId(instrument.getId());

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
    dirty = false;
    plotData = null;
  }

  public String finish() throws Exception {

    if (dirty) {
      dataset.setStatus(DataSet.STATUS_DATA_REDUCTION);
      DataSetDB.updateDataSet(getDataSource(), dataset);

      Properties jobProperties = new Properties();
      jobProperties.setProperty(DataSetJob.ID_PARAM, String.valueOf(datasetId));
      JobManager.addJob(getDataSource(), getUser(),
        LocateMeasurementsJob.class.getCanonicalName(), jobProperties);

    }
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
      sensorValues = DataSetDataDB
        .getSensorValues(getDataSource().getConnection(), dataset, true, true);
      preparePlotData();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public int getCurrentPair() {
    return currentPair;
  }

  public void setCurrentPair(int currentPair)
    throws SensorGroupsException, RecordNotFoundException {
    this.currentPair = currentPair;
    preparePlotData();
  }

  public Instrument getInstrument() {
    return instrument;
  }

  public String getTimeSeriesData()
    throws SensorGroupsException, SensorOffsetsException {
    String result = null;

    if (null != plotData) {
      result = plotData.getArray(dataset.getSensorOffsets(),
        getCurrentPairObject());
    }

    return result;
  }

  private void preparePlotData()
    throws SensorGroupsException, RecordNotFoundException {
    if (null != sensorValues) {
      SensorGroupPair pair = getCurrentPairObject();

      plotData = new SensorOffsetsPlotData(
        sensorValues.getColumnValues(pair.first().getNextLink()),
        sensorValues.getColumnValues(pair.second().getPrevLink()),
        sensorValues);
    }
  }

  public void preparePageData()
    throws SensorGroupsException, RecordNotFoundException {
    preparePlotData();
  }

  protected SensorOffsetsPlotData getPlotData() {
    return plotData;
  }

  public String getFirstName() throws SensorGroupsException {
    try {
      return instrument.getSensorGroups().getGroupPair(currentPair).first()
        .getNextLinkName();
    } catch (SensorGroupsException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public String getSecondName() throws SensorGroupsException {
    try {
      return instrument.getSensorGroups().getGroupPair(currentPair).second()
        .getPrevLinkName();
    } catch (SensorGroupsException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public List<SensorOffset> getOffsetsList() throws SensorGroupsException {
    return new ArrayList<SensorOffset>(
      dataset.getSensorOffsets().getOffsets(getCurrentPairObject()));
  }

  public String getOffsetsListJson() throws SensorGroupsException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapter(SensorOffset.class, new SensorOffsetSerializer(this))
      .create();

    return gson.toJson(getOffsetsList());
  }

  private SensorGroupPair getCurrentPairObject() throws SensorGroupsException {
    try {
      return instrument.getSensorGroups().getGroupPair(currentPair);
    } catch (SensorGroupsException e) {
      e.printStackTrace();
      throw e;
    }
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

  public void addOffset() throws SensorGroupsException, SensorOffsetsException {
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

  public void deleteOffset() throws SensorGroupsException {
    dataset.getSensorOffsets().deleteOffset(getCurrentPairObject(),
      DateTimeUtils.longToDate(deleteTime));
    dirty = true;
  }
}
