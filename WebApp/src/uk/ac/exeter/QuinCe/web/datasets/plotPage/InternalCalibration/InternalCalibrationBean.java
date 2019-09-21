package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileColumn;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSet;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSets;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageBean;

/**
 * Bean for handling review of calibration data
 *
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class InternalCalibrationBean extends PlotPageBean {

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "internal_calibration_plot";

  /**
   * Navigation to the dataset list
   */
  private static final String NAV_DATASET_LIST = "dataset_list";

  /**
   * Indicates whether or not the selected calibrations should be used
   */
  private boolean useCalibrations = true;

  /**
   * The message attached to calibrations that should not be used
   */
  private String useCalibrationsMessage = null;

  /**
   * The internal calibration references for the dataset
   */
  private String calibrationJson;

  /**
   * Initialise the required data for the bean
   */
  @Override
  public void init() {
    setFieldSet(DataSetDataDB.SENSORS_FIELDSET);
  }

  /**
   * Finish the calibration data validation
   *
   * @return Navigation to the data set list
   */
  public String finish() {
    if (dirty) {
      try {
        DataSetDB.setDatasetStatus(getDataSource(), datasetId,
          DataSet.STATUS_AUTO_QC);
        Map<String, String> jobParams = new HashMap<String, String>();
        jobParams.put(DataReductionJob.ID_PARAM, String.valueOf(datasetId));
        JobManager.addJob(getDataSource(), getUser(),
          AutoQCJob.class.getCanonicalName(), jobParams);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return NAV_DATASET_LIST;
  }

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  protected String buildPlotLabels(int plotIndex) {
    return null;
  }

  /**
   * Get the flag indicating whether the selected calibrations are to be used
   *
   * @return The use calibrations flag
   */
  public boolean getUseCalibrations() {
    return useCalibrations;
  }

  /**
   * Set the flag indicating whether the selected calibrations are to be used
   *
   * @param useCalibrations
   *          The use calibrations flag
   */
  public void setUseCalibrations(boolean useCalibrations) {
    this.useCalibrations = useCalibrations;
  }

  /**
   * Get the message that will be attached to calibrations which aren't being
   * used
   *
   * @return The message for unused calibrations
   */
  public String getUseCalibrationsMessage() {
    return useCalibrationsMessage;
  }

  /**
   * Set the message that will be attached to calibrations which aren't being
   * used
   *
   * @param useCalibrationsMessage
   *          The message for unused calibrations
   */
  public void setUseCalibrationsMessage(String useCalibrationsMessage) {
    this.useCalibrationsMessage = useCalibrationsMessage;
  }

  /**
   * Set the usage status of the selected rows
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public void setCalibrationUse()
    throws MissingParamException, DatabaseException {

    try {
      Flag newFlag = Flag.GOOD;
      if (!useCalibrations) {
        newFlag = Flag.BAD;
      }

      List<FieldValue> updatedValues = pageData.setQC(getSelectedRowsList(),
        selectedColumn, newFlag, useCalibrationsMessage);
      DataSetDataDB.setQC(getDataSource(), updatedValues);
      dirty = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected Field getDefaultPlot1YAxis() {
    return fieldSets.getField(1);
  }

  @Override
  protected Field getDefaultPlot2YAxis() {
    // TODO DO
    return null;
  }

  @Override
  public boolean getHasTwoPlots() {
    return false;
  }

  @Override
  protected void initData() throws Exception {

    try {
      fieldSets = new FieldSets("Date/Time");

      // Sensor columns
      List<FileColumn> calibratedColumns = InstrumentDB
        .getCalibratedSensorColumns(getDataSource(), getCurrentInstrumentId());

      List<String> calibrationRunTypes = getCurrentInstrument()
        .getRunTypes(RunTypeCategory.INTERNAL_CALIBRATION_TYPE);

      for (FileColumn column : calibratedColumns) {

        // We want the first field set to be the default
        FieldSet columnFieldSet = fieldSets.addFieldSet(column.getColumnId(),
          column.getColumnName(), fieldSets.size() == 1);

        // Add one field for each run type
        for (String runType : calibrationRunTypes) {
          fieldSets.addField(new RunTypeField(columnFieldSet, runType, column));
        }
      }

      pageData = new InternalCalibrationPageData(getCurrentInstrument(),
        fieldSets, dataset);

      pageData.addTimes(DataSetDataDB.getMeasurementTimes(getDataSource(),
        datasetId, calibrationRunTypes));

      // Load internal calibration data
      loadCalibrationData(calibratedColumns);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Load the external standard data into a JSON object. This is used to draw
   * target lines on the plots
   *
   * @param calibratedColumns
   * @throws Exception
   */
  private void loadCalibrationData(List<FileColumn> calibratedColumns)
    throws Exception {

    CalibrationSet calibrationSet = ExternalStandardDB.getInstance()
      .getMostRecentCalibrations(getDataSource(), getCurrentInstrumentId(),
        dataset.getStart());

    JSONObject json = new JSONObject();

    for (Calibration calibration : calibrationSet) {
      JSONObject variableCalibrations = new JSONObject();

      for (FileColumn column : calibratedColumns) {
        String sensorType = column.getSensorType().getName();

        variableCalibrations.put(column.getColumnName(),
          calibration.getCoefficient(sensorType));
      }

      json.put(calibration.getTarget(), variableCalibrations);
    }

    calibrationJson = json.toString();
  }

  public String getCalibrationJson() {
    return calibrationJson;
  }

  @Override
  public List<Integer> getSelectableColumns() {

    List<Integer> result = new ArrayList<Integer>();

    // Everything outside the base field set (i.e. Date/Time) is selectable
    LinkedHashMap<Long, List<Integer>> columnIndexes = fieldSets
      .getColumnIndexes();
    for (long fieldSet : columnIndexes.keySet()) {
      if (fieldSet != FieldSet.BASE_ID) {
        result.addAll(columnIndexes.get(fieldSet));
      }
    }

    return result;
  }

  @Override
  public LinkedHashMap<String, Long> getFieldSets(boolean includeTimePos)
    throws Exception {

    LinkedHashMap<String, Long> result = new LinkedHashMap<String, Long>();

    for (FieldSet fieldSet : fieldSets.keySet()) {
      if (includeTimePos || fieldSet.getId() != FieldSet.BASE_ID) {
        result.put(fieldSet.getName(), fieldSet.getId());
      }
    }

    return result;
  }
}
