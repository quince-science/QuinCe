package uk.ac.exeter.QuinCe.web.datasets.export;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.sql.DataSource;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Export.ExportConfig;
import uk.ac.exeter.QuinCe.data.Export.ExportException;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@ManagedBean
@SessionScoped
public class ExportBean extends BaseManagedBean {

  /**
   * Navigation to the export page
   */
  private static final String NAV_EXPORT_PAGE = "export";

  /**
   * The dataset to be exported
   */
  private DataSet dataset = null;

  /**
   * The chosen export option
   */
  private int chosenExportOption = -1;

  /**
   * Indicates whether raw files should be included in the export
   */
  private boolean includeRawFiles = false;

  /**
   * Initialise the bean
   */
  public String start() {
    return NAV_EXPORT_PAGE;
  }

  /**
   * Get the dataset ID
   *
   * @return The dataset ID
   */
  public long getDatasetId() {
    long result = -1;
    if (dataset != null) {
      result = dataset.getId();
    }

    return result;
  }

  /**
   * Set the dataset using its ID
   *
   * @param datasetId
   *          The dataset ID
   */
  public void setDatasetId(long datasetId) {
    try {
      this.dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the dataset
   *
   * @return The dataset
   */
  public DataSet getDataset() {
    return dataset;
  }

  /**
   * Set the dataset
   *
   * @param dataset
   *          The dataset
   */
  public void setDataset(DataSet dataset) {
    this.dataset = dataset;
  }

  /**
   * Get the list of available file export options
   *
   * @return The export options
   * @throws ExportException
   *           In an error occurs while retrieving the export options
   */
  public List<ExportOption> getExportOptions() throws ExportException {
    List<ExportOption> options = ExportConfig.getInstance().getOptions();
    if (chosenExportOption == -1 && options.size() > 0) {
      chosenExportOption = options.get(0).getIndex();
    }

    return options;
  }

  /**
   * Return the ID of the chosen file export option
   *
   * @return The export option ID
   */
  public int getChosenExportOption() {
    return chosenExportOption;
  }

  /**
   * Set the ID of the chosen export option
   *
   * @param chosenExportOption
   *          The export option ID
   */
  public void setChosenExportOption(int chosenExportOption) {
    this.chosenExportOption = chosenExportOption;
  }

  /**
   * Export the dataset in the chosen format
   */
  public void exportDataset() {

    if (includeRawFiles) {
      exportDatasetWithRawFiles();
    } else {
      exportSingleFile();
    }
  }

  /**
   * Export a dataset file by itself
   */
  private void exportSingleFile() {

    try {
      ExportOption exportOption = getExportOptions().get(chosenExportOption);

      byte[] fileContent = getDatasetExport(getCurrentInstrument(), dataset,
        exportOption);

      FacesContext fc = FacesContext.getCurrentInstance();
      ExternalContext ec = fc.getExternalContext();

      ec.responseReset();
      ec.setResponseContentType("text/csv");

      // Set it with the file size. This header is optional. It will work if
      // it's omitted,
      // but the download progress will be unknown.
      ec.setResponseContentLength(fileContent.length);

      // The Save As popup magic is done here. You can give it any file name you
      // want, this only won't work in MSIE,
      // it will use current request URL as file name instead.
      ec.setResponseHeader("Content-Disposition",
        "attachment; filename=\"" + getExportFilename(exportOption) + "\"");

      OutputStream outputStream = ec.getResponseOutputStream();
      outputStream.write(fileContent);

      fc.responseComplete();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create a ZIP file containing a dataset and its source raw data files
   */
  private void exportDatasetWithRawFiles() {

    Connection conn = null;

    try {
      conn = getDataSource().getConnection();
      ExportOption exportOption = getExportOptions().get(chosenExportOption);

      byte[] outBytes = buildExportZip(conn, getCurrentInstrument(), dataset,
        exportOption);

      FacesContext fc = FacesContext.getCurrentInstance();
      ExternalContext ec = fc.getExternalContext();

      ec.responseReset();
      ec.setResponseContentType("application/zip");

      // Set it with the file size. This header is optional. It will work if
      // it's omitted,
      // but the download progress will be unknown.
      ec.setResponseContentLength(outBytes.length);

      // The Save As popup magic is done here. You can give it any file name you
      // want, this only won't work in MSIE,
      // it will use current request URL as file name instead.
      ec.setResponseHeader("Content-Disposition",
        "attachment; filename=\"" + dataset.getName() + ".zip\"");

      OutputStream outputStream = ec.getResponseOutputStream();
      outputStream.write(outBytes);

      fc.responseComplete();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Obtain a dataset in the specified format as a byte array ready for export
   * or storage
   *
   * @param dataSource
   *          A data source
   * @param dataSet
   *          The dataset
   * @param exportOption
   *          The export format
   * @return The exported dataset
   * @throws Exception
   */
  private static byte[] getDatasetExport(Instrument instrument, DataSet dataset,
    ExportOption exportOption) throws Exception {

    DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

    ExportData data = exportOption.makeExportData(dataSource, instrument,
      dataset);
    data.loadData();

    // Run the post-processor before generating the final output
    data.postProcess();

    // Initialise the output
    StringBuilder output = new StringBuilder();

    List<String> headers = makeHeaders(instrument, data, exportOption, output);
    output.append(
      StringUtils.collectionToDelimited(headers, exportOption.getSeparator()));
    output.append('\n');

    // Process each row of the data
    for (Long rowId : data.getRowIDs()) {
      boolean firstColumn = true;

      // Time and position
      List<PlotPageColumnHeading> baseColumns = data.getExtendedColumnHeadings()
        .get(ExportData.ROOT_FIELD_GROUP);

      for (PlotPageColumnHeading column : baseColumns) {
        if (firstColumn) {
          firstColumn = false;
        } else {
          output.append(exportOption.getSeparator());
        }

        PlotPageTableValue value = data.getColumnValue(rowId, column.getId());

        addValueToOutput(output, exportOption, column.getId(), value,
          column.hasQC(), column.includeType());
      }

      // Measurement values
      Measurement measurement = data.getMeasurement(data.getRowTime(rowId));

      List<PlotPageColumnHeading> measurementValueColumns = data
        .getExtendedColumnHeadings()
        .get(ExportData.MEASUREMENTVALUES_FIELD_GROUP);

      for (PlotPageColumnHeading column : measurementValueColumns) {

        SensorType sensorType = ResourceManager.getInstance()
          .getSensorsConfiguration().getSensorType(column.getId());

        // Get the value for this SensorType. If there's a measurement and it
        // contains the SensorType, use that; otherwise get the original
        // SensorValue.
        PlotPageTableValue value = null;

        if (null != measurement
          && measurement.containsMeasurementValue(sensorType)) {
          value = measurement.getMeasurementValue(sensorType);
        } else {
          // TODO #1128 Handle multiple sensors
          List<Long> sensorTypeColumns = instrument.getSensorAssignments()
            .getColumnIds(sensorType);

          if (null != sensorTypeColumns && sensorTypeColumns.size() > 0) {
            value = data.getColumnValue(rowId, sensorTypeColumns.get(0));
          }
        }

        boolean useValueInThisColumn;

        if (columnsWithId(measurementValueColumns, column.getId()) == 1) {

          // There is only one column registered for this SensorType, so use it
          useValueInThisColumn = true;
        } else {
          // There are multiple columns for this SensorType, so where the value
          // goes is determined by the measurement's Run Type
          if (null == measurement) {
            // There is no measurement, so we leave the column blank
            useValueInThisColumn = false;

          } else {
            // If this column is for the Run Type of the measurement, we
            // populate it. Otherwise we leave it blank - there'll be another
            // column for the Run Type somewhere (or perhaps not, if it's a
            // non-measurement run type eg gas standard run)
            String runType = measurement.getRunType();

            // Look through all the column headings defined for the run type to
            // see if it contains our current column. If it does, we add the
            // value. If not, it'll be blank.
            Set<ColumnHeading> runTypeColumns = instrument
              .getAllVariableColumnHeadings(runType);

            useValueInThisColumn = ColumnHeading
              .containsColumnWithCode(runTypeColumns, column.getCodeName());
          }
        }

        output.append(exportOption.getSeparator());
        addValueToOutput(output, exportOption, column.getId(),
          useValueInThisColumn ? value : null, true, true);
      }

      // Data Reduction for all variables
      for (Variable variable : exportOption.getVariables()) {
        if (instrument.getVariables().contains(variable)) {
          List<CalculationParameter> params = DataReducerFactory
            .getCalculationParameters(variable,
              exportOption.includeCalculationColumns());

          for (CalculationParameter param : params) {
            output.append(exportOption.getSeparator());

            PlotPageTableValue value = data.getColumnValue(rowId,
              param.getId());
            addValueToOutput(output, exportOption, param.getId(), value,
              param.isResult(), false);
          }
        }
      }

      if (exportOption.includeRawSensors()) {
        List<PlotPageColumnHeading> sensorHeadings = data
          .getExtendedColumnHeadings().get(ExportData.SENSORS_FIELD_GROUP);

        for (PlotPageColumnHeading heading : sensorHeadings) {
          output.append(exportOption.getSeparator());

          PlotPageTableValue value = data.getColumnValue(rowId,
            heading.getId());
          addValueToOutput(output, exportOption, heading.getId(), value, true,
            false);
        }

        List<PlotPageColumnHeading> diagnosticHeadings = data
          .getExtendedColumnHeadings().get(ExportData.DIAGNOSTICS_FIELD_GROUP);

        if (null != diagnosticHeadings) {
          for (PlotPageColumnHeading heading : diagnosticHeadings) {
            output.append(exportOption.getSeparator());

            PlotPageTableValue value = data.getColumnValue(rowId,
              heading.getId());
            addValueToOutput(output, exportOption, heading.getId(), value, true,
              false);
          }
        }
      }

      output.append("\n");
    }

    // Destroy the ExportData object so it cleans up its resources
    data.destroy();

    return output.toString().getBytes();
  }

  /**
   * Create the header line in an output string.
   *
   * @param data
   *          The source data.
   * @param output
   *          The output.
   * @throws Exception
   */
  private static List<String> makeHeaders(Instrument instrument,
    ExportData data, ExportOption exportOption, StringBuilder output)
    throws Exception {

    List<String> headers = new ArrayList<String>();

    // Time and position
    for (PlotPageColumnHeading heading : data.getExtendedColumnHeadings()
      .get(ExportData.ROOT_FIELD_GROUP)) {
      addHeader(headers, exportOption, heading);
    }

    // Measurement Sensor Types - these are the calculated sensor values
    // used as input for the data reducers
    for (PlotPageColumnHeading measurementValueHeading : data
      .getExtendedColumnHeadings()
      .get(ExportData.MEASUREMENTVALUES_FIELD_GROUP)) {

      addHeader(headers, exportOption, measurementValueHeading);
    }

    // Headers for each variable
    for (Variable variable : exportOption.getVariables()) {
      if (instrument.getVariables().contains(variable)) {

        List<CalculationParameter> params = DataReducerFactory
          .getCalculationParameters(variable,
            exportOption.includeCalculationColumns());

        for (CalculationParameter param : params) {
          addHeader(headers, exportOption, param);
        }
      }
    }

    // Raw sensors, if required. These always use the Short name, which
    // is the sensor name defined for the instrument. Includes diagnostics.
    if (exportOption.includeRawSensors()) {
      List<PlotPageColumnHeading> sensorHeadings = data
        .getExtendedColumnHeadings().get(ExportData.SENSORS_FIELD_GROUP);

      for (PlotPageColumnHeading heading : sensorHeadings) {
        addHeader(headers, exportOption, heading,
          ExportOption.HEADER_MODE_SHORT);
      }

      List<PlotPageColumnHeading> diagnosticHeadings = data
        .getExtendedColumnHeadings().get(ExportData.DIAGNOSTICS_FIELD_GROUP);

      if (null != diagnosticHeadings) {
        for (PlotPageColumnHeading heading : diagnosticHeadings) {
          addHeader(headers, exportOption, heading,
            ExportOption.HEADER_MODE_SHORT);
        }
      }
    }

    return headers;
  }

  private static void addValueToOutput(StringBuilder output,
    ExportOption exportOption, long columnId, PlotPageTableValue value,
    boolean includeQcColumns, boolean includeType) {

    if (null == value) {
      // Value
      output.append(exportOption.getMissingValue());

      // QC Flag
      if (columnId != FileDefinition.TIME_COLUMN_ID && includeQcColumns) {
        output.append(exportOption.getSeparator());
        output.append(exportOption.getMissingQcFlag());

        // QC Comment if required
        if (exportOption.includeQCComments()) {
          output.append(exportOption.getSeparator());
        }
      }

      // Type
      if (includeType) {
        output.append(exportOption.getSeparator());
      }
    } else {

      // Value
      if (null == value.getValue()) {
        output.append(exportOption.getMissingValue());
      } else {
        output.append(exportOption.format(value.getValue()));
      }

      // QC Flag
      if (columnId != FileDefinition.TIME_COLUMN_ID && includeQcColumns) {
        output.append(exportOption.getSeparator());
        output.append(value.getQcFlag().getWoceValue());

        // QC Comment
        if (exportOption.includeQCComments()) {
          output.append(exportOption.getSeparator());
          output.append('"' + exportOption.format(value.getQcMessage()) + '"');
        }
      }

      if (includeType) {
        output.append(exportOption.getSeparator());
        output.append(value.getType());
      }
    }
  }

  private static void addHeader(List<String> headers, ExportOption exportOption,
    ColumnHeading heading) throws ExportException {
    addHeader(headers, exportOption, heading, null);
  }

  private static void addHeader(List<String> headers, ExportOption exportOption,
    ColumnHeading heading, Integer mode) throws ExportException {

    String header = null;

    if (null != mode) {
      header = getModeHeader(heading, (int) mode);
    } else {
      String replacementHeader = exportOption
        .getReplacementHeader(heading.getCodeName());
      if (null != replacementHeader) {
        header = replacementHeader;
      } else {
        header = getModeHeader(heading, exportOption.getHeaderMode());
      }
    }

    if (exportOption.includeUnits() && null != heading.getUnits()
      && heading.getUnits().length() > 0)

    {
      headers.add(header + " [" + heading.getUnits() + ']');
    } else {
      headers.add(header);
    }

    if (heading.hasQC()) {
      headers.add(header + exportOption.getQcFlagSuffix());

      if (exportOption.includeQCComments()) {
        headers.add(header + exportOption.getQcCommentSuffix());
      }
    }

    if (heading.includeType()) {
      headers.add(header + " Type");
    }
  }

  private static String getModeHeader(ColumnHeading heading, int mode)
    throws ExportException {

    String header;

    switch (mode) {
    case ExportOption.HEADER_MODE_SHORT: {
      header = heading.getShortName();
      break;
    }
    case ExportOption.HEADER_MODE_LONG: {
      header = heading.getLongName();
      break;
    }
    case ExportOption.HEADER_MODE_CODE: {
      header = heading.getCodeName();
      break;
    }
    default: {
      throw new ExportException("Unrecognised header mode");
    }
    }

    return header;
  }

  /**
   * Get the filename of the file that will be exported
   *
   * @param exportOption
   *          The export option
   * @return The export filename
   * @throws Exception
   *           If any errors occur
   */
  private String getExportFilename(ExportOption exportOption) throws Exception {
    StringBuffer fileName = new StringBuffer(
      dataset.getName().replaceAll("\\.", "_"));
    fileName.append('-');
    fileName.append(exportOption.getName());

    if (exportOption.getSeparator().equals("\t")) {
      fileName.append(".tsv");
    } else {
      fileName.append(".csv");
    }

    return fileName.toString();
  }

  /**
   * Determine whether raw files should be included in the export
   *
   * @return {@code true} if raw files should be included; {@code false} if not
   */
  public boolean getIncludeRawFiles() {
    return includeRawFiles;
  }

  /**
   * Specify whether raw files should be included in the export
   *
   * @param includeRawFiles
   *          The raw files flag
   */
  public void setIncludeRawFiles(boolean includeRawFiles) {
    this.includeRawFiles = includeRawFiles;
  }

  /**
   * Create the contents of the manifest.json file
   *
   * @return The manifest JSON
   */
  private static JSONObject makeManifest(Connection conn, DataSet dataset,
    List<ExportOption> exportOptions, List<DataFile> rawFiles)
    throws Exception {

    JSONObject result = new JSONObject();

    JSONObject manifest = new JSONObject();
    JSONArray raw = new JSONArray();
    for (DataFile file : rawFiles) {
      JSONObject fileJson = new JSONObject();
      fileJson.put("filename", file.getFilename());
      fileJson.put("startDate", DateTimeUtils.toIsoDate(file.getStartDate()));
      fileJson.put("endDate", DateTimeUtils.toIsoDate(file.getEndDate()));
      raw.put(fileJson);
    }
    manifest.put("raw", raw);

    JSONArray datasetArray = new JSONArray();
    for (ExportOption exportOption : exportOptions) {
      JSONObject datasetObject = new JSONObject();
      datasetObject.put("destination", exportOption.getName());
      datasetObject.put("filename",
        dataset.getName() + exportOption.getFileExtension());
      datasetArray.put(datasetObject);
    }

    manifest.put("dataset", datasetArray);

    JSONObject metadata = DataSetDB.getMetadataJson(conn, dataset);
    Properties appConfig = ResourceManager.getInstance().getConfig();

    metadata.put("quince_information", "Data processed using QuinCe version "
      + appConfig.getProperty("version"));
    manifest.put("metadata", metadata);

    result.put("manifest", manifest);
    return result;
  }

  /**
   * Build a ZIP file containing a full dataset export, including the raw files
   * used to build the dataset and a manifest containing metadata and details of
   * the files.
   *
   * The {@code exportOption} defines the export format to be used. If this is
   * {@code null}, all formats will be exported.
   *
   * @param conn
   *          A database connection
   * @param dataset
   *          The dataset to export
   * @param exportOption
   *          The export option to use
   * @return The export ZIP file
   * @throws Exception
   *           All exceptions are propagated upwards
   */
  public static byte[] buildExportZip(Connection conn, Instrument instrument,
    DataSet dataset, ExportOption exportOption) throws Exception {

    ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
    ZipOutputStream zip = new ZipOutputStream(zipOut);

    String dirRoot = dataset.getName();

    List<ExportOption> exportOptions;
    if (null != exportOption) {
      exportOptions = new ArrayList<ExportOption>(1);
      exportOptions.add(exportOption);
    } else {
      exportOptions = ExportConfig.getInstance().getOptions();
    }

    for (ExportOption option : exportOptions) {
      // Add the main dataset file
      String datasetPath = dirRoot + "/dataset/" + option.getName() + "/"
        + dataset.getName() + option.getFileExtension();

      ZipEntry datasetEntry = new ZipEntry(datasetPath);
      zip.putNextEntry(datasetEntry);
      zip.write(getDatasetExport(instrument, dataset, option));
      zip.closeEntry();
    }

    List<Long> rawIds = dataset.getSourceFiles(conn);
    List<DataFile> files = DataFileDB.getDataFiles(conn,
      ResourceManager.getInstance().getConfig(), rawIds);

    for (DataFile file : files) {
      String filePath = dirRoot + "/raw/" + file.getFilename();

      ZipEntry rawEntry = new ZipEntry(filePath);
      zip.putNextEntry(rawEntry);
      zip.write(file.getBytes());
      zip.closeEntry();
    }

    // Manifest
    JSONObject manifest = makeManifest(conn, dataset, exportOptions, files);
    ZipEntry manifestEntry = new ZipEntry(dirRoot + "/manifest.json");
    zip.putNextEntry(manifestEntry);
    zip.write(manifest.toString().getBytes());
    zip.closeEntry();

    // Write the response
    zip.close();
    byte[] outBytes = zipOut.toByteArray();
    zipOut.close();

    return outBytes;
  }

  private static long columnsWithId(List<PlotPageColumnHeading> columns,
    long id) {
    return columns.stream().filter(c -> c.getId() == id).count();
  }
}
