package uk.ac.exeter.QuinCe.web.files;

import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.ExportConfig;
import uk.ac.exeter.QuinCe.data.ExportException;
import uk.ac.exeter.QuinCe.data.ExportOption;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.database.files.FileDataInterrogator;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Backing bean for the data file list
 * @author Steve Jones
 *
 */
public class FileListBean extends BaseManagedBean {
	
	/**
	 * Navigation to the file upload page
	 */
	public static final String PAGE_UPLOAD_FILE = "upload_file";
	
	/**
	 * The navigation to the file list
	 */
	public static final String PAGE_FILE_LIST = "file_list";
	
	/**
	 * The navigation to the file export page
	 */
	public static final String PAGE_EXPORT = "export";
	
	/**
	 * The list of the user's files. Updated whenever
	 * getFileList is called
	 */
	private List<FileInfo> fileList;
	
	/**
	 * The database ID of the selected data file
	 */
	private long chosenFile;
	
	/**
	 * The index of the selected file export option
	 */
	private int chosenExportOption;
	
	/**
	 * Navigates to the file upload page
	 * @return The navigation string
	 */
	public String uploadFile() {
		return PAGE_UPLOAD_FILE;
	}
	
	/**
	 * Get the list of files for the user
	 * @return The user's files
	 */
	public List<FileInfo> getFileList() {
		
		try {
			fileList = DataFileDB.getUserFiles(ServletUtils.getDBDataSource(), getUser());
		} catch (Exception e) {
			// Do nothing
		}
		
		return fileList;
	}
	
	/**
	 * Delete a file
	 * @return The navigation to the file list page
	 */
	public String deleteFile() {
		try {
			DataFileDB.deleteFile(ServletUtils.getDBDataSource(), ServletUtils.getAppConfig(), getCurrentFileDetails());
		} catch (Exception e) {
			return internalError(e);
		}

		return PAGE_FILE_LIST;
	}
	
	/**
	 * Trigger reprocessing of a data file, from data reduction onwards.
	 * @return The navigation back to the file list page
	 */
	public String reprocessFile() {
		
		Connection conn = null;

		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			conn = dataSource.getConnection();
			
			List<String> params = new ArrayList<String>(1);
			params.add(String.valueOf(chosenFile));
			
			JobManager.addJob(conn, getUser(), FileInfo.JOB_CLASS_REDUCTION, params);
			DataFileDB.setCurrentJob(conn, chosenFile, FileInfo.JOB_CODE_REDUCTION);
		} catch (Exception e) {
			return internalError(e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
		
		return PAGE_FILE_LIST;
	}
	
	/**
	 * Load the details of the selected file
	 * @return The file information
	 */
	private FileInfo getCurrentFileDetails() {
		FileInfo result = null;
		
		for (FileInfo info : fileList) {
			if (info.getFileId() == chosenFile) {
				result = info;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Get the ID of the chosen file
	 * @return The file ID
	 */
	public long getChosenFile() {
		return chosenFile;
	}
	
	/**
	 * Set the ID of the chosen file
	 * @param chosenFile The file ID
	 */
	public void setChosenFile(long chosenFile) {
		this.chosenFile = chosenFile;
	}
	
	/**
	 * Navigates to the file export page
	 * @return The navigation to the export page
	 */
	public String export() {
		return PAGE_EXPORT;
	}
	
	/**
	 * Navigates to the main file list
	 * @return The navigation to the file list
	 */
	public String goToFileList() {
		return PAGE_FILE_LIST;
	}
	
	/**
	 * Gets the filename of the chosen data file
	 * @return The filename
	 * @throws MissingParamException If any parameters on internal calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the file does not exist in the database
	 * @throws ResourceException If an error occurs in any application resources
	 */
	public String getChosenFileName() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		return DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), chosenFile).getFileName();
	}
	
	/**
	 * Get the name of the instrument to which the selected file belongs
	 * @return The instrument name
	 * @throws MissingParamException If any parameters on internal calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the file does not exist in the database
	 * @throws ResourceException If an error occurs in any application resources
	 */
	public String getChosenFileInstrumentName() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		return InstrumentDB.getInstrumentByFileId(ServletUtils.getDBDataSource(), chosenFile).getName();
	}

	/**
	 * Get the list of available file export options
	 * @return The export options
	 * @throws ExportException In an error occurs while retrieving the export options
	 */
	public List<ExportOption> getExportOptions() throws ExportException {
		return ExportConfig.getInstance().getOptions();
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
	 * Export the selected data file using the chosen export option
	 * @throws Exception If any errors occur
	 * @see #chosenFile
	 * @see #chosenExportOption
	 */
	public void exportFile() throws Exception {

		DataSource dataSource = ServletUtils.getDBDataSource();
		Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, chosenFile);
		ExportOption exportOption = getExportOptions().get(chosenExportOption);
		
		byte[] fileContent = FileDataInterrogator.getCSVData(dataSource, ServletUtils.getAppConfig(), chosenFile, instrument, exportOption).getBytes();
				
		FacesContext fc = FacesContext.getCurrentInstance();
	    ExternalContext ec = fc.getExternalContext();

	    ec.responseReset();
	    ec.setResponseContentType("text/csv");
	    ec.setResponseContentLength(fileContent.length); // Set it with the file size. This header is optional. It will work if it's omitted, but the download progress will be unknown.
	    ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + getExportFilename(exportOption) + "\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.

	    OutputStream output = ec.getResponseOutputStream();
	    output.write(fileContent);

	    fc.responseComplete();
	}
	
	/**
	 * Get the filename of the file that will be exported
	 * @param exportOption The export option
	 * @return The export filename
	 * @throws Exception If any errors occur
	 */
	private String getExportFilename(ExportOption exportOption) throws Exception {
		StringBuffer fileName = new StringBuffer(getChosenFileName().replaceAll("\\.", "_"));
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
