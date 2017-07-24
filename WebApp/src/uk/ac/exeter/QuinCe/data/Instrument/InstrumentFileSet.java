package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;

/**
 * Methods to handle and process a set of file definitions for an instrument
 * @author Steve Jones
 *
 */
public class InstrumentFileSet extends ArrayList<FileDefinition> {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -998081927701592751L;

	/**
	 * Simple constructor to create an empty set
	 */
	protected InstrumentFileSet() {
		super();
	}
	
	@Override
	public boolean add(FileDefinition file) {
		boolean result = false;
		
		int currentPosition = indexOf(file);
		
		if (currentPosition == -1) {
			result = super.add(file);
		} else {
			set(currentPosition, file);
			result = true;
		}
		
		// Add the passed in file
		return result;
	}
	
	/**
	 * Determine whether or not the file set contains a file definition
	 * with the specified description
	 * @param description The file description
	 * @return {@code true} if a file with the specified description is present; {@code false} otherwise
	 */
	public boolean containsFileDescription(String description) {
		boolean found = false;
		
		for (FileDefinition file : this) {
			if (file.getFileDescription().equalsIgnoreCase(description)) {
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	/**
	 * Retrieve the file definition with the specified description.
	 * If no definition is found, the method returns {@code null}
	 * @param description The file description
	 * @return The file definition
	 */
	public FileDefinition get(String description) {
		FileDefinition result = null;
		
		for (FileDefinition file : this) {
			if (file.getFileDescription().equalsIgnoreCase(description)) {
				result = file;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Set the file that contains the primary position data for the instrument
	 * @param fileDescription The file description of the file
	 * @throws FileSetException If a file with the specified file description does not exist
	 */
	public void setPrimaryPositionFile(String fileDescription) throws FileSetException {

		String fileToSet;
		
		if (null == fileDescription) {
			fileToSet = null;
		} else if (fileDescription.trim().length() == 0) {
			fileToSet = null;
		} else {
			fileToSet = fileDescription.trim();
		}
		
		if (null != fileToSet && !containsFileDescription(fileDescription)) {
			throw new FileSetException("The file '" + fileDescription + "' does not exist");
		}
		
		if (null == fileToSet) {
			for (int i = 0; i < size(); i++) {
				get(i).positionPrimary = false;
			}
		} else {
			for (int i = 0; i < size(); i++) {
				FileDefinition file = get(i);
				if (file.getFileDescription().equalsIgnoreCase(fileToSet)) {
					file.positionPrimary = true;
				} else {
					file.positionPrimary = false;
				}
			}
		}
	}
	
	/**
	 * Get the description of the file that contains the primary position
	 * data for the instrument
	 * @return The file definition
	 */
	public String getPrimaryPositionFile() {
		String primaryPositionFile = null;
		
		for (int i = 0; i < size(); i++) {
			FileDefinition file = get(i);
			
			if (file.positionPrimary) {
				primaryPositionFile = file.getFileDescription();
				break;
			}
		}
		
		return primaryPositionFile;
	}
	
	@Override
	public boolean remove(Object o) {
		boolean removed = false;
		
		if (o instanceof FileDefinition) {
			removed = super.remove(o);
		} else if (o instanceof String) {
			int fileToRemove = -1;
			
			for (int i = 0; i < size(); i++) {
				if (get(i).getFileDescription().equalsIgnoreCase((String) o)) {
					fileToRemove = i;
					break;
				}
			}
			
			remove(fileToRemove);
		}
		
		return removed;
	}
}
