package uk.ac.exeter.QuinCe.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * <p>
 *   Holds details of the various file export options configured for the application.
 * </p>
 * 
 * <p>
 *   The export options are provided in a CSV file. Each line in the file represents one export option,
 *   and contains the following columns:
 * </p>
 * <ul>
 *   <li>
 *     <b>Name</b> - The display name of this export option.
 *   </li>
 *   <li>
 *     <b>Separator</b> - The column separator in the exported file. Since the configuration file is CSV,
 *     a comma separator must be escaped ({@code \,}).
 *   </li>
 *   <li>
 *     <b>CO<sub>2</sub> Type</b> - The type of CO<sub>2</sub> measurements to be exported:
 *     0 = Water, 1 = Atmospheric, 3 = Both.
 *   </li>
 *   <li>
 *     <b>Column, Column...</b> - After the CO<sub>2</sub> type, the remaining columns list the data
 *     columns to be included in the output.
 *   </li>
 * </ul>
 * 
 * <p>
 *   Supported column names are:
 * </p>
 * <ul>
 *   <li>{@code dateTime}</li>
 *   <li>{@code longitude}</li>
 *   <li>{@code latitude}</li>
 *   <li>{@code intakeTempMean}</li>
 *   <li>{@code intakeTemp1}</li>
 *   <li>{@code intakeTemp2}</li>
 *   <li>{@code intakeTemp3}</li>
 *   <li>{@code salinityMean}</li>
 *   <li>{@code salinity1}</li>
 *   <li>{@code salinity2}</li>
 *   <li>{@code salinity3}</li>
 *   <li>{@code eqtMean}</li>
 *   <li>{@code eqt1}</li>
 *   <li>{@code eqt2}</li>
 *   <li>{@code eqt3}</li>
 *   <li>{@code deltaT}</li>
 *   <li>{@code eqpMean}</li>
 *   <li>{@code eqp1}</li>
 *   <li>{@code eqp2}</li>
 *   <li>{@code eqp3}</li>
 *   <li>{@code airFlow1}</li>
 *   <li>{@code airFlow2}</li>
 *   <li>{@code airFlow3}</li>
 *   <li>{@code waterFlow1}</li>
 *   <li>{@code waterFlow2}</li>
 *   <li>{@code waterFlow3}</li>
 *   <li>{@code atmosPressure}</li>
 *   <li>{@code moistureMeasured}</li>
 *   <li>{@code moistureTrue}</li>
 *   <li>{@code pH2O}</li>
 *   <li>{@code co2Measured}</li>
 *   <li>{@code co2Dried}</li>
 *   <li>{@code co2Calibrated}</li>
 *   <li>{@code pCO2TEDry}</li>
 *   <li>{@code pCO2TEWet}</li>
 *   <li>{@code fCO2TE}</li>
 *   <li>{@code fCO2Final}</li>
 *   <li>{@code qcFlag}</li>
 *   <li>{@code qcMessage}</li>
 *   <li>{@code woceFlag}</li>
 *   <li>{@code woceMessage}</li>
 * </ul>
 * 
 * <p>
 *   There is also a special column called {@code original}, which will grab the original file from disk
 *   and include it in the exported file. If this is included, the separator specified in the configuration
 *   will be ignored, and the separator from the original file will be used instead.
 * </p>
 * 
 * <p>
 *   The export options are held in a list, kept in the same order as the options appear in the configuration file.
 * </p>
 * 
 * <p>
 *   This class exists as a singleton that must be initialised before it is used by calling the {@link #init(String)} method.
 * </p>
 * 
 * @author Steve Jones
 *
 */
public class ExportConfig {

	/**
	 * The concrete instance of the {@code ExportConfig} singleton
	 */
	private static ExportConfig instance = null;
	
	/**
	 * The set of export options
	 */
	private List<ExportOption> options = null;
	
	/**
	 * The full path of the export configuration file
	 */
	private static String configFilename = null;
	
	/**
	 * <p>
	 *   Loads and parses the export configuration file.
	 * </p>
	 * 
	 * <p>
	 *   This is an internal constructor that is called by the {@link #init(String)} method.
	 * </p>
	 * 
	 * @throws ExportException If the configuration file cannot be loaded or parsed
	 */
	private ExportConfig() throws ExportException {
		if (configFilename == null) {
			throw new ExportException("ExportConfig filename has not been set - must run init first");
		}

		options = new ArrayList<ExportOption>();
		try {
			readFile();
		} catch (FileNotFoundException e) {
			throw new ExportException("Could not find configuration file '" + configFilename + "'");
		}
	}
	
	/**
	 * Initialises the {@code ExportConfig} singleton. This must be called before the class can be used.
	 * @param configFile The full path to the export configuration file
	 * @throws ExportException If the configuration file cannot be loaded or parsed
	 */
	public static void init(String configFile) throws ExportException {
		configFilename = configFile;
		instance = new ExportConfig();
	}
	
	/**
	 * Returns the {@code ExportConfig} singleton instance
	 * @return The {@code ExportConfig} singleton instance
	 * @throws ExportException If the class has not been initialised
	 */
	public static ExportConfig getInstance() throws ExportException {
		
		if (null == instance) {
			throw new ExportException("Export options have not been configured");
		}
		
		return instance;
	}
	
	/**
	 * Read and parse the export options configuration file
	 * @throws ExportException If the configuration file is invalid
	 * @throws FileNotFoundException If the file specified in the {@link #init(String)} call does not exist
	 */
	private void readFile() throws ExportException, FileNotFoundException {
		
		BufferedReader reader = new BufferedReader(new FileReader(configFilename));
		
		String regex = "(?<!\\\\)" + Pattern.quote(",");
		int lineCount = 0;
		
		try {
			String line = reader.readLine();
			lineCount++;
			
			while (null != line) {
				if (!StringUtils.isComment(line)) {
					List<String> fields = Arrays.asList(line.split(regex));
					fields = StringUtils.trimList(fields);
					
					String name = fields.get(0);
					String separator = fields.get(1);
					if (separator.equals("\\t")) {
						separator = "\t";
					}
					
					int co2Type = Integer.parseInt(fields.get(2));
					
					List<String> columns = fields.subList(3, fields.size());
					
					options.add(new ExportOption(options.size(), name, separator, columns, co2Type));
				}
				
				line = reader.readLine();
				lineCount++;
			}
		} catch (Exception e) {
			if (e instanceof ExportException) {
				throw (ExportException) e;
			} else {
				throw new ExportException("Error initialising export options (line " + lineCount + ")", e);
			}
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				// Shrug
			}
		}
	}
	
	/**
	 * Returns the list of all configured export options
	 * @return The list of export options
	 */
	public List<ExportOption> getOptions() {
		return options;
	}
	
	/**
	 * <p>
	 *   Returns a specified export configuration referenced by its position in the configuration file (zero-based).
	 * </p>
	 * 
	 * <p>
	 *   This method will throw an {@link ArrayIndexOutOfBoundsException} if the specified index is outside the range
	 *   of the list of export options.
	 * </p>
	 * 
	 * @param index The index of the desired configuration.
	 * @return The export configuration
	 */
	public ExportOption getOption(int index) {
		return options.get(index);
	}
}
