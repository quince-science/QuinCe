package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

/**
 * Defines a Run Type Category
 * @author Steve Jones
 *
 */
public class RunTypeCategory implements Comparable<RunTypeCategory> {

	/**
	 * Measurement category identifier
	 */
	public static final int TYPE_MEASUREMENT = 0;
	
	/**
	 * Calibration category identifier
	 */
	public static final int TYPE_CALIBRATION = 1;
	
	/**
	 * The special IGNORED run type category
	 */
	public static RunTypeCategory IGNORED_CATEGORY = null;
	
	/**
	 * The special External Standard run type category
	 */
	public static RunTypeCategory EXTERNAL_STANDARD_CATEGORY = null;
	
	/**
	 * The category code
	 */
	private String code;
	
	/**
	 * The category name
	 */
	private String name;
	
	/**
	 * The category type
	 */
	private int type;
	
	/**
	 * Some applications require that there is a minimum number
	 * of run types for a given category in a single instrument.
	 * For example, an underway surface ocean pCO₂ instrument
	 * needs at least three gas standards.
	 */
	private int minCount;
	
	static {
		try {
			IGNORED_CATEGORY = new RunTypeCategory("IGN", "IGNORED", 0, 0);
			EXTERNAL_STANDARD_CATEGORY = new RunTypeCategory("EXT", "External Standard", 1, 0);
		} catch (InvalidCategoryTypeException e) {
			// Do nothing
		}
	}
	
	/**
	 * Basic constructor
	 * @param code The category code
	 * @param name The category name
	 * @param type The category type
	 * @param minCount The minimum number of run types required for this category
	 * @throws InvalidCategoryTypeException If the type is invalid
	 */
	protected RunTypeCategory(String code, String name, int type, int minCount) throws InvalidCategoryTypeException {
		this.code = code;
		this.name = name;
		
		if (type != TYPE_MEASUREMENT && type != TYPE_CALIBRATION) {
			throw new InvalidCategoryTypeException(type);
		}
		
		this.type = type;
		this.minCount = minCount;
	}
	
	/**
	 * Get the category code
	 * @return The code
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * Get the category name
	 * @return The name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the category type
	 * @return The type
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Get the minimum number of run types of this category
	 * required by the application
	 * @return The minimum run type count
	 */
	public int getMinCount() {
		return minCount;
	}

	@Override
	public int compareTo(RunTypeCategory o) {
		return code.compareTo(o.code);
	}
	
	/**
	 * Determine whether or not this run type is for measurements
	 * @return {@code true} if this is a measurement type; {@code false} otherwise
	 */
	public boolean isMeasurementType() {
		return type == TYPE_MEASUREMENT;
	}
	
	@Override
	public boolean equals(Object o) {
		
		boolean equals = false;
		
		if (o instanceof RunTypeCategory) {
			RunTypeCategory oRunTypeCategory = (RunTypeCategory) o;
			equals = oRunTypeCategory.code.equals(code) && oRunTypeCategory.name.equals(name);
		}
		
		return equals;
	}
	
	@Override
	public String toString() {
		return code + ":" + name;
	}
}
