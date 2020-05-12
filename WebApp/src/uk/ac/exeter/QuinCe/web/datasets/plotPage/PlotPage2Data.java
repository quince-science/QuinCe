package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;

public abstract class PlotPage2Data {

  /**
   * Indicates a select action
   */
  protected static final int SELECT = 1;

  /**
   * Indicates a deselect action
   */
  protected static final int DESELECT = -1;

  /**
   * Json serialization type for lists of strings
   */
  Type stringList = new TypeToken<List<String>>() {
  }.getType();

  /**
   * Gson instance for serializing table data
   */
  private static Gson tableDataGson;

  /**
   * An error string to display to the user if something goes wrong.
   */
  private String errorMessage = null;

  /**
   * Indicates whether or not data has been loaded.
   */
  protected boolean loaded = false;

  /**
   * The column for which values have been selected
   */
  protected long selectedColumn = -1L;

  /**
   * The IDs of the selected rows
   */
  protected List<String> selectedRows = null;

  /**
   * The ID of the last row that was selected/deselected
   */
  protected String lastClickedRow = null;

  /**
   * Indicates whether the last selection action was a select or a deselect
   */
  protected int lastSelectionAction = SELECT;

  /**
   * The indicator of the root field group.
   *
   * <p>
   * This group is used as the basis for the page table - the columns are always
   * visible as the reference for the rest of the table. These columns are also
   * given a special status in plots and maps.
   * </p>
   */
  public static final String ROOT_FIELD_GROUP = "_ROOT";

  static {
    // Initialise Gson builder
    tableDataGson = new GsonBuilder().registerTypeAdapter(
      PlotPageTableRecord.class, new PlotPageTableRecordSerializer()).create();
  }

  /**
   * Load all page data.
   *
   * <p>
   * This calls {@link #loadDataAction(DataSource)} to do the actual loading
   * work.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @see #loadDataAction(DataSource)
   */
  public void loadData(DataSource dataSource) {
    try {
      loadDataAction(dataSource);
      loaded = true;
    } catch (Exception e) {
      error("Error while loading dataset data", e);
    }
  }

  /**
   * The actual method for loading the data.
   *
   * @param dataSource
   *          A data source.
   */
  protected abstract void loadDataAction(DataSource dataSource)
    throws Exception;

  /**
   * Get the column headings for the table in groups, without QC columns.
   *
   * <p>
   * Although all data values require accompanying QC Flag and QC Message
   * columns, they must not be included in the output of this method. The
   * application will ensure that they are included in the necessary places.
   * </p>
   *
   * <p>
   * The column headings will be returned as a map of
   * {@code <group>, <header list>} so the headings can be grouped. The map is a
   * {@link LinkedHashMap} so iterating over the map keys will always give the
   * same group (and therefore column) order.
   * </p>
   *
   * <p>
   * The first group should be named by {@link #ROOT_FIELD_GROUP}. This group
   * will be 'locked' in the display table so its columns are always visible.
   * </p>
   *
   * @return
   */
  protected abstract LinkedHashMap<String, List<ColumnHeading>> getColumnHeadings();

  /**
   * Get the table headings in JSON format.
   *
   * <pre>
   * The JSON will be an array of objects, one for each group. Each object will
   * contain the group name and an array of the column headings within that
   * group.
   * </p>
   *
   * <pre>
   * {[ { group: 'Group 1', headings: ['Heading 1', 'Heading 2'] }, { group:
   * 'Group 2', headings: ['Heading 3', 'Heading 4'] } ]}
   * </pre>
   *
   *
   * @return The table headings JSON.
   */
  public String getColumnHeadingsJson() {

    String result = null;

    if (loaded) {
      // Reorganise the column headings into a structure that can be used to
      // build the JSON
      LinkedHashMap<String, List<ColumnHeading>> headings = getColumnHeadings();

      List<JsonColumnGroup> jsonGroups = new ArrayList<JsonColumnGroup>(
        headings.size());

      if (validateColumnHeadings(headings)) {
        for (Map.Entry<String, List<ColumnHeading>> group : headings
          .entrySet()) {
          jsonGroups.add(new JsonColumnGroup(group));
        }
      }

      // Convert the reorganised data to JSON
      result = new Gson().toJson(jsonGroups);
    }

    return result;
  }

