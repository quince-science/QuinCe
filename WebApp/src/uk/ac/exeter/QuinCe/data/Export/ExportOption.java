package uk.ac.exeter.QuinCe.data.Export;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QCRoutines.messages.Flag;

/**
 * Class to hold details of a single export configuration
 * @author Steve Jones
 * @see ExportConfig
 */
public class ExportOption {

	/**
	 * The index of this configuration in the export configuration file
	 */
	private int index;
	
	/**
	 * The name of this export configuration
	 */
	private String name;
	
	/**
	 * The separator to be used between columns in the output file
	 */
	private String separator;
	
	/**
	 * The set of columns to be exported. See {@link ExportConfig} for a list
	 * of supported columns
	 */
	private List<String> columns;

	/**
	 * The type of CO<sub>2</sub> measurement to be exported. This is one of:
	 * <ul>
	 *   <li><b>0</b> : Water</li>
	 *   <li><b>1</b> : Atmospheric</li>
	 *   <li><b>3</b> : Both</li>
	 * </ul>
	 */
	private int co2Type;
	
	/**
	 * The list of flags that records must match to be included in the export
	 */
	private List<Integer> flags;
	
	/**
	 * Constructor for an export option object
	 * @param index The index of the configuration in the export configuration file 
	 * @param name The name of the configuration
	 * @param separator The column separator
	 * @param columns The columns to be exported
	 * @param co2Type The type of measurements to be exported
	 * @throws ExportException If the export configuration is invalid
	 */
	public ExportOption(int index, String name, String separator, List<String> columns, int co2Type) throws ExportException {
		this.index = index;
		this.name = name;
		this.separator = separator;
		this.columns = columns;
		this.co2Type = co2Type;
		
		flags = new ArrayList<Integer>();
		flags.add(Flag.VALUE_GOOD);
		flags.add(Flag.VALUE_ASSUMED_GOOD);
		flags.add(Flag.VALUE_QUESTIONABLE);
		flags.add(Flag.VALUE_BAD);
		flags.add(Flag.VALUE_NEEDED);
	}
	
	/**
	 * Returns the index of this configuration in the export configuration file
	 * @return The configuration index
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Returns the name of the configuration
	 * @return The configuration name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the list of columns to be included in the export file
	 * @return The list of columns
	 */
	public List<String> getColumns() {
		return columns;
	}
	
	/**
	 * The column separator to be used in the export file
	 * @return The column separator
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * Returns the type of measurements to be included in the export file
	 * @return The type of measurements to be included in the export file
	 * @see #co2Type
	 */
	public int getCo2Type() {
		return co2Type;
	}
	
	/**
	 * Returns the list of record flags to be included in the export file.
	 * Records without one of these flags will not be exported.
	 * @return The list of flags
	 */
	public List<Integer> getFlags() {
		return flags;
	}
}
