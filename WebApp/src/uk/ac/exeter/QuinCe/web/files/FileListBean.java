package uk.ac.exeter.QuinCe.web.files;

import java.io.OutputStream;
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
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.database.files.FileDataInterrogator;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for the main file list
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
	
	public static final String PAGE_EXPORT = "export";
	
	/**
	 * The list of the user's files. Updated whenever
	 * getFileList is called
	 */
	private List<FileInfo> fileList;
	
	private long chosenFile;
	
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
	
	public String export() {
		return PAGE_EXPORT;
	}
	
	public String goToFileList() {
		return PAGE_FILE_LIST;
	}
	
	public String getChosenFileName() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		return DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), chosenFile).getFileName();
	}
	
	public List<ExportOption> getExportOptions() throws ExportException {
		return ExportConfig.getInstance().getOptions();
	}
	
	public int getChosenExportOption() {
		return chosenExportOption;
	}
	
	public void setChosenExportOption(int chosenExportOption) {
		this.chosenExportOption = chosenExportOption;
	}
	
	public void exportFile() throws Exception {

		DataSource dataSource = ServletUtils.getDBDataSource();
		
		Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, chosenFile);
		
		String fileContent = FileDataInterrogator.getCSVData(dataSource, ServletUtils.getAppConfig(), chosenFile, instrument, getExportOptions().get(chosenExportOption));
				
		FacesContext fc = FacesContext.getCurrentInstance();
	    ExternalContext ec = fc.getExternalContext();

	    ec.responseReset();
	    ec.setResponseContentType("text/csv");
	    ec.setResponseContentLength(fileContent.length()); // Set it with the file size. This header is optional. It will work if it's omitted, but the download progress will be unknown.
	    ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + getChosenFileName() + "\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.

	    OutputStream output = ec.getResponseOutputStream();
	    output.write(fileContent.getBytes());

	    fc.responseComplete();
	}
}
