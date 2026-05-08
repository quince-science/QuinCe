package uk.ac.exeter.QuinCe.data.Export;

import java.lang.reflect.Constructor;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.math.NumberUtils;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.export.ExportData;

/**
 * Class to hold details of a single export configuration
 *
 * @see ExportConfig
 */
public class ExportOption {

  public static final int HEADER_MODE_SHORT = 0;

  public static final int HEADER_MODE_LONG = 1;

  public static final int HEADER_MODE_CODE = 2;

  protected static final String EXPORT_DATA_PACKAGE = "uk.ac.exeter.QuinCe.web.datasets.export.";

  protected static final String DEFAULT_EXPORT_DATA_CLASS = EXPORT_DATA_PACKAGE
    + "ExportData";

  private int index;

  /**
   * The name of this export configuration
   */
  private String name;

  /**
   * The separator to be used between columns in the output file
   */
  private String separator;

  /**
   * The string to put in files where a value is missing
   */
  private String missingValue = "NaN";

  /**
   * The string to use for QC flags of missing values
   */
  private String missingQcFlag = "";

  /**
   * The variables to be exported as part of this export. If any instrument does
   * not contain a given variable it will be ignored
   */
  private List<Variable> variables;

  /**
   * The export data class to use for this export. Used for custom
   * post-processors
   */
  private Class<? extends ExportData> dataClass;

  /**
   * Indicates whether raw sensors will be exported
   */
  private boolean includeRawSensors = false;

  /**
   * Indicates whether all intermediate calculation columns will be exported, or
   * just the final calculated value
   */
  private boolean includeCalculationColumns = false;

  /**
   * Indicates whether to use column codes instead of names
   */
  private int headerMode = HEADER_MODE_LONG;

  /**
   * Indicates whether units should be included in column headings
   */
  private boolean includeUnits = true;

  /**
   * Indicates whether QC comments should be included in the output
   */
  private boolean includeQCComments = true;

  /**
   * The header for the timestamp column
   */
  private String timestampHeader = null;

  /**
   * The format to use for timestamps.
   */
  private DateTimeFormatter timestampFormat = null;

  /**
   * QC Flag header suffix
   */
  private String qcFlagSuffix = " QC Flag";

  /**
   * QC Comment header suffix
   */
  private String qcCommentSuffix = " QC Comment";

  /**
   * Custom column header replacements
   */
  private Map<String, String> replacementColumnHeaders = null;

  /**
   * List of columns to be excluded from export.
   */
  private List<String> excludedColumns = null;

  /**
   * Only export measurements and their related sensor values.
   */
  private boolean measurementsOnly = false;

  /**
   * Do not export Bad measurements and/or sensor values.
   *
   * <p>
   * If a bad sensor value is used by a good measurement, it will still be
   * included.
   * </p>
   */
  private boolean skipBad = false;

  /**
   * Indicates whether export option should be visible in webapp
   */
  private boolean visible = true;

  /**
   * Build an ExportOption object from a JSON string
   *
   * @param index
   *          The index of the configuration in the export configuration file
   * @param json
   *          The JSON string defining the export option
   * @throws ExportException
   *           If the export configuration is invalid
   */
  @SuppressWarnings("unchecked")
  public ExportOption(int index, String name, String separator,
    List<Variable> variables) throws ExportConfigurationException {
    this.index = index;
    this.name = name;
    this.separator = separator;
    this.variables = variables;

    try {
      this.dataClass = (Class<? extends ExportData>) Class
        .forName(DEFAULT_EXPORT_DATA_CLASS);
    } catch (ClassNotFoundException e) {
      // Since we've already checked the class, this shouldn't really happen
      throw new ExportConfigurationException(name, e);
    }

  }

  protected void setVisible(boolean visible) {
    this.visible = visible;
  }

  protected void setMissingValue(String missingValue) {
    this.missingValue = missingValue;
  }

  protected void setMissingQCFlag(String missingQCFlag) {
    this.missingQcFlag = missingQCFlag;
  }

  protected void setIncludeRawSensors(boolean includeRawSensors) {
    this.includeRawSensors = includeRawSensors;
  }

  protected void setIncludeCalculationColumns(
    boolean includeCalculationColuns) {
    this.includeCalculationColumns = includeCalculationColuns;
  }

  protected void setHeaderMode(int headerMode)
    throws ExportConfigurationException {
    validateHeaderMode(headerMode);
    this.headerMode = headerMode;
  }

  protected void setIncludeUnits(boolean includeUnits) {
    this.includeUnits = includeUnits;
  }

  protected void setIncludeQCComments(boolean includeQCComments) {
    this.includeQCComments = includeQCComments;
  }

  protected void setTimestampHeader(String timestampHeader) {
    this.timestampHeader = timestampHeader;
  }

  protected void setTimestampFormat(DateTimeFormatter timestampFormat) {
    this.timestampFormat = timestampFormat;
  }

  protected void setQcFlagSuffix(String qcFlagSuffix) {
    this.qcFlagSuffix = qcFlagSuffix;
  }

  protected void setQcCommentSuffix(String qcCommentSuffix) {
    this.qcCommentSuffix = qcCommentSuffix;
  }

