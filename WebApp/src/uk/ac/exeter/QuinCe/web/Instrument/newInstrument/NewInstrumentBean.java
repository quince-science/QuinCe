package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.InvalidPositionFormatException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
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
	 * Sensor assignment - file index
	 */
	private String sensorAssignmentFile = null;
	
	/**
	 * Sensor assignment - column index
	 */
	private int sensorAssignmentColumn = -1;
	
	/**
	 * The name of the sensor being assigned
	 */
	private String sensorAssignmentSensorType = null;
	
	/**
	 * Sensor assignment - must post-calibration be applied?
	 */
	private boolean sensorAssignmentPostCalibrated = false;
	
	/**
	 * Sensor assignment - the answer to the Depends Question
	 * @see SensorType#getDependsQuestion()
	 */
	private boolean sensorAssignmentDependsQuestionAnswer = false;
	
	/**
	 * Sensor assignment - is this a primary or fallback sensor?
	 */
	private boolean sensorAssignmentPrimary = false;
	
	/**
	 * The file for which the longitude is being set
	 */
	private String longitudeFile = null;
	
	/**
	 * The column index of the longitude
	 */
	private int longitudeColumn = -1;
	
	/**
	 * The longitude format
	 */
	private int longitudeFormat = 0;
	
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
			currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);
			
			result = NAV_UPLOAD_FILE;
		} else {
			if (null == currentInstrumentFile) {
				currentInstrumentFile = FileDefinitionBuilder.copy(instrumentFiles.get(0));
			}
			result = NAV_ASSIGN_VARIABLES; 
		}
		
		return result;
	}
	
	/**
	 * Add a new file to the instrument
	 * @return The navigation to the file upload
	 */
	public String addFile() {
		currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);
		return NAV_UPLOAD_FILE;
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
		resetSensorAssignmentValues();
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
		currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);
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
			json.append(',');
			
			// Are many assignments allowed for this sensor?
			json.append("\"many\":");
			json.append(sensorType.canHaveMany());
			json.append(',');
			
			// Will multiple sensors be averaged?
			json.append("\"averaged\":");
			json.append(sensorType.isAveraged());
			json.append(',');
			
			// Can sensors of this type be post-calibrated?
			json.append("\"postCalibrated\":");
			json.append(sensorType.canBePostCalibrated());
			json.append(',');
			
			// The Depends Question
			json.append("\"dependsQuestion\":");
			if (null == sensorType.getDependsQuestion()) {
				json.append("null");
			} else {
				json.append('"');
				json.append(sensorType.getDependsQuestion());
				json.append('"');
			}
			json.append(',');
			
			// The columns assigned to the sensor type
			Set<SensorAssignment> assignments = sensorAssignments.get(sensorType);
			
			json.append("\"assignments\":[");
			
			if (null != assignments) {
				int assignmentCount = 0;
				for (SensorAssignment assignment : assignments) {
					json.append("{\"file\":\"");
					json.append(assignment.getDataFile());
					json.append("\",\"column\":");
					json.append(assignment.getColumn());
					json.append(",\"postCalibrated\":");
					json.append(assignment.getPostCalibrated());
					json.append(",\"primary\":");
					json.append(assignment.isPrimary());
					json.append('}');
					
					if (assignmentCount < assignments.size() - 1) {
						json.append(',');
					}
					
					assignmentCount++;
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
	
	/**
	 * Dummy method for setting sensor assignments. It doesn't
	 * actually do anything, but it's needed for the JSF communications
	 * to work.
	 * @param assignments The assignments. They are ignored.
	 */
	public void setSensorAssignments(String assignments) {
		// Do nothing
	}
	
	/**
	 * Get the time and position column assignments for all
	 * files related to this instrument.
	 * 
	 * <p>
	 *   The assignments are encoded as a JSON string in
	 *   the following format:
	 * </p>
	 * <pre>
	 * [
	 * ]
	 * </pre>
	 * @return The time and position assignments
	 */
	public String getTimePositionAssignments() {
		StringBuilder json = new StringBuilder();
		
		json.append('[');
		
		for (int i = 0; i < instrumentFiles.size(); i++) {
			FileDefinitionBuilder file = instrumentFiles.get(i);
			
			json.append('{');
			json.append("\"primaryPosition\":");
			json.append(file.getPositionPrimary());
			json.append(",\"longitude\":");
			json.append(file.getLongitudeSpecification().getJsonString());
			json.append(",\"latitude\":");
			json.append(file.getLatitudeSpecification().getJsonString());
			json.append(",\"dateTime\":");
			json.append(file.getDateTimeSpecification().getJsonString());
			json.append('}');
			
			if (i < instrumentFiles.size() - 1) {
				json.append(',');
			}
		}
		
		json.append(']');
		
		return json.toString();
	}
	
	/**
	 * Dummy method for setting time and position assignments. It doesn't
	 * actually do anything, but it's needed for the JSF communications
	 * to work.
	 * @param assignments The assignments. They are ignored.
	 */
	public void setTimePositionAssignments(String assignments) {
		// Do nothing
	}
	
	/**
	 * Get the list of registered file descriptions and their columns as a JSON string
	 * @return The file names
	 */
	public String getFilesAndColumns() {
		StringBuilder json = new StringBuilder();
		
		json.append('[');

		for (int i = 0; i < instrumentFiles.size(); i++) {
			json.append('{');
			
			json.append("'description':'");
			json.append(instrumentFiles.get(i).getFileDescription());
			json.append("','columns':");
			json.append(instrumentFiles.get(i).getFileColumns());
			json.append('}');

			if (i < instrumentFiles.size() - 1) {
				json.append(',');
			}
		}
		
		json.append(']');
		return json.toString();
	}

	/**
	 * Get the sensor assignment file
	 * @return The file
	 */
	public String getSensorAssignmentFile() {
		return sensorAssignmentFile;
	}

	/**
	 * Set the sensor assignment file
	 * @param sensorAssignmentFile The file
	 */
	public void setSensorAssignmentFile(String sensorAssignmentFile) {
		this.sensorAssignmentFile = sensorAssignmentFile;
	}

	/**
	 * Get the sensor assignment column index
	 * @return The column index
	 */
	public int getSensorAssignmentColumn() {
		return sensorAssignmentColumn;
	}

	/**
	 * Set the sensor assignment column index
	 * @param sensorAssignmentColumn The column index
	 */
	public void setSensorAssignmentColumn(int sensorAssignmentColumn) {
		this.sensorAssignmentColumn = sensorAssignmentColumn;
	}

	/**
	 * Get the name of the sensor type being assigned
	 * @return The sensor type
	 */
	public String getSensorAssignmentSensorType() {
		return sensorAssignmentSensorType;
	}

	/**
	 * Set the name of the sensor type being assigned
	 * @param sensorAssignmentSensorType The sensor type
	 */
	public void setSensorAssignmentSensorType(String sensorAssignmentSensorType) {
		this.sensorAssignmentSensorType = sensorAssignmentSensorType;
	}

	/**
	 * Get the sensor assignment post-calibration flag
	 * @return The post-calibration flag
	 */
	public boolean getSensorAssignmentPostCalibrated() {
		return sensorAssignmentPostCalibrated;
	}

	/**
	 * Set the sensor assignment post-calibration flag
	 * @param sensorAssignmentPostCalibrated The post-calibration flag
	 */
	public void setSensorAssignmentPostCalibrated(boolean sensorAssignmentPostCalibrated) {
		this.sensorAssignmentPostCalibrated = sensorAssignmentPostCalibrated;
	}

	/**
	 * Get the answer to the sensor assignment's Depends Question
	 * @return The answer to the Depends Question
	 * @see SensorType#getDependsQuestion()
	 */
	public boolean getSensorAssignmentDependsQuestionAnswer() {
		return sensorAssignmentDependsQuestionAnswer;
	}

	/**
	 * Set the answer to the sensor assignment's Depends Question
	 * @param sensorAssignmentDependsQuestionAnswer The answer to the Depends Question
	 * @see SensorType#getDependsQuestion()
	 */
	public void setSensorAssignmentDependsQuestionAnswer(boolean sensorAssignmentDependsQuestionAnswer) {
		this.sensorAssignmentDependsQuestionAnswer = sensorAssignmentDependsQuestionAnswer;
	}

	/**
	 * Get the flag indicating whether the assigned sensor is primary or fallback
	 * @return The primary sensor flag
	 */
	public boolean getSensorAssignmentPrimary() {
		return sensorAssignmentPrimary;
	}

	/**
	 * Set the flag indicating whether the assigned sensor is primary or fallback
	 * @param sensorAssignmentPrimary The primary sensor flag
	 */
	public void setSensorAssignmentPrimary(boolean sensorAssignmentPrimary) {
		this.sensorAssignmentPrimary = sensorAssignmentPrimary;
	}
	
	/**
	 * Add a new assignment to the sensor assignments
	 * @throws Exception If any errors occur
	 */
	public void storeSensorAssignment() throws Exception {
		SensorAssignment assignment = new SensorAssignment(sensorAssignmentFile, sensorAssignmentColumn, sensorAssignmentPostCalibrated, sensorAssignmentPrimary, sensorAssignmentDependsQuestionAnswer);
		sensorAssignments.addAssignment(sensorAssignmentSensorType, assignment);
		
		// Reset the assign dialog values, because it's so damn hard to do in Javascript
		resetSensorAssignmentValues();
	}
	
	/**
	 * Set the assignment dialog values to their defaults
	 */
	public void resetSensorAssignmentValues() {
		sensorAssignmentFile = null;
		sensorAssignmentColumn = -1;
		sensorAssignmentSensorType = null;
		sensorAssignmentPrimary = true;
		sensorAssignmentPostCalibrated = false;
		sensorAssignmentDependsQuestionAnswer = false;

	}
	
	/**
	 * Get the file for which the longitude is being set
	 * @return The longitude file
	 */
	public String getLongitudeFile() {
		return longitudeFile;
	}
	
	/**
	 * Set the file for which the longitude is being set
	 * @param longitudeFile The longitude file
	 */
	public void setLongitudeFile(String longitudeFile) {
		this.longitudeFile = longitudeFile;
	}
	
	/**
	 * Get the longitude column index
	 * @return The longitude column index
	 */
	public int getLongitudeColumn() {
		return longitudeColumn;
	}
	
	/**
	 * Set the longitude column index
	 * @param longitudeColumn The longitude column index
	 */
	public void setLongitudeColumn(int longitudeColumn) {
		this.longitudeColumn = longitudeColumn;
	}
	
	/**
	 * Get the longitude format
	 * @return The longitude format
	 */
	public int getLongitudeFormat() {
		return longitudeFormat;
	}
	
	/**
	 * Set the longitude format
	 * @param longitudeFormat The longitude format
	 */
	public void setLongitudeFormat(int longitudeFormat) {
		this.longitudeFormat = longitudeFormat;
	}
	
	/**
	 * Set the longitude column and format for a file
	 * @throws InvalidPositionFormatException If the format is invalid
	 */
	public void assignLongitude() throws InvalidPositionFormatException {
		FileDefinitionBuilder file = instrumentFiles.get(longitudeFile);
		file.getLongitudeSpecification().setValueColumn(longitudeColumn);
		file.getLongitudeSpecification().setFormat(longitudeFormat);
		if (longitudeFormat != LongitudeSpecification.FORMAT_0_180) {
			file.getLongitudeSpecification().setHemisphereColumn(-1);
		}
	}
}
