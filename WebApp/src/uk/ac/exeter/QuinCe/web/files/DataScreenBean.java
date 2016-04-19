package uk.ac.exeter.QuinCe.web.files;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

public class DataScreenBean extends BaseManagedBean {

	static {
		FORM_NAME = "dataScreen";
	}

	public static final String CURRENT_FILE_SESSION_ATTRIBUTE = "currentFile";
	
	public static final String PAGE_START = "data_screen";
	
	public static final String PAGE_END = "file_list";
	
	private long fileId;
	
	/**
	 * Required basic constructor. All the actual construction
	 * is done in init().
	 */
	public DataScreenBean() {
		// Do nothing
	}

	public String start() {
		clearData();
		System.out.println("Data Screen for " + fileId);
		return PAGE_START;
	}
	
	public String end() {
		clearData();
		return PAGE_END;
	}
	
	private void clearData() {
		
	}
	
	public long getFileId() {
		return fileId;
	}
	
	public void setFileId(long fileId) {
		this.fileId = fileId;
	}
}
