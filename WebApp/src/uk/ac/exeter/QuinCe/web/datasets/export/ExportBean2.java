package uk.ac.exeter.QuinCe.web.datasets.export;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@ManagedBean
@SessionScoped
public class ExportBean2 extends BaseManagedBean {

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

    Export2Data data = exportOption.makeExportData(dataSource, instrument,
      dataset);
    data.loadData();

    // Run the post-processor before generating the final output
    data.postProcess();

    // Work out which columns we are going to export
    List<PlotPageColumnHeading> exportColumns = buildExportColumns(data,
      instrument, exportOption);

    // Let's make some output
    StringBuilder output = new StringBuilder();

    List<String> headers = new ArrayList<String>();

    for (PlotPageColumnHeading heading : exportColumns) {
      if (heading.getId() == FileDefinition.TIME_COLUMN_ID) {
        headers.add(exportOption.getTimestampHeader());
      } else {
        addHeader(headers, exportOption, heading);
      }
    }

    for (InstrumentVariable variable : exportOption.getVariables()) {
      if (instrument.getVariables().contains(variable)) {

        List<CalculationParameter> params = DataReducerFactory
          .getCalculationParameters(variable,
            exportOption.includeCalculationColumns());

        for (CalculationParameter param : params) {
          addHeader(headers, exportOption, param);
        }
      }
    }

    output.append(
      StringUtils.collectionToDelimited(headers, exportOption.getSeparator()));
    output.append('\n');

    // Process each row of the data
    for (Long rowId : data.getRowIDs()) {

      boolean firstColumn = true;

      // Sensor columns
      for (PlotPageColumnHeading column : exportColumns) {

        // Separator management. Add a separator before the column details,
        // unless we're on the first column
        if (firstColumn) {
          firstColumn = false;
        } else {
          output.append(exportOption.getSeparator());
        }

        PlotPageTableValue value = data.getColumnValue(rowId, column.getId());
        addValueToOutput(output, exportOption, column.getId(), value,
          column.getId() != FileDefinition.TIME_COLUMN_ID);
      }

      for (InstrumentVariable variable : exportOption.getVariables()) {
        if (instrument.getVariables().contains(variable)) {

          List<CalculationParameter> params = DataReducerFactory
            .getCalculationParameters(variable,
              exportOption.includeCalculationColumns());

          for (CalculationParameter param : params) {
            // Separator management. Add a separator before the column details,
            // unless we're on the first column
            if (firstColumn) {
              firstColumn = false;
            } else {
              output.append(exportOption.getSeparator());
            }

            PlotPageTableValue value = data.getColumnValue(rowId,
              param.getId());
            addValueToOutput(output, exportOption, param.getId(), value,
              param.isResult());
          }
        }
      }

      output.append("\n");
    }

    return output.toString().getBytes();
  }

  private static void addValueToOutput(StringBuilder output,
    ExportOption exportOption, long columnId, PlotPageTableValue value,
    boolean includeQcColumns) {

    if (null == value) {
      // Value
      output.append(exportOption.getMissingValue());
      output.append(exportOption.getSeparator());

      // QC Flag
      if (includeQcColumns) {
        output.append(Flag.VALUE_NO_QC);

        // QC Comment if required
        if (exportOption.includeQCComments()) {
          output.append(exportOption.getSeparator());
        }
      }
    } else {

      // Value
      if (null == value.getValue()) {
        output.append(exportOption.getMissingValue());
      } else {
        output.append(exportOption.format(value.getValue()));
      }

      // QC Flag
      if (columnId != FileDefinition.TIME_COLUMN_ID) {
        output.append(exportOption.getSeparator());
        output.append(value.getQcFlag().getWoceValue());

        // QC Comment
        if (exportOption.includeQCComments()) {
          output.append(exportOption.getSeparator());
          output.append(exportOption.format(value.getQcMessage()));
        }
      }
    }
  }

  private static void addHeader(List<String> headers, ExportOption exportOption,
    ColumnHeading heading) throws ExportException {

    String header = null;

    String replacementHeader = exportOption
      .getReplacementHeader(heading.getCodeName());
    if (null != replacementHeader) {
      header = replacementHeader;
    } else {
      switch (exportOption.getHeaderMode()) {
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

      if (exportOption.includeUnits()) {
        headers.add(header + " [" + heading.getUnits() + ']');
      } else {
        headers.add(header);
      }
    }

    if (heading.hasQC()) {
      headers.add(header + exportOption.getQcFlagSuffix());

      if (exportOption.includeQCComments()) {
        headers.add(header + exportOption.getQcCommentSuffix());
      }
    }
  }

  private static List<PlotPageColumnHeading> buildExportColumns(
    Export2Data data, Instrument instrument, ExportOption exportOption)
    throws Exception {

    List<PlotPageColumnHeading> exportColumns = new ArrayList<PlotPageColumnHeading>();

    LinkedHashMap<String, List<PlotPageColumnHeading>> dataHeadings = data
      .getExtendedColumnHeadings();

    // Add the base columns - time, position etc
    exportColumns.addAll(dataHeadings.get(Export2Data.ROOT_FIELD_GROUP));

    List<PlotPageColumnHeading> sensorColumns = dataHeadings
      .get(Export2Data.SENSORS_FIELD_GROUP);
    if (exportOption.includeAllSensors()) {
      exportColumns.addAll(sensorColumns);
    } else {

      Set<SensorType> variableSensorTypes = new HashSet<SensorType>();

      // Get the sensors required for the instrument's variables
      for (InstrumentVariable variable : instrument.getVariables()) {
        variableSensorTypes.addAll(variable.getAllSensorTypes(false));
      }

      // Find those columns with the required sensor types
      for (PlotPageColumnHeading sensorHeading : sensorColumns) {
        SensorType headingSensorType = instrument.getSensorAssignments()
          .getSensorTypeForDBColumn(sensorHeading.getId());
        if (variableSensorTypes.contains(headingSensorType)) {
          exportColumns.add(sensorHeading);
        }
      }
    }

    return exportColumns;
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
}