  protected void setMeasurementsOnly(boolean measurementsOnly) {
    this.measurementsOnly = measurementsOnly;
  }

  protected void setSkipBad(boolean skipBad) {
    this.skipBad = skipBad;
  }

  protected void setDataClass(Class<? extends ExportData> dataClass) {
    this.dataClass = dataClass;
  }

  protected void setReplacementColumnHeaders(
    Map<String, String> replacementColumnHeaders) {
    this.replacementColumnHeaders = replacementColumnHeaders;
  }

  protected void setExcludedColumns(List<String> excludedColumns) {
    this.excludedColumns = excludedColumns;
  }

  private void validateHeaderMode(int mode)
    throws ExportConfigurationException {
    if (mode != HEADER_MODE_SHORT && mode != HEADER_MODE_LONG
      && mode != HEADER_MODE_CODE) {
      throw new ExportConfigurationException(name,
        "Invalid header mode '" + headerMode + "'");
    }
  }

  public int getIndex() {
    return index;
  }

  /**
   * Returns the name of the configuration
   *
   * @return The configuration name
   */
  public String getName() {
    return name;
  }

  /**
   * The column separator to be used in the export file
   *
   * @return The column separator
   */
  public String getSeparator() {
    return separator;
  }

  public String getMissingValue() {
    return missingValue;
  }

  public String getMissingQcFlag() {
    return missingQcFlag;
  }

  public List<Variable> getVariables() {
    return variables;
  }

  public boolean includeRawSensors() {
    return includeRawSensors;
  }

  public boolean includeCalculationColumns() {
    return includeCalculationColumns;
  }

  public int getHeaderMode() {
    return headerMode;
  }

  public boolean includeUnits() {
    return includeUnits;
  }

  public boolean measurementsOnly() {
    return measurementsOnly;
  }

  public boolean skipBad() {
    return skipBad;
  }

  public boolean filterSensorValues() {
    return measurementsOnly || skipBad;
  }

  public boolean getVisible() {
    return visible;
  }

  /**
   * Get the file extension for this export option, based on the separator used
   *
   * @return The file extension
   */
  public String getFileExtension() {
    String result;

    switch (separator) {
    case ",": {
      result = ".csv";
      break;
    }
    case "\t": {
      result = ".tsv";
      break;
    }
    default: {
      result = ".txt";
    }
    }

    return result;
  }

  public boolean includeQCComments() {
    return includeQCComments;
  }

  public String getQcFlagSuffix() {
    return qcFlagSuffix;
  }

  public String getQcCommentSuffix() {
    return qcCommentSuffix;
  }

  public String getTimestampHeader() {
    return timestampHeader;
  }

  public DateTimeFormatter getTimestampFormatter() {
    return timestampFormat;
  }

  public String getReplacementHeader(String code) {
    String result = null;
    if (null != replacementColumnHeaders
      && null != replacementColumnHeaders.get(code)) {
      result = replacementColumnHeaders.get(code);
    }
    return result;
  }

  /**
   * Get the class to be used for building the export data. This is typically
   * used for customizing data post-processors before the final export.
   *
   * @return The export data class
   */
  public Class<? extends ExportData> getExportDataClass() {
    return dataClass;
  }

  /**
   * Create the ExportData object to be used for this Export Option
   *
   * @param dataSource
   *          A data source
   * @param instrument
   *          The instrument that the exported data set belongs to
   * @param dataset
   *          The dataset that will be exported
   * @return The ExportData object
   */
  public ExportData makeExportData(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws ExportConfigurationException {

    try {
      Constructor<? extends ExportData> constructor = dataClass.getConstructor(
        DataSource.class, Instrument.class, DataSet.class, this.getClass());

      return constructor.newInstance(dataSource, instrument, dataset, this);
    } catch (Exception e) {
      throw new ExportConfigurationException(name,
        "Error creating ExportData object", e);
    }
  }

  /**
   * Format a field value to fit the export format.
   *
   * <p>
   * Newlines are replaced with semicolons. Instances of the separator are
   * replaced with spaces. Double quotes are escaped.
   * </p>
   *
   * @param fieldValue
   *          The value to be formatted.
   * @return The formatted value.
   */
  public String format(String fieldValue) {
    String result = null;

    if (null == fieldValue) {
      result = fieldValue;
    } else if (NumberUtils.isCreatable(fieldValue)) {
      result = StringUtils.formatNumber(fieldValue);
    } else {
      String newlinesRemoved = fieldValue.replaceAll("[\\r\\n]", " ");
      String separatorsRemoved = newlinesRemoved.replaceAll(separator, " ");
      String quotesEscaped = separatorsRemoved.replaceAll("\"", "'");
      return quotesEscaped;
    }

    return result;
  }

  /**
   * Determine whether or not a header should be excluded from the export.
   *
   * <p>
   * The heading is checked using both its short name and code name.
   * </p>
   *
   * @param header
   *          The header.
   * @return {@code true} if the header should be excluded; {@code false}
   *         otherwise.
   */
  public boolean columnExcluded(ColumnHeading heading) {
    boolean result = false;

    if (null != excludedColumns) {
      result = excludedColumns.contains(heading.getCodeName())
        || excludedColumns.contains(heading.getShortName());
    }

    return result;
  }
}
