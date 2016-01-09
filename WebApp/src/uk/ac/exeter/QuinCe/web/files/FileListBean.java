package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
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
	 * Navigates to the file upload page
	 * @return The navigation string
	 */
	public String uploadFile() {
		return PAGE_UPLOAD_FILE;
	}
	
	public List<FileInfo> getFileList() {
		List<FileInfo> files = null;
		
		try {
			files = DataFileDB.getUserFiles(ServletUtils.getDBDataSource(), getUser());
		} catch (Exception e) {
			// Do nothing
		}
		
		return files;
	}
	
}
