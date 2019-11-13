package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Instrument.FileColumn;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.datasets.data.CommentSet;
import uk.ac.exeter.QuinCe.web.datasets.data.CommentSetEntry;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSet;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSets;
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
   * The number of flags that need to be checked by the user
   */
  private int flagsRequired = 0;

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

    List<FieldValue> updatedValues = null;

    if (positionColumnSelected()) {
      updatedValues = acceptPositionAutoQC();
    } else {
      updatedValues = getSelectedRowsList().stream()
        .map(t -> pageData.getValue(t, selectedColumn))
        .collect(Collectors.toList());

      // TODO If the Positional QC is worse than this QC, use that instead.
    }

    saveUpdates(updatedValues);
  }

  private List<FieldValue> acceptPositionAutoQC() {

    List<LocalDateTime> times = getSelectedRowsList();

    List<FieldValue> updates = new ArrayList<FieldValue>();

    int otherPositionColumn = (selectedColumn == fieldSets
      .getColumnIndex(FileDefinition.LONGITUDE_COLUMN_ID))
        ? fieldSets.getColumnIndex(FileDefinition.LATITUDE_COLUMN_ID)
        : fieldSets.getColumnIndex(FileDefinition.LONGITUDE_COLUMN_ID);

    for (LocalDateTime time : times) {

      // Both position values are always flagged
      FieldValue chosenPositionValue = pageData.getValue(time, selectedColumn);
      FieldValue otherPositionValue = pageData.getValue(time,
        otherPositionColumn);

      // Record which is the most significant position QC - this will be applied
      // to the sensors
      Flag appliedFlag = chosenPositionValue.getQcFlag();
      String appliedComment = chosenPositionValue.getQcComment();

      // Whichever position has the worst flag, that's the one we use. If it's a
      // tie, use each value's own QC.
      if (otherPositionValue.getQcFlag()
        .moreSignificantThan(chosenPositionValue.getQcFlag())) {

        chosenPositionValue.setQcFlag(otherPositionValue.getQcFlag());
        chosenPositionValue.setQcComment(otherPositionValue.getQcComment());
        appliedFlag = otherPositionValue.getQcFlag();
        appliedComment = otherPositionValue.getQcComment();
      } else if (chosenPositionValue.getQcFlag()
        .moreSignificantThan(otherPositionValue.getQcFlag())) {

        otherPositionValue.setQcFlag(chosenPositionValue.getQcFlag());
        otherPositionValue.setQcComment(chosenPositionValue.getQcComment());
      }

      chosenPositionValue.setNeedsFlag(false);
      otherPositionValue.setNeedsFlag(false);

      updates.add(chosenPositionValue);
      updates.add(otherPositionValue);

      updates.addAll(setSensorsQc(time, appliedFlag, appliedComment));

      // TODO Also check any values between this and next time with position. Do
      // all those values too.

    }

    return updates;

  }

  private List<FieldValue> setSensorsQc(LocalDateTime time, Flag flag,
    String comment) {

    List<FieldValue> updatedValues = new ArrayList<FieldValue>();

    List<Field> sensorFields = fieldSets
      .get(fieldSets.getFieldSet(DataSetDataDB.SENSORS_FIELDSET));

    for (Field field : sensorFields) {
      FieldValue value = pageData.getValue(time,
        fieldSets.getColumnIndex(field.getId()));

      // We don't change the QC if the automatic QC has yet to be verified.
      // The user's choice may override the position flag
      if (!value.needsFlag() && flag.moreSignificantThan(value.getQcFlag())) {
        value.setQcFlag(flag);
        value.setQcComment("Position: " + comment);
        updatedValues.add(value);
      }
    }

    return updatedValues;
  }

  private void saveUpdates(List<FieldValue> updates) {

    if (null != updates && updates.size() > 0) {
      try {
        DataSetDataDB.setQC(getDataSource(), updates);

        for (FieldValue value : updates) {
          value.setNeedsFlag(false);
        }

        updateFlagsRequired();
        dirty = true;
      } catch (Exception e) {
        e.printStackTrace();
      }
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

    List<FieldValue> updates = null;

    try {
      if (positionColumnSelected()) {
        updates = applyManualPositionFlag();
      } else {
        updates = pageData.setQC(getSelectedRowsList(), selectedColumn,
          userFlag, userComment);

        // TODO If the Positional QC is worse than this QC, use that instead.
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    saveUpdates(updates);
  }

  private List<FieldValue> applyManualPositionFlag()
    throws InvalidFlagException {

    List<FieldValue> updates = new ArrayList<FieldValue>();

    List<LocalDateTime> times = getSelectedRowsList();

    Field selectedField = fieldSets.getField(selectedColumn);
    Field otherPositionField;

    if (selectedColumn == fieldSets
      .getColumnIndex(FileDefinition.LONGITUDE_COLUMN_ID)) {

      otherPositionField = fieldSets
        .getField(fieldSets.getColumnIndex(FileDefinition.LATITUDE_COLUMN_ID));
    } else {
      otherPositionField = fieldSets
        .getField(fieldSets.getColumnIndex(FileDefinition.LONGITUDE_COLUMN_ID));
    }

    for (LocalDateTime time : times) {

      updates.add(pageData.setQC(time, selectedField, userFlag, userComment));
      updates
        .add(pageData.setQC(time, otherPositionField, userFlag, userComment));

      // Set all sensor QCs here.
    }

    return updates;
  }

  @Override
  protected Field getDefaultPlot1YAxis() {
    Field result = null;

    try {
      List<FileColumn> fileColumns = InstrumentDB
        .getSensorColumns(getDataSource(), getCurrentInstrumentId());

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

  @Override
  protected void initData() throws Exception {
    try {
      fieldSets = new FieldSets("Date/Time");

      fieldSets.addField(new Field(FieldSet.BASE_FIELD_SET,
        FileDefinition.LONGITUDE_COLUMN_ID, "Longitude"));
      fieldSets.addField(new Field(FieldSet.BASE_FIELD_SET,
        FileDefinition.LATITUDE_COLUMN_ID, "Latitude"));

      // Sensor columns
      List<FileColumn> fileColumns = InstrumentDB
        .getSensorColumns(getDataSource(), getCurrentInstrumentId());

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

        fieldSets.addField(
          new Field(addFieldSet, column.getColumnId(), column.getColumnName()));
      }

      // Data reduction columns
      for (InstrumentVariable variable : getCurrentInstrument()
        .getVariables()) {
        LinkedHashMap<String, Long> variableParameters = DataReducerFactory
          .getCalculationParameters(variable);

        FieldSet varFieldSet = fieldSets.addFieldSet(variable.getId(),
          variable.getName(), false);

        // Columns from data reduction are given IDs based on the
        // variable ID and parameter number
        for (Map.Entry<String, Long> entry : variableParameters.entrySet()) {

          fieldSets
            .addField(new Field(varFieldSet, entry.getValue(), entry.getKey()));
        }
      }

      pageData = new ManualQCPageData(getCurrentInstrument(), fieldSets,
        dataset);

      // Load data for sensor columns
      List<Long> fieldIds = new ArrayList<Long>();
      fieldIds.add(FileDefinition.LONGITUDE_COLUMN_ID);
      fieldIds.add(FileDefinition.LATITUDE_COLUMN_ID);
      fieldIds.addAll(fieldSets.getFieldIds(sensorFieldSet));
      fieldIds.addAll(fieldSets.getFieldIds(diagnosticFieldSet));

      pageData.addTimes(DataSetDataDB.getSensorValueDates(getDataSource(),
        getDataset().getId()));

      if (dataset.isNrt()) {
        flagsRequired = 0;
      } else {
        updateFlagsRequired();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public List<Integer> getSelectableColumns() {
    List<Integer> result = new ArrayList<Integer>();

    // Nothing is selectable for NRT datasets
    if (!dataset.isNrt()) {

      // Position columns
      result.add(fieldSets.getColumnIndex(FileDefinition.LONGITUDE_COLUMN_ID));
      result.add(fieldSets.getColumnIndex(FileDefinition.LATITUDE_COLUMN_ID));

      LinkedHashMap<Long, List<Integer>> columnIndexes = fieldSets
        .getColumnIndexes();
      result.addAll(columnIndexes.get(DataSetDataDB.SENSORS_FIELDSET));
      result.addAll(columnIndexes.get(DataSetDataDB.DIAGNOSTICS_FIELDSET));
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

  /**
   * Get the number of flags that need to be checked by the user
   *
   * @return
   */
  public int getFlagsRequired() {
    return flagsRequired;
  }

  private void updateFlagsRequired()
    throws MissingParamException, DatabaseException {
    flagsRequired = DataSetDataDB.getFlagsRequired(getDataSource(),
      getDataset().getId());
  }

  @Override
  public String getGhostDataLabel() {
    return "Flushing";
  }
}
