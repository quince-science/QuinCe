package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.html.HtmlUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 */
public class NewInstrumentBean extends FileUploadBean {

	/**
	 * Navigation to start definition of a new instrument
	 */
	private static final String NAV_NAME = "name";
	
	/**
	 * Navigation when cancelling definition of a new instrument
	 */
	private static final String NAV_CANCEL = "cancel";
	
	/**
	 * Navigation to the Upload File page
	 */
	private static final String NAV_UPLOAD_FILE = "upload_file";
	
	/**
	 * Navigation to the Assign Variables page
	 */
	private static final String NAV_ASSIGN_VARIABLES = "assign_variables";
	
	/**
	 * The name of the new instrument
	 */
	private String instrumentName;
	
	/**
	 * The set of sample files for the instrument definition
	 */
	private InstrumentFileSet instrumentFiles;
	
	/**
	 * The assignments of sensors to data file columns
	 */
	private SensorAssignments sensorAssignments;
	
	/**
	 * The sample file that is currently being edited
	 */
	private FileDefinitionBuilder currentInstrumentFile;
	
	/**
	 * Begin a new instrument definition
	 * @return The navigation to the start page
	 */
	public String start() {
		clearAllData();
		return NAV_NAME;
	}
	
	/**
	 * Cancel the current instrument definition
	 * @return Navigation to the instrument list
	 */
	public String cancel() {
		clearAllData();
		return NAV_CANCEL;
	}
	
	/**
	 * Navigate to the Name page
	 * @return Navigation to the name page
	 */
	public String goToName() {
		clearFile();
		return NAV_NAME;
	}
	
	/**
	 * Navigate to the files step.
	 * 
	 * <p>
	 *   The page we navigate to depends on the current status of the instrument.
	 * <p>
	 * 
	 * <p>
	 *   If no files have been added, we create a new empty file and go to the upload page.
	 *   Otherwise, we go to the variable assignment page.
	 * </p>
	 * 
	 * @return Navigation to the files
	 * @throws InstrumentFileExistsException If the default instrument file has already been added.
	 */
	public String goToFiles() throws InstrumentFileExistsException {
		String result;
		
		if (instrumentFiles.size() == 0) {
			currentInstrumentFile = new FileDefinitionBuilder();
			
			result = NAV_UPLOAD_FILE;
		} else {
			if (null == currentInstrumentFile) {
				currentInstrumentFile = FileDefinitionBuilder.copy(instrumentFiles.first());
			}
			result = NAV_ASSIGN_VARIABLES; 
		}
		
		return result;
	}
	
	@Override
	protected String getFormName() {
		return "newInstrumentForm";
	}

	/**
	 * Store the uploaded data in the current instrument file.
	 * Detailed processing will be triggered by the source page calling {@link FileDefinitionBuilder#guessFileLayout}.
	 */
	@Override
	public void processUploadedFile() {
		String fileContent = new String(getFile().getContents(), StandardCharsets.UTF_8);
		List<String> fileLines = Arrays.asList(fileContent.split("[\\r\\n]+"));
		
		if (fileLines.size() > FileDefinitionBuilder.FILE_DATA_MAX_LINES) {
			fileLines = fileLines.subList(0, FileDefinitionBuilder.FILE_DATA_MAX_LINES - 1);
		}
		
		currentInstrumentFile.setFileDataArray(fileLines);
		currentInstrumentFile.setFileData(HtmlUtils.makeJSONArray(fileLines));
	}
	
	/**
	 * Clear all data from the bean ready for a new
	 * instrument to be defined
	 */
	private void clearAllData() {
		instrumentName = null;
		instrumentFiles = new InstrumentFileSet();
		sensorAssignments = ResourceManager.getInstance().getSensorsConfiguration().getNewSensorAssigments();
		clearFile();
	}
	
	/**
	 * Get the name of the new instrument
	 * @return The instrument name
	 */
	public String getInstrumentName() {
		return instrumentName;
	}
	
	/**
	 * Set the name of the new instrument
	 * @param instrumentName The instrument name
	 */
	public void setInstrumentName(String instrumentName) {
		this.instrumentName = instrumentName;
	}

	/**
	 * Get the instrument file that is currently being worked on
	 * @return The current instrument file
	 */
	public FileDefinitionBuilder getCurrentInstrumentFile() {
		return currentInstrumentFile;
	}
	
	/**
	 * Retrieve the full set of instrument files
	 * @return The instrument files
	 */
	public InstrumentFileSet getInstrumentFiles() {
		return instrumentFiles;
	}
	
	/**
	 * Determines whether or not the file set contains more than one file
	 * @return {@code true} if more than one file is in the set; {@code false} if there are zero or one files
	 */
	public boolean getHasMultipleFiles() {
		return (instrumentFiles.size() > 1);
	}
	
	/**
	 * Add the current instrument file to the file set (or update it)
	 * and clear its status as 'current'. Then navigate to the
	 * variable assignment page.
	 * @return The navigation to the variable assignment page
	 */
	public String useFile() {
		instrumentFiles.storeFile(currentInstrumentFile);
		clearFile();
		return NAV_ASSIGN_VARIABLES;
	}

	/**
	 * Discard the current instrument file
	 */
	public void discardUploadedFile() {
		clearFile();
	}
	
	@Override
	public void clearFile() {
		currentInstrumentFile = new FileDefinitionBuilder();
		super.clearFile();
	}
	
	/**
	 * Get the current set of sensor assignments as a JSON string.
	 * 
	 * <p>
	 *   The format of the JSON is as follows:
	 * </p>
	 * <pre>
	 * [
	 *   {
	 *     "name": "&lt;sensor type name&gt;",
	 *     "required": &lt;true/false&gt;,
	 *     "assignments": [
	 *       "file": "&lt;data file name&gt;",
	 *       "column": &lt;column index&gt;
	 *     ]
	 *   }
	 * ]
	 * </pre>
	 * 
	 * @return The sensor assignments
	 * @throws SensorAssignmentException If the sensor assignments are internally invalid 
	 */
	public String getSensorAssignments() throws SensorAssignmentException {
		StringBuilder json = new StringBuilder();
		
		// Start the array of objects
		json.append('[');
		
		int count = 0;
		for (SensorType sensorType : sensorAssignments.keySet()) {
			json.append('{');
			
			// Sensor Type name
			json.append("\"name\": \"");
			json.append(sensorType.getName());
			json.append("\",");
			
			// Is an assignment required?
			json.append("\"required\":");
			json.append(sensorAssignments.isAssignmentRequired(sensorType));
			json.append(",");
			
			// The columns assigned to the sensor type
			Set<SensorAssignment> assignments = sensorAssignments.get(sensorType);
			
			json.append("\"assignments\":[");
			
			if (null != assignments) {
				int assignmentCount = 0;
				for (SensorAssignment assignment : assignments) {
					json.append("{\"file\":\"");
					json.append(assignment.getDataFile());
					json.append("\",\"column\";");
					json.append(assignment.getColumn());
					json.append('}');
					
					if (assignmentCount < assignments.size() - 1) {
						json.append(',');
					}
				}
			}
			
			json.append(']');
			
			// End the array, and add a comma if this isn't the last object
			json.append('}');
			if (count < sensorAssignments.size() - 1) {
				json.append(',');
			}
			count++;
		}
		
		// Finish the array
		json.append(']');
		
		return json.toString();
	}
}
