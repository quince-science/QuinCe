package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import uk.ac.exeter.QuinCe.data.ExportConfig;
import uk.ac.exeter.QuinCe.data.ExportException;
import uk.ac.exeter.QuinCe.data.ExportOption;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
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
	 * The ID of the chosen file
	 */
	private long chosenFile;
	
	/**
	 * The list of the user's files. Updated whenever
	 * getFileList is called
	 */
	private List<FileInfo> fileList;
	
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
			DataFileDB.deleteFile(ServletUtils.getDBDataSource(), ServletUtils.getAppConfig(), getChosenFileDetails());
		} catch (Exception e) {
			return internalError(e);
		}

		return PAGE_FILE_LIST;
	}
	
	private FileInfo getChosenFileDetails() {
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
	
	public String exportFile() {
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
}
