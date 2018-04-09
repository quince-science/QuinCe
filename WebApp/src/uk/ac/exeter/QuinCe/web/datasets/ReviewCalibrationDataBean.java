package uk.ac.exeter.QuinCe.web.datasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.PlotPageBean;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableList;

/**
 * Bean for handling review of calibration data
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ReviewCalibrationDataBean extends PlotPageBean {

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "calibration_data_plot";

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
   * Initialise the required data for the bean
   */
  @Override
  public void init() {
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
  protected List<Long> loadRowIds() throws Exception {
    return CalibrationDataDB.getCalibrationRowIds(getDataSource(), getDatasetId(), null);
  }

  @Override
  protected String buildTableHeadings() {
    StringBuilder headings = new StringBuilder();

    headings.append('[');
    headings.append("\"ID\",");
    headings.append("\"Date\",");
    headings.append("\"Run Type\",");
    headings.append("\"CO2\",");
    headings.append("\"Use?\",");
    headings.append("\"Use Message\"");
    headings.append(']');

    return headings.toString();
  }

  @Override
  protected String buildSelectableRows() throws Exception {
    List<Long> ids = CalibrationDataDB.getCalibrationRowIds(getDataSource(), getDatasetId(), null);
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
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  protected String buildPlotLabels(int plotIndex) {
    String result = null;

    if (plotIndex == 1) {
      // TODO This should be built dynamically in CalibrationDataDB
      return "[\"Date\",\"ID\",\"CO2\"]";
    }

    return result;
  }

  @Override
  protected String loadTableData(int start, int length) throws Exception {
    return CalibrationDataDB.getJsonTableData(getDataSource(), getDatasetId(), null, start, length);
  }

  /**
   * Get the flag indicating whether the selected calibrations are to be used
   * @return The use calibrations flag
   */
  public boolean getUseCalibrations() {
    return useCalibrations;
  }

  /**
   * Set the flag indicating whether the selected calibrations are to be used
   * @param useCalibrations The use calibrations flag
   */
  public void setUseCalibrations(boolean useCalibrations) {
    this.useCalibrations = useCalibrations;
  }

  /**
   * Get the message that will be attached to calibrations which aren't being used
   * @return The message for unused calibrations
   */
  public String getUseCalibrationsMessage() {
    return useCalibrationsMessage;
  }

  /**
   * Set the message that will be attached to calibrations which aren't being used
   * @param useCalibrationsMessage The message for unused calibrations
   */
  public void setUseCalibrationsMessage(String useCalibrationsMessage) {
    this.useCalibrationsMessage = useCalibrationsMessage;
  }

  /**
   * Set the usage status of the selected rows
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public void setCalibrationUse() throws MissingParamException, DatabaseException {

    try {
      CalibrationDataDB.setCalibrationUse(getDataSource(), getSelectedRowsList(), useCalibrations, useCalibrationsMessage);
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
    return variables.getGroup("CO2").getVariables();
  }

  @Override
  protected Variable getDefaultPlot2XAxis() {
    return null;
  }

  @Override
  protected List<Variable> getDefaultPlot2YAxis() {
    return new ArrayList<Variable>();
  }

  @Override
  protected Variable getDefaultMap1Variable() {
    return null;
  }

  @Override
  protected Variable getDefaultMap2Variable() {
    return null;
  }

  @Override
  protected void buildVariableList(VariableList variables) throws Exception {
    variables.addVariable("Date/Time", new Variable(Variable.TYPE_BASE, "Date/Time", "date", true, false, false));
    CalibrationDataDB.populateVariableList(getDataSource(), getDataset(), variables);
  }

  @Override
  protected String getData(List<String> fields) throws Exception {
    List<String> standardNames = new ArrayList<String>();
    for (Variable variable : plot1.getYAxisVariables()) {
      standardNames.add(variable.getFieldName());
    }

    return CalibrationDataDB.getJsonPlotData(getDataSource(), getDataset(), standardNames);
  }

  @Override
  public boolean getHasTwoPlots() {
    return false;
  }
}
