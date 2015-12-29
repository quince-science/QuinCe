package uk.ac.exeter.QuinCe.web.files;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for the main file list
 * @author Steve Jones
 *
 */
public class FileListBean extends BaseManagedBean {
	
	public static final String PAGE_UPLOAD_FILE = "upload_file";
	
	public String uploadFile() {
		return PAGE_UPLOAD_FILE;
	}
}
