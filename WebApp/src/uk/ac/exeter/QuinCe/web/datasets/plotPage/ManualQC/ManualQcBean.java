package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileColumn;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.datasets.data.CommentSet;
import uk.ac.exeter.QuinCe.web.datasets.data.CommentSetEntry;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageBean;

/**
 * User QC bean
 * 
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
   * The set of comments for the user QC dialog. Stored as a Javascript array of
   * entries, with each entry containing Comment, Count and Flag value
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
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  protected String buildPlotLabels(int plotIndex) {
    return null;
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

    // Destroy all bean data
    reset();

    return NAV_DATASET_LIST;
  }

  /**
   * Apply the automatically generated QC flags to the rows selected in the
   * table
   */
  public void acceptAutoQc() {
    try {

      List<LocalDateTime> times = getSelectedRowsList();
      List<FieldValue> updateValues = new ArrayList<FieldValue>(times.size());

      for (LocalDateTime time : times) {
        updateValues.add(pageData.getValue(time, selectedColumn));
      }

      DataSetDataDB.setQC(getDataSource(), updateValues);

      for (FieldValue value : updateValues) {
        value.setNeedsFlag(false);
      }

      dirty = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the set of comments for the WOCE dialog
   * 
   * @return The comments for the WOCE dialog
   */
  public String getUserCommentList() {
    return userCommentList;
  }

  /**
   * Dummy: Sets the set of comments for the WOCE dialog
   * 
   * @param userCommentList
   *          The comments; ignored
   */
  public void setUserCommentList(String userCommentList) {
    // Do nothing
  }

  /**
   * Get the worst flag set on the selected rows
   * 
   * @return The worst flag on the selected rows
   */
  public int getWorstSelectedFlag() {
    return worstSelectedFlag.getFlagValue();
  }

  /**
   * Dummy: Set the worst selected flag
   * 
   * @param worstSelectedFlag
   *          The flag; ignored
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
      CommentSet comments = pageData.getCommentSet(selectedColumn,
        getSelectedRowsList());
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
   * @param userComment
   *          the userComment to set
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
   * @param userFlag
   *          the userFlag to set
   */
  public void setUserFlag(int userFlag) {
    this.userFlag = userFlag;
  }

  /**
   * Apply the entered WOCE flag and comment to the rows selected in the table
   */
  public void applyManualFlag() {
    try {
      List<FieldValue> updatedValues = pageData.setQC(getSelectedRowsList(),
        selectedColumn, new Flag(userFlag), userComment);
      DataSetDataDB.setQC(getDataSource(), updatedValues);
      dirty = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected Field getDefaultPlot1YAxis() {
    Field result = null;

    try {
      List<FileColumn> fileColumns = InstrumentDB
        .getSensorColumns(getDataSource(), instrument.getDatabaseId());

      for (FileColumn column : fileColumns) {
        if (column.getSensorType().getName().equals("Intake Temperature")) {
          result = fieldSets.getField(column.getColumnId());
        }
      }
    } catch (Exception e) {
      // Do nothing
    }

    return result;
  }

  @Override
  protected Field getDefaultPlot2YAxis() {
    return fieldSets.getField("fCO₂");
  }

  @Override
  public boolean getHasTwoPlots() {
    return true;
  }

  /**
   * Load the data for the current field set
   */
  @Override
  protected void loadData() throws Exception {
/*
    try {
      fieldSets = new FieldSets("Date/Time");

      fieldSets.addField(FieldSet.BASE_FIELD_SET,
        new Field(FileDefinition.LONGITUDE_COLUMN_ID, "Longitude"));
      fieldSets.addField(FieldSet.BASE_FIELD_SET,
        new Field(FileDefinition.LATITUDE_COLUMN_ID, "Latitude"));

      // Sensor columns
      List<FileColumn> fileColumns = InstrumentDB
        .getSensorColumns(getDataSource(), instrument.getDatabaseId());

      FieldSet sensorFieldSet = fieldSets.addFieldSet(
        DataSetDataDB.SENSORS_FIELDSET, DataSetDataDB.SENSORS_FIELDSET_NAME,
        true);

      FieldSet diagnosticFieldSet = fieldSets.addFieldSet(
        DataSetDataDB.DIAGNOSTICS_FIELDSET,
        DataSetDataDB.DIAGNOSTICS_FIELDSET_NAME, false);

      for (FileColumn column : fileColumns) {

        FieldSet addFieldSet = sensorFieldSet;
        if (column.getSensorType().isDiagnostic()) {
          addFieldSet = diagnosticFieldSet;
        }

        fieldSets.addField(addFieldSet,
          new Field(column.getColumnId(), column.getColumnName()));
      }

      // Data reduction columns
      for (InstrumentVariable variable : instrument.getVariables()) {
        LinkedHashMap<String, Long> variableParameters = DataReducerFactory
          .getCalculationParameters(variable);

        FieldSet varFieldSet = fieldSets.addFieldSet(variable.getId(),
          variable.getName(), false);

        // Columns from data reduction are given IDs based on the
        // variable ID and parameter number
        for (Map.Entry<String, Long> entry : variableParameters.entrySet()) {

          fieldSets.addField(varFieldSet,
            new Field(entry.getValue(), entry.getKey()));
        }
      }

      pageData = new ManualQCPageData(instrument, fieldSets, dataset);

      // Load data for sensor columns
      List<Long> fieldIds = new ArrayList<Long>();
      fieldIds.add(FileDefinition.LONGITUDE_COLUMN_ID);
      fieldIds.add(FileDefinition.LATITUDE_COLUMN_ID);
      fieldIds.addAll(fieldSets.getFieldIds(sensorFieldSet));
      fieldIds.addAll(fieldSets.getFieldIds(diagnosticFieldSet));

      DataSetDataDB.getQCSensorData(getDataSource(), pageData,
        getDataset().getId(), instrument, fieldIds);

      // Load data reduction data
      DataSetDataDB.getDataReductionData(getDataSource(), pageData, dataset);
    } catch (Exception e) {
      e.printStackTrace();
    }
*/
  }

  @Override
  public List<Integer> getSelectableColumns() {
    List<Integer> result = new ArrayList<Integer>();

    // Nothing is selectable for NRT datasets
    if (!dataset.isNrt()) {
      // Sensor values. This is the field set with ID -1.
      // We'll add diagnostics sometime.
      LinkedHashMap<Long, List<Integer>> columnIndexes = fieldSets
        .getColumnIndexes();
      result.addAll(columnIndexes.get(DataSetDataDB.SENSORS_FIELDSET));
    }

    return result;
  }

  /**
   * Get the available field sets for this dataset keyed by name. Builds the
   * list once, then caches it
   * 
   * @return The field sets
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws VariableNotFoundException
   *           If an invalid variable is configured for the instrument
   */
  @Override
  public LinkedHashMap<String, Long> getFieldSets(boolean includeTimePos)
    throws MissingParamException, VariableNotFoundException, DatabaseException {

    return dataset.getFieldSets(includeTimePos);
  }

}
