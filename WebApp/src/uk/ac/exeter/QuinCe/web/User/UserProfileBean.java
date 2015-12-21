package uk.ac.exeter.QuinCe.web.User;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

public class UserProfileBean extends BaseManagedBean {

	/**
	 * The navigation string for the file list page
	 */
	private static final String PAGE_FILE_LIST = "file_list";
	
	/**
	 * Cancels editing of the user profile
	 * @return The navigation to the file list page
	 */
	public String cancelEdit() {
		return PAGE_FILE_LIST;
	}
	
}