  /**
   * Get the columns indices at which each column group is positioned in the
   * full list of headings.
   *
   * <p>
   * The result is a map of {@code groupName -> first column index}.
   * </p>
   *
   * <p>
   * The root column group is not included because that group's columns are
   * always visible in the displayed table.
   * </p>
   *
   * @return The column indices for each group.
   */
  public LinkedHashMap<String, Integer> getColumnGroupOffsets() {

    LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();

    int nextColumn = 0;
    for (Map.Entry<String, List<ColumnHeading>> groupEntry : getColumnHeadings()
      .entrySet()) {

      if (!groupEntry.getKey().equals(ROOT_FIELD_GROUP)) {
        result.put(groupEntry.getKey(), nextColumn);
        nextColumn += groupEntry.getValue().size();
      }
    }

    return result;
  }

  /**
   * Validate the column headings supplied by {@link #getColumnHeadings()}.
   *
   * @param headings
   *          The column headings
   * @return {@code true} if the column headings are valid; {@code false} if not
   */
  private boolean validateColumnHeadings(
    LinkedHashMap<String, List<ColumnHeading>> headings) {

    boolean valid = true;

    // There must be at least one column group
    if (null == headings || headings.size() == 0) {
      error("No column headings available");
      valid = false;
    } else {
      for (Map.Entry<String, List<ColumnHeading>> group : headings.entrySet()) {

        // A column group name cannot be null or empty
        if (StringUtils.isBlank(group.getKey())) {
          error("Blank column group detected");
          valid = false;
        }

        List<ColumnHeading> groupColumns = group.getValue();

        // Each column group must contain at least one column
        if (null == groupColumns || groupColumns.size() == 0) {
          error("Empty column group detected",
            new Exception("Empty column group '" + group.getKey() + "'"));
        } else {

          for (int i = 0; i < groupColumns.size(); i++) {

            String columnName = groupColumns.get(i).getHeading();

            // Blank column is not allowed
            if (StringUtils.isBlank(columnName)) {
              error("Blank column heading detected",
                new Exception("Blank column heading in group " + group.getKey()
                  + ", index " + i));
            } else if (columnName
              .equals(FileDefinition.LONGITUDE_COLUMN_NAME)) {

              // Longitude must be followed by Latitude
              if (i + 1 == groupColumns.size() || !groupColumns.get(i + 1)
                .getHeading().equals(FileDefinition.LATITUDE_COLUMN_NAME)) {
                error("Invalid position columns",
                  new Exception("Longitude must be followed by Latitude"));
              }
            } else if (columnName.equals(FileDefinition.LATITUDE_COLUMN_NAME)) {

              // Latitude must be preceded by Longitude
              if (i == 0 || !groupColumns.get(i - 1).getHeading()
                .equals(FileDefinition.LONGITUDE_COLUMN_NAME)) {

                error("Invalid position columns",
                  new Exception("Latitude must be preceded by Longitude"));
              }
            }
          }
        }
      }
    }

    return valid;
  }

  /**
   * Get the last set error message.
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Register an error encountered during data processing.
   *
   * <p>
   * The error message will be displayed to the user. A dummy exception will be
   * created and logged.
   * </p>
   *
   * @param message
   *          The error message.
   */
  protected void error(String message) {
    error(message, new Exception(message));
  }

  /**
   * Register an error encountered during data processing.
   *
   * <p>
   * The error message will be displayed to the user. The cause will be logged
   * to the system.
   * </p>
   *
   * @param message
   *          The error message.
   * @param cause
   *          The underlying cause.
   */
  protected void error(String message, Throwable cause) {
    this.errorMessage = message;
    cause.printStackTrace();
  }

