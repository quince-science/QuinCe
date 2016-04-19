package uk.ac.exeter.QuinCe.web.files;

import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class DataScreenBean extends BaseManagedBean {

	static {
		FORM_NAME = "dataScreen";
	}

	public static final String CURRENT_FILE_SESSION_ATTRIBUTE = "currentFile";
	
	public static final String PAGE_START = "data_screen";
	
	public static final String PAGE_END = "file_list";
	
	private long fileId;
	
	private FileInfo fileDetails;
	
	/**
	 * Required basic constructor. All the actual construction
	 * is done in init().
	 */
	public DataScreenBean() {
		// Do nothing
	}

	public String start() throws Exception {
		clearData();
		loadFileDetails();
		return PAGE_START;
	}
	
	public String end() {
		clearData();
		return PAGE_END;
	}
	
	private void clearData() {
		fileDetails = null;
	}
	
	public long getFileId() {
		return fileId;
	}
	
	public void setFileId(long fileId) {
		this.fileId = fileId;
	}
	
	public FileInfo getFileDetails() {
		return fileDetails;
	}
	
	private void loadFileDetails() throws MissingParamException, DatabaseException, ResourceException {
		fileDetails = DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), fileId);
	}
}
