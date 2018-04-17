package uk.ac.exeter.QuinCe.web.datasets;

import java.io.OutputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringEscapeUtils;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecordFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Export.ExportConfig;
import uk.ac.exeter.QuinCe.data.Export.ExportException;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

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
   * Formatter for numeric values
   * All values are displayed to 3 decimal places.
   */
  private static DecimalFormat numberFormatter;

  static {
    numberFormatter = new DecimalFormat("#.000");
    numberFormatter.setRoundingMode(RoundingMode.HALF_UP);
  }

  /**
   * Initialise the bean
   */
  public String start() {
    return NAV_EXPORT_PAGE;
  }

  /**
   * Get the dataset ID
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
   * @param datasetId The dataset ID
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
   * @return The dataset
   */
  public DataSet getDataset() {
    return dataset;
  }

  /**
   * Set the dataset
   * @param dataset The dataset
   */
  public void setDataset(DataSet dataset) {
    this.dataset = dataset;
  }

  /**
   * Get the list of available file export options
   * @return The export options
   * @throws ExportException In an error occurs while retrieving the export options
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
   * @return The export option ID
   */
  public int getChosenExportOption() {
    return chosenExportOption;
  }

  /**
   * Set the ID of the chosen export option
   * @param chosenExportOption The export option ID
   */
  public void setChosenExportOption(int chosenExportOption) {
    this.chosenExportOption = chosenExportOption;
  }

  /**
   * Export the dataset in the chosen format
   */
  public void exportDataset() {

    try {
      ExportOption exportOption = getExportOptions().get(chosenExportOption);

      // TODO This will get all sensor columns. When the sensor data storage is updated (Issue #576), this can be revised.
      List<DataSetRawDataRecord> datasetData = DataSetDataDB.getMeasurements(getDataSource(), getDataset());

      List<CalculationRecord> calculationData = new ArrayList<CalculationRecord>(datasetData.size());
      for (DataSetRawDataRecord record : datasetData) {
        CalculationRecord calcRecord = CalculationRecordFactory.makeCalculationRecord(getDatasetId(), record.getId());
        CalculationDBFactory.getCalculationDB().getCalculationValues(getDataSource(), calcRecord);
        calculationData.add(calcRecord);
      }

      StringBuilder output = new StringBuilder();

      // The header
      output.append("Date");
      output.append(exportOption.getSeparator());
      output.append("Longitude");
      output.append(exportOption.getSeparator());
      output.append("Latitude");
      output.append(exportOption.getSeparator());

      for (String sensorColumn : exportOption.getSensorColumns()) {
        output.append(sensorColumn);
        output.append(exportOption.getSeparator());
      }

      // TODO Replace when mutiple calculation paths are in place
      List<String> calculationColumns = exportOption.getCalculationColumns("equilibrator_pco2");
      for (int i = 0; i < calculationColumns.size(); i++) {
        output.append(calculationColumns.get(i));
        output.append(exportOption.getSeparator());
      }

      output.append("QC Flag");
      output.append(exportOption.getSeparator());
      output.append("QC Message");
      output.append('\n');


      for (int i = 0; i < datasetData.size(); i++) {

        DataSetRawDataRecord sensorRecord = datasetData.get(i);
        CalculationRecord calculationRecord = calculationData.get(i);

        if (exportOption.flagAllowed(calculationRecord.getUserFlag())) {

          output.append(DateTimeUtils.formatDateTime(sensorRecord.getDate()));
          output.append(exportOption.getSeparator());
          output.append(numberFormatter.format(sensorRecord.getLongitude()));
          output.append(exportOption.getSeparator());
          output.append(numberFormatter.format(sensorRecord.getLatitude()));
          output.append(exportOption.getSeparator());

          for (String sensorColumn : exportOption.getSensorColumns()) {
            Double value = sensorRecord.getSensorValue(sensorColumn);
            if (null == value) {
              output.append("NaN");
            } else {
              output.append(numberFormatter.format(value));
            }

            output.append(exportOption.getSeparator());
          }

          for (String calculatedColumn : exportOption.getCalculationColumns("equilibrator_pco2")) {
            Double value = calculationRecord.getNumericValue(calculatedColumn);
            if (null == value) {
              output.append("NaN");
            } else {
              output.append(numberFormatter.format(value));
            }

            output.append(exportOption.getSeparator());
          }

          output.append(Flag.getWoceValue(calculationRecord.getUserFlag().getFlagValue()));
          output.append(exportOption.getSeparator());

          String qcMessage = calculationRecord.getUserMessage();
          if (null != qcMessage) {
            if (qcMessage.length() > 0) {
              output.append(StringEscapeUtils.escapeCsv(qcMessage.trim()));
            }
          }

          output.append('\n');
        }

      }

      byte[] fileContent = output.toString().getBytes();

      FacesContext fc = FacesContext.getCurrentInstance();
      ExternalContext ec = fc.getExternalContext();

      ec.responseReset();
      ec.setResponseContentType("text/csv");
      ec.setResponseContentLength(fileContent.length); // Set it with the file size. This header is optional. It will work if it's omitted, but the download progress will be unknown.
      ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + getExportFilename(exportOption) + "\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.

      OutputStream outputStream = ec.getResponseOutputStream();
      outputStream.write(fileContent);

      fc.responseComplete();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the filename of the file that will be exported
   * @param exportOption The export option
   * @return The export filename
   * @throws Exception If any errors occur
   */
  private String getExportFilename(ExportOption exportOption) throws Exception {
    StringBuffer fileName = new StringBuffer(dataset.getName().replaceAll("\\.", "_"));
    fileName.append('-');
    fileName.append(exportOption.getName());

    if (exportOption.getSeparator().equals("\t")) {
      fileName.append(".tsv");
    } else {
      fileName.append(".csv");
    }

    return fileName.toString();
  }
}
