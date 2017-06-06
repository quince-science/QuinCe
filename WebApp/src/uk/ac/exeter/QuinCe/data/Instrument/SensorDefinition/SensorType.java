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
	private String requiredGroup = null;
	
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
	private String dependsOn = null;
	
	/**
	 * Specifies a question to ask that determines whether the {@link #dependsOn} criterion
	 * should be honoured. This question will yield a {@code boolean} result. If the result
	 * is {@code true}, then the {@link #dependsOn} criterion will be enforced. If false, it will not.
	 * If the question is empty or null, then the criterion will always be enforced.
	 */
	private String dependsQuestion = null;
	
	/**
	 * Indicates whether multiple instances of a sensor can be configured for an instrument
	 */
	private boolean many = true;
	
	/**
	 * Simple constructor - sets all values
	 * @param name The name of the sensor type
	 * @param required Whether or not the sensor type is required
	 * @param requiredGroup The Required Group that this sensor type belongs to
	 * @param dependsOn The name of another sensor type that this sensor type depends on
	 * @param dependsQuestion The question that determines whether the {@link #dependsOn} criterion will be honoured.
	 * @param many Whether or not multiple instances of the sensor are allowed
	 */
	protected SensorType(String name, boolean required, String requiredGroup, String dependsOn, String dependsQuestion, boolean many) {
		this.name = name;
		this.required = required;
		if (null != requiredGroup && requiredGroup.trim().length() > 0) {
			this.requiredGroup = requiredGroup;
		}
		if (null != dependsOn && dependsOn.trim().length() > 0) {
			this.dependsOn = dependsOn;
		}
		
		if (null != dependsQuestion && dependsQuestion.trim().length() > 0) {
			this.dependsQuestion = dependsQuestion;
		}
		
		this.many = many;
	}
	
	/**
	 * Get the name of this sensor type
	 * @return The name of the sensor type
	 */
	public String getName() {
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
	 * Get the flag indicating whether or not this sensor
	 * type is required.
	 * @return The required flag
	 */
	public boolean isRequired() {
		return required;
	}
	
	/**
	 * Get the name of the Required Group
	 * that this sensor type belongs to
	 * @return The Required Group
	 */
	public String getRequiredGroup() {
		return requiredGroup;
	}
	
	/**
	 * Gets the question that determines whether the {@link #dependsOn}
	 * criterion will be honoured.
	 * @return The question
	 * @see #dependsQuestion
	 */
	public String getDependsQuestion() {
		return dependsQuestion;
	}
	
	/**
	 * Determine whether or not multiple instances of this sensor are allowed.
	 * @return {@code true} if multiple instances are allowed; {@code false} if only one instance is allowed
	 */
	public boolean canHaveMany() {
		return many;
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
