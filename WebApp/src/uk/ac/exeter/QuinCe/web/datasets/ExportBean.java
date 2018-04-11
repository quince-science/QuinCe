package uk.ac.exeter.QuinCe.web.datasets;

import java.io.OutputStream;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Export.ExportConfig;
import uk.ac.exeter.QuinCe.data.Export.ExportException;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
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

      //byte[] fileContent = DataSetDataDB.getCSVData(getDataSource(), getAppConfig(), dataset, exportOption).getBytes();
      byte[] fileContent = "I am an exported file".getBytes();

      FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        ec.responseReset();
        ec.setResponseContentType("text/csv");
        ec.setResponseContentLength(fileContent.length); // Set it with the file size. This header is optional. It will work if it's omitted, but the download progress will be unknown.
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + getExportFilename(exportOption) + "\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.

        OutputStream output = ec.getResponseOutputStream();
        output.write(fileContent);

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
