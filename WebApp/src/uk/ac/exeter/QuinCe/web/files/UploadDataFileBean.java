package uk.ac.exeter.QuinCe.web.files;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.primefaces.context.RequestContext;

import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.RawDataFile;
import uk.ac.exeter.QuinCe.data.Files.RawDataFileException;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Standards.GasStandardDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.html.HtmlUtils;
import uk.ac.exeter.QuinCe.web.html.TableData;
import uk.ac.exeter.QuinCe.web.html.TableException;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class UploadDataFileBean extends FileUploadBean {

	/**
	 * Indicates that the file was processed with no errors
	 */
	public static final int FILE_NO_ERROR = 0;
	
	/**
	 * Indicates that the file could not be parsed
	 */
	public static final int FILE_PARSE_ERROR = 1;
	
	/**
	 * Indicates that the file could be parsed, but required
	 * calibration and/or gas standards are not present so the
	 * file cannot be processed further.
	 */
	public static final int FILE_BAD_DATES = 2;
	
	public static final String PAGE_FILE_LIST = "file_list";
	
	/**
	 * The ID of the instrument to which the uploaded file belongs
	 */
	private long instrument;
	
	/**
	 * The raw data file contents, before they are saved to disk
	 */
	private RawDataFile rawDataFile;
	
	/**
	 * Simply constructs the base RawDataFile object.
	 * No extraction or processing is performed.
	 */
	@Override
	public void processUploadedFile() {
		try {
			rawDataFile = new RawDataFile(InstrumentDB.getInstrument(ServletUtils.getDBDataSource(), instrument), getFile().getFileName(), getFile().getContents());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the list of instruments owned by the current user
	 * @return The list of instruments
	 * @throws MissingParamException If any parameters to the database call are missing
	 * @throws DatabaseException If an error occurs while searching the database
	 * @throws ResourceException If the data source cannot be located
	 */
	public List<InstrumentStub> getInstrumentList() throws MissingParamException, DatabaseException, ResourceException {
		return InstrumentDB.getInstrumentList(ServletUtils.getDBDataSource(), getUser());
	}
	
	/**
	 * Check the file for basic validity.
	 * At present, we are simply checking that there are
	 * calibrations and standards that can be used.
	 * Other short checks will be added later.
	 */
	public void checkFile(ActionEvent event) {

		RequestContext context = RequestContext.getCurrentInstance();
		int fileState = FILE_NO_ERROR;		
		List<String> messages = new ArrayList<String>();

		try {
			List<Calendar> dates = rawDataFile.getDates(messages);
			Calendar firstDate = dates.get(0);
			Calendar lastDate = dates.get(dates.size() - 1);
			
			TableData datesTable = new TableData("fileDates", "right");
			boolean datesOK = buildDatesTable(firstDate, lastDate, datesTable, messages);
			context.addCallbackParam("DATES_TABLE", datesTable.toHtml());
			if (!datesOK) {
				fileState = FILE_BAD_DATES;
			}
		} catch (RawDataFileException e) {
			fileState = FILE_PARSE_ERROR;
		} catch (Exception e) {
			e.printStackTrace();
			fileState = FILE_PARSE_ERROR;
			messages.add("SYSTEM ERROR: " + e.getMessage());
		} finally {
			context.addCallbackParam("CHECK_RESULT", fileState);
			context.addCallbackParam("MESSAGES", StringUtils.listToDelimited(messages));
		}
	}
	
	/**
	 * Build the table of calibration and standard dates for the uploaded file.
	 * This also checks that all necessary calibrations and standard are present
	 * for the file to be processed successfully.
	 * 
	 * @param firstDate The first date in the data file
	 * @param lastDate The last date in the data file
	 * @param table The table to which the content should be added
	 * @param messages The set of error messages for the file upload
	 * @return {@code true} if all required calibrations and standards are preset; {@code false} if any are not available.
	 * @throws MissingParamException If any of the parameters on the database calls are missing
	 * @throws DatabaseException If an error occurs while searching the database
	 * @throws ResourceException If an error occurs while retrieving the data source
	 * @throws TableException If an error occurs while building the table
	 */
	private boolean buildDatesTable(Calendar firstDate, Calendar lastDate, TableData table, List<String> messages) throws MissingParamException, DatabaseException, ResourceException, TableException {

		boolean result = true;
		
		List<Calendar> calibrationsInFile = CalibrationDB.getCalibrationDatesBetween(ServletUtils.getDBDataSource(), instrument, firstDate, lastDate);
		Calendar calibrationBeforeFile = CalibrationDB.getCalibrationDateBefore(ServletUtils.getDBDataSource(), instrument, firstDate);
		
		List<Calendar> standardsInFile = GasStandardDB.getStandardDatesBetween(ServletUtils.getDBDataSource(), instrument, firstDate, lastDate);
		Calendar standardBeforeFile = GasStandardDB.getStandardDateBefore(ServletUtils.getDBDataSource(), instrument, firstDate);
		
		// Now we've got all the data we need, let's build the result
		table.setHeaders("File dates", "Calibration Dates", "Standard Dates");
		
		// The first line is for the pre-file calibration and standard dates,
		// so the first column (file date) is empty
		table.addEmptyColumn();
		
		// The calibration date before the file, if there is one
		if (null == calibrationBeforeFile) {
			table.addEmptyColumn();
		} else {
			table.addColumn(calibrationBeforeFile);
		}
		
		// The standard date before the file, if there is one
		if (null == standardBeforeFile) {
			table.addEmptyColumn();
		} else {
			table.addColumn(standardBeforeFile);
		}
		
		
		// Now the first date of the data file
		table.addColumn(firstDate);
		
		// The age of the preceding calibration, or an error message
		if (null == calibrationBeforeFile) {
			if (calibrationsInFile.size() > 0) {
				table.addColumn("NO CALIBRATIONS BEFORE FILE", HtmlUtils.CLASS_ERROR);
				messages.add("There are no calibrations available before the first record in this file");
			} else {
				table.addEmptyColumn();
			}
			result = false;
		} else {
			table.addColumn(DateTimeUtils.getDaysBetween(calibrationBeforeFile, firstDate) + " days old", HtmlUtils.CLASS_INFO);
		}
		
		// The age of the preceding standard, or an error message
		if (null == standardBeforeFile) {
			if (standardsInFile.size() > 0) {
				table.addColumn("NO STANDARDS BEFORE FILE", HtmlUtils.CLASS_ERROR);
				messages.add("There are no gas standards available before the first record in this file");
			} else {
				table.addEmptyColumn();
			}
			result = false;
		}  else {
			table.addColumn(DateTimeUtils.getDaysBetween(standardBeforeFile, firstDate) + " days old", HtmlUtils.CLASS_INFO);
		}
		
		// Loop through all the calibrations and standards in the file,
		// and generate table rows - one per unique date
		int currentCalibration = 0;
		int currentStandard = 0;
		
		while (currentCalibration < calibrationsInFile.size() && currentStandard < standardsInFile.size()) {
			
			// The file date is always empty
			table.addEmptyColumn();
			
			// Work out whether a calibration or standard is next
			int comparison = calibrationsInFile.get(currentCalibration).compareTo(standardsInFile.get(currentStandard));
			
			// If the calibration is next (or equal to the next standard)...
			if (comparison <= 0) {
				table.addColumn(calibrationsInFile.get(currentCalibration));
				currentCalibration++;
			} else {
				table.addEmptyColumn();
			}
			
			// If the standard is next (or equal to the next calibration)...
			if (comparison >= 0) {
				table.addColumn(standardsInFile.get(currentStandard));
				currentStandard++;
			} else {
				table.addEmptyColumn();
			}
		}
		
		// If we have calibrations left, add them
		for (;currentCalibration < calibrationsInFile.size(); currentCalibration++) {
			table.addEmptyColumn();
			table.addColumn(calibrationsInFile.get(currentCalibration));
			table.addEmptyColumn();
		}
		
		for (;currentStandard < standardsInFile.size(); currentStandard++) {
			table.addEmptyColumn();
			table.addEmptyColumn();
			table.addColumn(standardsInFile.get(currentStandard));
		}
		
		// And finally, the last file date, with the age to the last in-file calibration/standard.
		// Or if there aren't any, the pre-file calibration/standard. Or an error message.
		table.addColumn(lastDate);
		
		if (calibrationsInFile.size() > 0) {
			table.addColumn(DateTimeUtils.getDaysBetween(calibrationsInFile.get(calibrationsInFile.size() - 1), lastDate) + " days old", HtmlUtils.CLASS_INFO);
		} else if (null != calibrationBeforeFile) {
			table.addColumn(DateTimeUtils.getDaysBetween(calibrationBeforeFile, lastDate) + " days old", HtmlUtils.CLASS_INFO);
		} else {
			table.addColumn("NO CALIBRATIONS AVAILABLE", HtmlUtils.CLASS_ERROR);
			messages.add("There are no sensor calibrations available to calibrate this data file");
			result = false;
		}
		
		if (standardsInFile.size() > 0) {
			table.addColumn(DateTimeUtils.getDaysBetween(standardsInFile.get(standardsInFile.size() - 1), lastDate) + " days old", HtmlUtils.CLASS_INFO);		
		} else if (null != standardBeforeFile) {
			table.addColumn(DateTimeUtils.getDaysBetween(standardBeforeFile, lastDate) + " days old", HtmlUtils.CLASS_INFO);
		} else {
			table.addColumn("NO STANDARDS AVAILABLE", HtmlUtils.CLASS_ERROR);
			messages.add("There are no gas standards available to calibrate this data file");
			result = false;
		}
		
		return result;
	}
	
	/**
	 * A dummy method allowing the front end to
	 * send the chosen instrument back to this bean
	 */
	public void storeInstrument() {
		// Do nothing
	}
	
	/**
	 * Navigation for the cancel button
	 * @return Navigation back to the file list
	 */
	public String cancelUpload() {
		return PAGE_FILE_LIST;
	}

	/**
	 * Stores the uploaded file
	 * @return The navigation string for the file list
	 */
	public String submitFile() {
		
		try {
			DataFileDB.storeFile(ServletUtils.getDBDataSource(), ServletUtils.getAppConfig(), getUser(), instrument, rawDataFile);
		} catch (Exception e) {
			return internalError(e);
		}
		
		return PAGE_FILE_LIST;
	}
	
	
	/////////////// *** GETTERS AND SETTERS *** ////////////////////////
	
	/**
	 * Returns the ID of the instrument to which this file belongs
	 * @return The instrument ID
	 */
	public long getInstrument() {
		return instrument;
	}

	/**
	 * Sets the ID of the instrument to which this file belongs
	 * @param instrument The instrument ID
	 */
	public void setInstrument(long instrument) {
		this.instrument = instrument;
	}

	@Override
	protected String getFormName() {
		return "uploadFileForm";
	}
}
