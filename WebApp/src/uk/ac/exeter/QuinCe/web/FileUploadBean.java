package uk.ac.exeter.QuinCe.web;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * Extended version of the {@link BaseManagedBean} that includes
 * file upload handling.
 * 
 * @author Steve Jones
 * @see BaseManagedBean
 */
public abstract class FileUploadBean extends BaseManagedBean {
	
	/**
	 * The uploaded file
	 */
	protected UploadedFile file = null;
	
	/**
	 * The contents of the uploaded file as a list of strings
	 * @see #extractFileLines()
	 */
	protected List<String> fileLines = null;

	/**
	 * Handle the file upload and subsequent processing.
	 * @param event The file upload event
	 */
	public final void handleFileUpload(FileUploadEvent event) {
		setFile(event.getFile());
		processUploadedFile();
	}
	
	/**
	 * Process the uploaded file
	 */
	public abstract void processUploadedFile();
	
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
    
    /**
     * Remove any existing uploaded file
     */
    public void clearFile() {
    	this.file = null;
    }
    
    /**
     * Extract the contents of the uploaded file as a list of strings
     */
    protected void extractFileLines() {
		String fileContent = new String(getFile().getContents(), StandardCharsets.UTF_8);
		fileLines = Arrays.asList(fileContent.split("[\\r\\n]+"));
		
		// Remove empty lines at the end of the file
		boolean blankLine = true;
		while (blankLine) {
			String lastLine = fileLines.get(fileLines.size() - 1);
			if (lastLine.trim().length() == 0) {
				fileLines.remove(fileLines.size() - 1);
			} else {
				blankLine = false;
			}
		}
    }
    
    /**
     * Get the name of the uploaded file
     * @return The filename
     */
    public String getFilename() {
    	String result = null;
    	
    	if (null != file) {
    		result = file.getFileName();
    	}
    	
    	return result;
    }
}
