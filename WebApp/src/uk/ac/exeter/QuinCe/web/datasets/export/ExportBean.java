package uk.ac.exeter.QuinCe.web.datasets.export;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.sql.DataSource;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Export.ExportConfig;
import uk.ac.exeter.QuinCe.data.Export.ExportException;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSet;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@ManagedBean
@SessionScoped
public class ExportBean extends BaseManagedBean {

  /**
   * Navigation to the export page
   */
  private static final String NAV_EXPORT_PAGE = "export";

  /**
   * The database ID of the dataset to be exported
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
   * Formatter for numeric values All values are displayed to 3 decimal places.
   */
  private static DecimalFormat numberFormatter;

  /**
   * The export data, organised ready for building export files
   */
  private ExportData exportData = null;

  static {
    numberFormatter = new DecimalFormat("#0.000");
    numberFormatter.setRoundingMode(RoundingMode.HALF_UP);
  }

  /**
   * Initialise the bean
   */
  public String start() {
    // Reset the export data
    exportData = null;

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

    Connection conn = null;

    try {
      conn = getDataSource().getConnection();
      ExportOption exportOption = getExportOptions().get(chosenExportOption);

      byte[] fileContent = getDatasetExport(conn, getCurrentInstrument(),
        dataset, exportOption);

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
    } finally {
      DatabaseUtils.closeConnection(conn);
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
  private static byte[] getDatasetExport(Connection conn, Instrument instrument,
    DataSet dataset, ExportOption exportOption) throws Exception {

    DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

    ExportData data = new ExportData(dataSource, instrument, dataset,
      exportOption);

    data
      .addTimes(DataSetDataDB.getSensorValueDates(dataSource, dataset.getId()));

    DataSetDataDB.loadMeasurementData(dataSource, data, data.getRowIds());

    // Let's make some output
    StringBuilder output = new StringBuilder();

    // Headers
    List<String> headers = new ArrayList<String>();
    headers.add(exportOption.getTimestampHeader());

    List<ExportField> exportFields = new ArrayList<ExportField>();

    for (FieldSet fieldSet : data.getFieldSets().keySet()) {

      if (fieldSet.getId() <= 0
        || exportOption.getVariables().contains(fieldSet.getId())) {
        for (Field field : data.getFieldSets().get(fieldSet)) {
          ExportField exportField = (ExportField) field;
          if (!exportField.isDiagnostic()) {
            exportFields.add(exportField);
            String header = exportField.getName();
            headers.add(header);
            if (exportField.hasQC()) {
              headers.add(header + exportOption.getQcFlagSuffix());
              if (exportOption.includeQCComments()) {
                headers.add(header + exportOption.getQcCommentSuffix());
              }
            }
          }
        }
      }
    }

    output.append(
      StringUtils.collectionToDelimited(headers, exportOption.getSeparator()));
    output.append('\n');

    for (LocalDateTime rowId : data.keySet()) {

      output.append(DateTimeUtils.toIsoDate(rowId));

      for (Map.Entry<Field, FieldValue> entry : data.get(rowId).entrySet()) {
        ExportField field = (ExportField) entry.getKey();
        if (!field.isDiagnostic()) {
          if (exportFields.contains(field)) {
            FieldValue fieldValue = entry.getValue();

            output.append(exportOption.getSeparator());
            if (null == fieldValue || fieldValue.isNaN()) {
              output.append(exportOption.getMissingValue());
            } else {
              output.append(numberFormatter.format(fieldValue.getValue()));
            }

            if (field.hasQC()) {
              output.append(exportOption.getSeparator());
              if (null == fieldValue) {
                output.append(exportOption.getMissingValue());
              } else {
                if (!dataset.isNrt() && fieldValue.needsFlag()) {
                  output.append(Flag.NEEDED.getWoceValue());
                } else {
                  output.append(fieldValue.getQcFlag().getWoceValue());
                }
              }

              if (exportOption.includeQCComments()) {
                output.append(exportOption.getSeparator());
                if (null == fieldValue) {
                  output.append("");
                } else {
                  output.append(
                    StringUtils.makeCsvString(fieldValue.getQcComment()));
                }
              }
            }
          }
        }
      }

      output.append('\n');
    }

    return output.toString().getBytes();
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
      zip.write(getDatasetExport(conn, instrument, dataset, option));
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
