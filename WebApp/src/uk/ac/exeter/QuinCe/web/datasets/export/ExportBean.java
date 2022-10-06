package uk.ac.exeter.QuinCe.web.datasets.export;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.sql.DataSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
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
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.NullPlotPageTableValue;
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
   * The chosen export options
   */
  private List<Integer> chosenExportOptions = new ArrayList<Integer>();

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
      ExceptionUtils.printStackTrace(e);
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
    return ExportConfig.getInstance().getOptions();
  }

  /**
   * Return the ID of the chosen file export option
   *
   * @return The export option ID
   */
  public List<Integer> getChosenExportOptions() {
    return chosenExportOptions;
  }

  /**
   * Set the ID of the chosen export option
   *
   * @param chosenExportOption
   *          The export option ID
   */
  public void setChosenExportOptions(List<Integer> chosenExportOptions) {
    this.chosenExportOptions = chosenExportOptions;
  }

  /**
   * Export the dataset in the chosen format
   */
  public void exportDataset() {
    exportDatasetWithRawFiles();
  }

  /**
   * Create a ZIP file containing a dataset and its source raw data files
   */
  private void exportDatasetWithRawFiles() {

    Connection conn = null;

    try {
      conn = getDataSource().getConnection();

      byte[] outBytes = buildExportZip(conn, getCurrentInstrument(), dataset,
        ExportConfig.getInstance().getOptions(chosenExportOptions));

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
      ExceptionUtils.printStackTrace(e);
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
  private static DatasetExport getDatasetExport(Instrument instrument,
    DataSet dataset, ExportOption exportOption) throws Exception {

    DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

    ExportData data = exportOption.makeExportData(dataSource, instrument,
      dataset);
    data.loadData();

    // Run the post-processor before generating the final output
    data.postProcess();

    // Initialise the output
    DatasetExport result = new DatasetExport();

    List<String> headers = makeHeaders(instrument, data, exportOption, result);
    result.append(
      StringUtils.collectionToDelimited(headers, exportOption.getSeparator()));
    result.append('\n');

    // Process each row of the data
    for (Long rowId : data.getRowIDs()) {
      if (data.containsTime(DateTimeUtils.longToDate(rowId),
        exportOption.includeRawSensors())) {

        boolean firstColumn = true;

        // Time and position
        List<PlotPageColumnHeading> baseColumns = data
          .getExtendedColumnHeadings().get(ExportData.ROOT_FIELD_GROUP);

        for (PlotPageColumnHeading column : baseColumns) {
          if (firstColumn) {
            firstColumn = false;
          } else {
            result.append(exportOption.getSeparator());
          }

          PlotPageTableValue value = data.getColumnValue(rowId, column.getId());

          addValueToOutput(result, exportOption, column.getId(), value,
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

          PlotPageTableValue value = null;

          if (null != measurement
            && measurement.containsMeasurementValue(sensorType)) {
            value = measurement.getMeasurementValue(sensorType);
          } else {
            value = new NullPlotPageTableValue();
          }

          boolean useValueInThisColumn;

          if (columnsWithId(measurementValueColumns, column.getId()) == 1) {

            // There is only one column registered for this SensorType, so use
            // it
            useValueInThisColumn = true;
          } else {
            // There are multiple columns for this SensorType (e.g. xCO2 is for
            // underway marine pCO2 and underway atmospheric pCO2, so where the
            // value goes is determined by the measurement's Run Type
            if (null == measurement) {
              // There is no measurement, so we leave the column blank
              useValueInThisColumn = false;

            } else {

              // If this column is for the Run Type of the measurement, we
              // populate it. Otherwise we leave it blank - there'll be another
              // column for the Run Type somewhere (or perhaps not, if it's a
              // non-measurement run type eg gas standard run)
              String runType = measurement
                .getRunType(Measurement.GENERIC_RUN_TYPE_VARIABLE);

              // Look through all the column headings defined for the run type
              // to
              // see if it contains our current column. If it does, we add the
              // value. If not, it'll be blank.
              Set<ColumnHeading> runTypeColumns = instrument
                .getAllVariableColumnHeadings(runType);

              useValueInThisColumn = ColumnHeading
                .containsColumnWithCode(runTypeColumns, column.getCodeName());
            }
          }

          result.append(exportOption.getSeparator());
          addValueToOutput(result, exportOption, column.getId(),
            useValueInThisColumn ? value : null, true, true);
        }

        // Data Reduction for all variables
        for (Variable variable : exportOption.getVariables()) {
          if (instrument.getVariables().contains(variable)) {
            List<CalculationParameter> params = DataReducerFactory
              .getCalculationParameters(variable,
                exportOption.includeCalculationColumns());

            for (CalculationParameter param : params) {
              result.append(exportOption.getSeparator());

              PlotPageTableValue value = data.getColumnValue(rowId,
                param.getId());
              addValueToOutput(result, exportOption, param.getId(), value,
                param.isResult(), false);
            }
          }
        }

        if (exportOption.includeRawSensors()) {
          List<PlotPageColumnHeading> sensorHeadings = data
            .getExtendedColumnHeadings().get(ExportData.SENSORS_FIELD_GROUP);

          for (PlotPageColumnHeading heading : sensorHeadings) {
            result.append(exportOption.getSeparator());

            PlotPageTableValue value = data.getColumnValue(rowId,
              heading.getId());
            addValueToOutput(result, exportOption, heading.getId(), value, true,
              false);
          }

          List<PlotPageColumnHeading> diagnosticHeadings = data
            .getExtendedColumnHeadings()
            .get(ExportData.DIAGNOSTICS_FIELD_GROUP);

          if (null != diagnosticHeadings) {
            for (PlotPageColumnHeading heading : diagnosticHeadings) {
              result.append(exportOption.getSeparator());

              PlotPageTableValue value = data.getColumnValue(rowId,
                heading.getId());
              addValueToOutput(result, exportOption, heading.getId(), value,
                true, false);
            }
          }
        }

        result.append('\n');
        result.addRecord();
      }
    }

    // Destroy the ExportData object so it cleans up its resources
    data.destroy();

    return result;
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
    ExportData data, ExportOption exportOption, DatasetExport export)
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

  private static void addValueToOutput(DatasetExport export,
    ExportOption exportOption, long columnId, PlotPageTableValue value,
    boolean includeQcColumns, boolean includeType) {

    if (null == value) {

      // Value
      export.append(exportOption.getMissingValue());

      // QC Flag
      if (columnId != FileDefinition.TIME_COLUMN_ID && includeQcColumns) {
        export.append(exportOption.getSeparator());
        export.append(exportOption.getMissingQcFlag());

        // QC Comment if required
        if (exportOption.includeQCComments()) {
          export.append(exportOption.getSeparator());
        }
      }

      // Type
      if (includeType) {
        export.append(exportOption.getSeparator());
        export.append(PlotPageTableValue.NAN_TYPE);
      }
    } else {
      export.registerValue(columnId, value);

      // Replacing FLUSHING values with empty
      if (value.getQcFlag().equals(Flag.FLUSHING)) {
        // Empty columns
        export.append(exportOption.getMissingValue());

        if (includeQcColumns) {
          export.append(exportOption.getSeparator());

          if (exportOption.includeQCComments()) {
            export.append(exportOption.getSeparator());
          }
        }

        if (includeType) {
          export.append(exportOption.getSeparator()); // Type
        }
      } else {

        // Value
        if (null == value.getValue()) {
          export.append(exportOption.getMissingValue());
        } else {
          export.append(exportOption.format(value.getValue()));
        }

        // QC Flag
        if (columnId != FileDefinition.TIME_COLUMN_ID && includeQcColumns) {
          export.append(exportOption.getSeparator());

          // If the value is NULL, the QC flag is empty. So only put in the flag
          // if it's not null.
          if (null != value.getValue()) {
            export.append(value.getQcFlag().getWoceValue());
          }

          // QC Comment
          if (exportOption.includeQCComments()) {
            export.append(exportOption.getSeparator());
            // If the value is NULL, the QC flag is empty. So only put in the
            // flag if it's not null.
            if (null != value.getValue()) {
              export.append(
                '"' + exportOption.format(value.getQcMessage(true)) + '"');
            }
          }
        }

        if (includeType) {
          export.append(exportOption.getSeparator());
          export.append(value.getType());
        }
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
    } else if (heading.getCodeName()
      .equals(FileDefinition.TIME_COLUMN_HEADING.getCodeName())) {
      if (null != exportOption.getTimestampHeader()) {
        header = exportOption.getTimestampHeader();
      } else {
        header = heading.getShortName();
      }
    } else {
      String replacementHeader = exportOption
        .getReplacementHeader(heading.getCodeName());
      if (null != replacementHeader) {
        header = replacementHeader;
      } else {
        header = getModeHeader(heading, exportOption.getHeaderMode());
      }
    }

    // Strip out any instances of the column separator from the header
    header = header.replaceAll(exportOption.getSeparator(), "");

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
   * Create the contents of the manifest.json file
   *
   * @return The manifest JSON
   */
  private static JsonObject makeManifest(Connection conn, DataSet dataset,
    List<DataFile> rawFiles) throws Exception {

    JsonObject result = new JsonObject();

    JsonObject manifest = new JsonObject();
    JsonArray raw = new JsonArray();
    for (DataFile file : rawFiles) {
      JsonObject fileJson = new JsonObject();
      fileJson.addProperty("filename", file.getFilename());
      fileJson.addProperty("startDate",
        DateTimeUtils.toIsoDate(file.getRawStartTime()));
      fileJson.addProperty("endDate",
        DateTimeUtils.toIsoDate(file.getRawEndTime()));
      fileJson.addProperty("timeOffset", file.getTimeOffset());
      raw.add(fileJson);
    }
    manifest.add("raw", raw);

    JsonObject metadata = DataSetDB.getMetadataJson(conn, dataset);

    metadata.addProperty("quince_information",
      "Data processed using QuinCe " + dataset.getProcessingVersion());
    manifest.add("metadata", metadata);

    result.add("manifest", manifest);
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
    DataSet dataset, Collection<ExportOption> exportOptions) throws Exception {

    // Get the list of raw files
    List<Long> rawIds = dataset.getSourceFiles(conn);
    List<DataFile> files = DataFileDB.getDataFiles(conn,
      ResourceManager.getInstance().getConfig(), rawIds);

    // Get the base manifest. We will add to it as we go.
    JsonObject manifest = makeManifest(conn, dataset, files);

    /*
     * Create the dataset array, which contains details of each export format.
     * We fill this in as we go along
     */
    JsonObject exportFilesJson = new JsonObject();

    String dirRoot = dataset.getName();

    ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
    ZipOutputStream zip = new ZipOutputStream(zipOut);

    for (ExportOption option : null != exportOptions ? exportOptions
      : ExportConfig.getInstance().getOptions()) {

      // Add the main dataset file
      String datasetPath = dirRoot + "/dataset/" + option.getName() + "/"
        + dataset.getName() + option.getFileExtension();

      DatasetExport export = getDatasetExport(instrument, dataset, option);

      ZipEntry datasetEntry = new ZipEntry(datasetPath);
      zip.putNextEntry(datasetEntry);
      zip.write(export.getContent());
      zip.closeEntry();

      // Add the details to the manifest
      JsonObject datasetObject = new JsonObject();
      datasetObject.addProperty("filename",
        dataset.getName() + option.getFileExtension());
      datasetObject.addProperty("records", export.getRecordCount());
      datasetObject.addProperty("validStartDate",
        DateTimeUtils.toIsoDate(export.getStartDate()));
      datasetObject.addProperty("validEndDate",
        DateTimeUtils.toIsoDate(export.getEndDate()));

      if (instrument.fixedPosition()) {
        datasetObject.add("validBounds", makeFixedBoundsJson(instrument));
      } else {
        datasetObject.add("validBounds", export.getBoundsJson());
      }

      exportFilesJson.add(option.getName(), datasetObject);
    }

    // Add the dataset details to the manifest
    manifest.getAsJsonObject("manifest").add("exportFiles", exportFilesJson);

    for (DataFile file : files) {
      String filePath = dirRoot + "/raw/" + file.getFilename();

      ZipEntry rawEntry = new ZipEntry(filePath);
      zip.putNextEntry(rawEntry);
      zip.write(file.getBytes());
      zip.closeEntry();
    }

    // Manifest
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

  private static JsonObject makeFixedBoundsJson(Instrument instrument) {
    JsonObject boundsObject = new JsonObject();
    boundsObject.addProperty("south",
      Double.parseDouble(instrument.getProperty("latitude")));
    boundsObject.addProperty("west",
      Double.parseDouble(instrument.getProperty("longitude")));
    boundsObject.addProperty("east",
      Double.parseDouble(instrument.getProperty("longitude")));
    boundsObject.addProperty("north",
      Double.parseDouble(instrument.getProperty("latitude")));

    return boundsObject;
  }

  private static long columnsWithId(List<PlotPageColumnHeading> columns,
    long id) {
    return columns.stream().filter(c -> c.getId() == id).count();
  }
}
