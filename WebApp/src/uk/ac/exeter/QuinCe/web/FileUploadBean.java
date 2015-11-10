package uk.ac.exeter.QuinCe.web;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * Extended version of the {@link BaseManagedBean} that includes
 * file upload handling.
 * 
 * @author Steve Jones
 *
 */
public abstract class FileUploadBean extends BaseManagedBean {
	
	/**
	 * The uploaded file
	 */
	protected UploadedFile file = null;

	/**
	 * Handle the file upload and subsequent processing.
	 * @param event The file upload event
	 */
	public abstract void handleFileUpload(FileUploadEvent event);
	
	/**
	 * Retrieve the uploaded file
	 * @return The uploaded file
	 */
	public UploadedFile getFile() {
		return file;
	}
	
	/**
	 * Set the uploaded file
	 * @param file The uploaded file
	 */
    public void setFile(UploadedFile file) {
        this.file = file;
    }
}
