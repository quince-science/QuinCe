package uk.ac.exeter.QuinCe.web.User;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

public class UserProfileBean extends BaseManagedBean {

	/**
	 * The navigation string for the file list page
	 */
	private static final String PAGE_FILE_LIST = "file_list";
	
	/**
	 * The user's current password - required to make changes
	 */
	private String currentPassword = null;
	
	/**
	 * The user's new password
	 */
	private String newPassword1 = null;
	
	/**
	 * Confirmation of the user's new password
	 */
	private String newPassword2 = null;
	
	/////////// *** METHODS **** ///////////////
	
	/**
	 * Cancels editing of the user profile
	 * @return The navigation to the file list page
	 */
	public String cancelEdit() {
		return PAGE_FILE_LIST;
	}
	
	////////// *** GETTERS AND SETTERS *** /////////
	
	/**
	 * Returns the user's current password
	 * @return The user's current password
	 */
	public String getCurrentPassword() {
		return currentPassword;
	}
	
	/**
	 * Sets the user's current password
	 * @param currentPassword The user's current password
	 */
	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}
	
	/**
	 * Returns the user's first new password entry
	 * @return The user's first new password entry
	 */
	public String getNewPassword1() {
		return newPassword1;
	}
	
	/**
	 * Sets the user's first new password entry
	 * @param newPassword1 The user's first new password entry
	 */
	public void setNewPassword1(String newPassword1) {
		this.newPassword1 = newPassword1;
	}
	
	/**
	 * Returns the user's second new password entry
	 * @return The user's second new password entry
	 */
	public String getNewPassword2() {
		return newPassword2;
	}
	
	/**
	 * Sets the user's second new password entry
	 * @param newPassword2 The user's second new password entry
	 */
	public void getNewPassword2(String newPassword2) {
		this.newPassword2 = newPassword2;
	}
}