  /**
   * Get the number of records in the dataset.
   *
   * <p>
   * This is equivalent to the number of records that will be shown in the QC
   * table.
   * </p>
   *
   * @return The dataset size.
   */
  public abstract int size();

  /**
   * Generate the data for the table as a JSON String.
   *
   * <p>
   * The format for the JSON string is specified by
   * {@link PlotPage2Bean#generateTableData()}.
   * </p>
   *
   * @param start
   *          The first row to generate.
   * @param length
   *          The number of rows to generate.
   * @return The JSON string.
   *
   * @see PlotPage2Bean#generateTableData()
   */
  public String generateTableData(int start, int length) {

    String result = null;

    if (loaded) {
      List<PlotPageTableRecord> records = generateTableDataRecords(start,
        length);
      result = tableDataGson.toJson(records);
    }

    return result;
  }

  /**
   * Get the complete list of row IDs.
   *
   * @return The row IDs.
   */
  protected abstract List<String> getRowIDs();

  /**
   * Return the list of row IDs as a JSON string array.
   *
   * <p>
   * We can't use the standard JSF mechanism to directly return a
   * {@code List<String>} to the front end, because JSF 'helpfully' converts
   * numeric values to numbers instead of keeping them as strings which screws
   * up the Javascript processing.
   * </p>
   *
   * @return The row IDs as a JSON string.
   */
  public String getRowIDsJson() {
    return new Gson().toJson(getRowIDs());
  }

  /**
   * Generate the table data records for {@link #generateTableData(int, int)}.
   *
   * @param start
   *          The first row to generate.
   * @param length
   *          The number of rows to generate.
   * @return The table records.
   */
  protected abstract List<PlotPageTableRecord> generateTableDataRecords(
    int start, int length);

  /**
   * Utility class to represent a column header group as an independent Java
   * object.
   *
   * <p>
   * This is used by {@link PlotPage2Data#getColumnHeadingsJson()} to construct
   * the JSON object.
   * </p>
   *
   * @author Steve Jones
   *
   */
  class JsonColumnGroup {

    protected String group;

    protected List<ColumnHeading> headings;

    private JsonColumnGroup(
      Map.Entry<String, List<ColumnHeading>> headingGroup) {
      this.group = headingGroup.getKey();
      this.headings = headingGroup.getValue();
    }
  }

  /**
   * Get the currently selected column index.
   *
   * @return The selected column index.
   */
  public long getSelectedColumn() {
    return selectedColumn;
  }

  /**
   * Set the currently selected column index.
   *
   * @param selectedColumn
   *          The selected column index.
   */
  public void setSelectedColumn(long selectedColumn) {
    this.selectedColumn = selectedColumn;
  }

  /**
   * Get the current list of selected rows.
   *
   * @return The selected rows.
   */
  public String getSelectedRows() {
    return new Gson().toJson(selectedRows);
  }

  /**
   * Set the list of currently selected rows.
   *
   * <p>
   * Converts all supplied values to Strings. The method assumes that the
   * strings are already sorted.
   * </p>
   *
   * @param selectedRows
   *          The selected rows.
   */
  public void setSelectedRows(String selectedRows) {
    this.selectedRows = new Gson().fromJson(selectedRows, stringList);
  }

  /**
   * Get the ID of the last clicked row.
   *
   * @return The last clicked row
   */
  public String getLastClickedRow() {
    return lastClickedRow;
  }

  /**
   * Set the ID of the last clicked row.
   *
   * @param lastClickedRow
   *          The last clicked row.
   */
  public void setLastClickedRow(String lastClickedRow) {
    this.lastClickedRow = lastClickedRow;
  }

  /**
   * Get the last selection action
   *
   * @return The last selection action
   * @see #SELECT
   * @see #DESELECT
   */
  public int getLastSelectionAction() {
    return lastSelectionAction;
  }

  /**
   * Set the last selection action
   *
   * @return The last selection action
   * @see #SELECT
   * @see #DESELECT
   */
  public void setLastSelectionAction(int lastSelectionAction) {
    this.lastSelectionAction = lastSelectionAction;
  }
}
