package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

/**
 * Defines an individual sensor type for an instrument
 * @author Steve Jones
 */
public class SensorType {

	/**
	 * The name of the sensor type
	 */
	private String name;
	
	/**
	 * Specifies whether or not the sensor is required in the instrument.
	 * 
	 * <p>
	 *   Setting a {@link #requiredGroup} overrides the behaviour of this flag.
	 * </p>
	 */
	private boolean required;
	
	/**
	 * The name of the group of sensors to which this sensor belongs.
	 * 
	 * <p>
	 *   Some sensors can be defined together in groups, where at least
	 *   one sensor in the group must be present. This names the group -
	 *   any sensors with the same name will be in the same group.
	 * </p>
	 * 
	 * <p>
	 *   Setting a value in the {@code requiredGroup} overrides the behaviour
	 *   of the {@link #required} flag.
	 * </p>
	 */
	private String requiredGroup;
	
	/**
	 * Specifies the name of another sensor that must also be present if
	 * this sensor is present.
	 * 
	 * <p>
	 *   Some sensor values depend on the value of another sensor in the instrument
	 *   for the necessary calculations to be performed. For example, a differential
	 *   pressure sensor requires an absolute atmospheric pressure sensor in order for
	 *   the true pressure to be calculated.
	 * </p>
	 */
	private String dependsOn;
	
	/**
	 * Indicates whether or not more than one sensor of this type can be
	 * present in an instrument.
	 * 
	 * <p>
	 *   In most cases multiple instances of a given sensor are allowed, but
	 *   there are instances where only one sensor can be used.
	 * </p>
	 */
	private boolean multipleAllowed;
	
	/**
	 * Simple constructor - sets all values
	 * @param name The name of the sensor type
	 * @param required Whether or not the sensor type is required
	 * @param requiredGroup The Required Group that this sensor type belongs to
	 * @param dependsOn The name of another sensor type that this sensor type depends on
	 * @param multipleAllowed Whether more than one instance of this sensor type is permitted
	 */
	protected SensorType(String name, boolean required, String requiredGroup, String dependsOn, boolean multipleAllowed) {
		this.name = name;
		this.required = required;
		this.requiredGroup = requiredGroup;
		this.dependsOn = dependsOn;
		this.multipleAllowed = multipleAllowed;
	}
	
	/**
	 * Get the name of this sensor type
	 * @return The name of the sensor type
	 */
	protected String getName() {
		return name;
	}
	
	/**
	 * Get the name of the sensor type that must exist in conjunction
	 * with this sensor type
	 * @return The name of the sensor type that this sensor type depends on
	 */
	public String getDependsOn() {
		return dependsOn;
	}
	
	/**
	 * Equality is based on the sensor name only
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		
		if (null != o && o instanceof SensorType) {
			equal = ((SensorType) o).name.equalsIgnoreCase(this.name);
		}
		
		return equal;
	}
}
