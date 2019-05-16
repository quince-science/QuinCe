package uk.ac.exeter.QuinCe.web.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecordFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CommentSet;
import uk.ac.exeter.QuinCe.data.Calculation.CommentSetEntry;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DiagnosticDataDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.PlotPageBean;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableList;

/**
 * User QC bean
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ManualQcBean extends PlotPageBean {

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "user_qc";

  /**
   * Navigation to the dataset list
   */
  private static final String NAV_DATASET_LIST = "dataset_list";

  /**
   * The number of sensor columns
   */
  private int sensorColumnCount = 0;

  /**
   * The number of calculation columns
   */
  private int calculationColumnCount = 0;

  /**
   * The number of diagnostic columns
   */
  private int diagnosticColumnCount = 0;

  /**
   * The column containing the auto QC flag
   */
  private int autoFlagColumn = 0;

  /**
   * The column containing the manual QC flag
   */
  private int userFlagColumn = 0;

  /**
   * The set of comments for the user QC dialog. Stored as a Javascript array of entries, with each entry containing
   * Comment, Count and Flag value
   */
  private String userCommentList = "[]";

  /**
   * The worst flag set on the selected rows
   */
  private Flag worstSelectedFlag = Flag.GOOD;

  /**
   * The user QC flag
   */
  private int userFlag;

  /**
   * The user QC comment
   */
  private String userComment;

  /**
   * Initialise the required data for the bean
   */
  @Override
  public void init() {
    setFieldSet(DataSetDataDB.SENSORS_FIELDSET);
  }

  @Override
  protected List<Long> loadRowIds() throws Exception {
    //return DataSetDataDB.getMeasurementIds(getDataSource(), getDatasetId());
    return new ArrayList<Long>();
  }

  @Override
  protected String buildTableHeadings() throws Exception {
    List<String> dataHeadings = DataSetDataDB.getDatasetDataColumnNames(getDataSource(), getDataset());
    sensorColumnCount = dataHeadings.size() - 4; // Skip id, date, lat, lon
    List<String> calculationHeadings = CalculationDBFactory.getCalculationDB().getCalculationColumnHeadings();
    calculationColumnCount = calculationHeadings.size();
    List<String> diagnosticHeadings = DiagnosticDataDB.getDiagnosticSensorNames(getDataSource(), getDataset().getInstrumentId());
    diagnosticColumnCount = diagnosticHeadings.size();

    JSONArray headings = new JSONArray();

    for (String heading : dataHeadings) {
      headings.put(heading);
    }

    for (int i = 0; i < calculationHeadings.size(); i++) {
      headings.put(calculationHeadings.get(i));
    }

    for (String heading : diagnosticHeadings) {
      headings.put(heading);
    }

    headings.put("Automatic QC");
    headings.put("Automatic QC Message");

    // Columns are zero-based, so we don't need to add one to get to the auto flag column
    autoFlagColumn = dataHeadings.size() + calculationHeadings.size() + diagnosticHeadings.size();

    if (!getDataset().isNrt()) {
      headings.put("Manual QC");
      headings.put("Manual QC Message");
      userFlagColumn = autoFlagColumn + 2;
    } else {
      userFlagColumn = -1;
    }


    return headings.toString();
  }

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  protected String buildSelectableRows() throws Exception {
    List<Long> ids = CalculationDBFactory.getCalculationDB().getSelectableMeasurementIds(getDataSource(), getDatasetId());

    StringBuilder result = new StringBuilder();

    result.append('[');

    for (int i = 0; i < ids.size(); i++) {
      result.append(ids.get(i));
      if (i < ids.size() - 1) {
        result.append(',');
      }
    }

    result.append(']');

    return result.toString();
  }

  @Override
  protected String buildPlotLabels(int plotIndex) {
    return null;
  }

  @Override
  protected String loadTableData(int start, int length) throws Exception {
    List<DataSetRawDataRecord> datasetData = DataSetDataDB.getMeasurements(getDataSource(), getDataset(), start, length);

    List<Long> measurementIds = new ArrayList<Long>(datasetData.size());
    List<CalculationRecord> calculationData = new ArrayList<CalculationRecord>(datasetData.size());
    for (DataSetRawDataRecord record : datasetData) {
      measurementIds.add(record.getId());
      CalculationRecord calcRecord = CalculationRecordFactory.makeCalculationRecord(getDatasetId(), record.getId());
      CalculationDBFactory.getCalculationDB().loadCalculationValues(getDataSource(), calcRecord);
      calculationData.add(calcRecord);
    }

    List<String> diagnosticHeadings = DiagnosticDataDB.getDiagnosticSensorNames(getDataSource(), getDataset().getInstrumentId());
    Map<Long, Map<String, Double>> diagnosticData = DiagnosticDataDB.getDiagnosticValues(getDataSource(), getDataset().getInstrumentId(), measurementIds, diagnosticHeadings);

    JSONArray json = new JSONArray();

    int rowId = start - 1;
    for (int i = 0; i < datasetData.size(); i++) {
      rowId++;

      DataSetRawDataRecord dsData = datasetData.get(i);
      CalculationRecord calcData = calculationData.get(i);

      JSONObject obj = new JSONObject();

      obj.put("DT_RowId", "row" + rowId);

      int columnIndex = 0;
      obj.put(String.valueOf(columnIndex), dsData.getId()); // ID

      columnIndex++;
      obj.put(String.valueOf(columnIndex), DateTimeUtils.dateToLong(dsData.getDate())); // Date

      columnIndex++;
      obj.put(String.valueOf(columnIndex), dsData.getLongitude()); // Longitude

      columnIndex++;
      obj.put(String.valueOf(columnIndex), dsData.getLatitude()); // Latitude

      // Sensor values
      for (Map.Entry<String, Double> entry : dsData.getSensorValues().entrySet()) {
        columnIndex++;
        Double value = entry.getValue();
        if (null == value) {
          obj.put(String.valueOf(columnIndex), JSONObject.NULL);
        } else {
          obj.put(String.valueOf(columnIndex), value);
        }
      }

      // Calculation values
      List<String> calcColumns = calcData.getCalculationColumns();

      for (int j = 0; j < calcColumns.size(); j++) {
        columnIndex++;
        Double value = calcData.getNumericValue(calcColumns.get(j));
        if (null == value) {
          obj.put(String.valueOf(columnIndex), JSONObject.NULL);
        } else {
          obj.put(String.valueOf(columnIndex), value);
        }
      }

      // Diagnostic values
      Map<String, Double> diagnosticValues = diagnosticData.get(dsData.getId());
      for (String column : diagnosticHeadings) {
        columnIndex++;
        if (null == diagnosticValues.get(column)) {
          obj.put(String.valueOf(columnIndex), JSONObject.NULL);
        } else {
          obj.put(String.valueOf(columnIndex), diagnosticValues.get(column));
        }
      }

      columnIndex++;
      obj.put(String.valueOf(columnIndex), calcData.getAutoFlag().getFlagValue());

      columnIndex++;
      if (null == calcData.getAutoQCMessagesString()) {
        obj.put(String.valueOf(columnIndex), JSONObject.NULL);
      } else {
        obj.put(String.valueOf(columnIndex), calcData.getAutoQCMessagesString());
      }

      if (!getDataset().isNrt()) {
        columnIndex++;
        obj.put(String.valueOf(columnIndex), calcData.getUserFlag().getFlagValue());

        columnIndex++;
        if (null == calcData.getUserMessage()) {
          obj.put(String.valueOf(columnIndex), JSONObject.NULL);
        } else {
          obj.put(String.valueOf(columnIndex), calcData.getUserMessage());
        }
      }

      json.put(obj);
    }

    return json.toString();
  }

  /**
   * Finish the calibration data validation
   * @return Navigation to the data set list
   */
  public String finish() {
    if (dirty) {
      try {
        DataSetDB.setDatasetStatus(getDataSource(), getDatasetId(), DataSet.STATUS_DATA_REDUCTION);
        Map<String, String> jobParams = new HashMap<String, String>();
        jobParams.put(DataReductionJob.ID_PARAM, String.valueOf(getDatasetId()));
        JobManager.addJob(getDataSource(), getUser(), DataReductionJob.class.getCanonicalName(), jobParams);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return NAV_DATASET_LIST;
  }

  @Override
  public String getAdditionalTableData() {
    JSONObject json = new JSONObject();

    json.put("sensorColumnCount", sensorColumnCount);
    json.put("calculationColumnCount", calculationColumnCount);
    json.put("diagnosticColumnCount", diagnosticColumnCount);

    JSONArray flagColumns = new JSONArray();
    flagColumns.put(autoFlagColumn);
    flagColumns.put(userFlagColumn);

    json.put("flagColumns", flagColumns);

    return json.toString();
  }

  /**
   * Apply the automatically generated QC flags to the rows selected in the table
   */
  public void acceptAutoQc() {
    try {
      CalculationDBFactory.getCalculationDB().acceptAutoQc(getDataSource(), getSelectedRowsList());
      dirty = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the set of comments for the WOCE dialog
   * @return The comments for the WOCE dialog
   */
  public String getUserCommentList() {
    return userCommentList;
  }

  /**
   * Dummy: Sets the set of comments for the WOCE dialog
   * @param userCommentList The comments; ignored
   */
  public void setUserCommentList(String userCommentList) {
    // Do nothing
  }

  /**
   * Get the worst flag set on the selected rows
   * @return The worst flag on the selected rows
   */
  public int getWorstSelectedFlag() {
    return worstSelectedFlag.getFlagValue();
  }

  /**
   * Dummy: Set the worst selected flag
   * @param worstSelectedFlag The flag; ignored
   */
  public void setWorstSelectedFlag(int worstSelectedFlag) {
    // Do nothing
  }

  /**
   * Generate the list of comments for the WOCE dialog
   */
  public void generateUserCommentList() {

    worstSelectedFlag = Flag.GOOD;

    JSONArray json = new JSONArray();

    try {
      CommentSet comments = CalculationDBFactory.getCalculationDB().getCommentsForRows(getDataSource(), getSelectedRowsList());
      for (CommentSetEntry entry : comments) {
        json.put(entry.toJson());
        if (entry.getFlag().moreSignificantThan(worstSelectedFlag)) {
          worstSelectedFlag = entry.getFlag();
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      JSONArray jsonEntry = new JSONArray();
      jsonEntry.put("Existing comments could not be retrieved");
      jsonEntry.put(4);
      jsonEntry.put(1);
      json.put(jsonEntry);
      worstSelectedFlag = Flag.BAD;
    }

    userCommentList = json.toString();
  }

  /**
   * @return the userComment
   */
  public String getUserComment() {
    return userComment;
  }

  /**
   * @param userComment the userComment to set
   */
  public void setUserComment(String userComment) {
    this.userComment = userComment;
  }

  /**
   * @return the userFlag
   */
  public int getUserFlag() {
    return userFlag;
  }

  /**
   * @param userFlag the userFlag to set
   */
  public void setUserFlag(int userFlag) {
    this.userFlag = userFlag;
  }

  /**
   * Apply the entered WOCE flag and comment to the rows selected in the table
   */
  public void applyManualFlag() {
    try {
      CalculationDBFactory.getCalculationDB().applyManualFlag(getDataSource(), getSelectedRowsList(), userFlag, userComment);
      dirty = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected Variable getDefaultPlot1XAxis() {
    return variables.getVariableWithLabel("Date/Time");
  }

  @Override
  protected List<Variable> getDefaultPlot1YAxis() {
    List<Variable> result = new ArrayList<Variable>(1);
    result.add(variables.getVariableWithLabel("Intake Temperature"));
    return result;
  }

  @Override
  protected Variable getDefaultPlot2XAxis() {
    return variables.getVariableWithLabel("Date/Time");
  }

  @Override
  protected List<Variable> getDefaultPlot2YAxis() {
    List<Variable> result = new ArrayList<Variable>(1);
    result.add(variables.getVariableWithLabel("Final fCO2"));
    return result;
  }

  @Override
  protected Variable getDefaultMap1Variable() {
    return variables.getVariableWithLabel("Intake Temperature");
  }

  @Override
  protected Variable getDefaultMap2Variable() {
    return variables.getVariableWithLabel("Final fCO2");
  }

  @Override
  protected void buildVariableList(VariableList variables) throws Exception {
    DataSetDataDB.populateVariableList(getDataSource(), getDataset(), variables);
    CalculationDBFactory.getCalculationDB().populateVariableList(variables);
    DiagnosticDataDB.populateVariableList(getDataSource(), getDataset(), variables);
  }

  @Override
  protected String getData(List<String> fields) throws Exception {
    return CalculationDBFactory.getCalculationDB().getJsonData(getDataSource(), getDataset(), fields, fields.get(0));
  }

  @Override
  public boolean getHasTwoPlots() {
    return true;
  }
}
