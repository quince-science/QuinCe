package uk.ac.exeter.QuinCe.web.files;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;

import org.primefaces.model.UploadedFile;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;

/**
 * @author Jonas F. Henriksen
 *
 */
public class UploadedFileExtended {
	private UploadedFile uploadedFile;
	private int rowCount = 0;
	private boolean store = true;
	private DataFile dataFile = null;
	public UploadedFileExtended(UploadedFile file) {
		setUploadedFile(file);
	}
	/**
	 * @return the uploadedFile
	 */
	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}
	/**
	 * @param uploadedFile the uploadedFile to set
	 */
	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
		String[] fileLines = getLines();
		rowCount = fileLines.length;
		while ("".equals(fileLines[rowCount - 1])) {
			rowCount--;
		}
	}
	public String[] getLines() {
		String fileContent = new String(uploadedFile.getContents(), StandardCharsets.UTF_8);
		return fileContent.split("[\\r\\n]+");
	}
	/**
	 * Get number of rows in the file
	 * @return the rowCount
	 */
	public int getRowCount() {
		return rowCount;
	}

	/**
	 * @param rowCount the rowCount to set
	 */
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	/**
	 * @return the base file name
	 */
	public String getName() {
		return uploadedFile.getFileName();
	}

	/**
	 * @return indication whether this file should be stored in the database
	 */
	public boolean isStore() {
		return store;
	}

	/**
	 * @param store says whether this file should be stored to the database
	 */
	public void setStore(boolean store) {
		this.store = store;
	}

	/**
	 * @return the dataFile
	 */
	public DataFile getDataFile() {
		return dataFile;
	}

	/**
	 * @param dataFile the dataFile to set
	 */
	public void setDataFile(DataFile dataFile) {
		this.dataFile = dataFile;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return null == dataFile ? null : Date.from(dataFile.getStartDate().atZone(ZoneId.of("UTC")).toInstant());
	}

	/**
	 * @return the endDate
	 * @throws DataFileException
	 */
	public Date getEndDate() throws DataFileException {
		return null == dataFile ? null : Date.from(dataFile.getEndDate().atZone(ZoneId.of("UTC")).toInstant());
	}
}
