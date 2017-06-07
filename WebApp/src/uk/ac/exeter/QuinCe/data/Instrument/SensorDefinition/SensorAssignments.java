package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Map of sensors to their assignments in an instrument's data files.
 * <p>
 *   Although this class extends the Map interface, you should use the
 *   convenience methods defined here to change the contents of the Map.
 *   You can do it all yourself if you like, but caveat emptor.
 * </p>
 * 
 * @author Steve Jones
 *
 */
public class SensorAssignments extends LinkedHashMap<SensorType, Set<SensorAssignment>> {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -7045591929422515240L;

	/**
	 * Simple constructor
	 */
	protected SensorAssignments(List<SensorType> sensorTypes) {
		super(sensorTypes.size());
		for (SensorType type : sensorTypes) {
			put(type, new HashSet<SensorAssignment>());
		}
	}
	
	/**
	 * Determines whether or not a given sensor type must have a column assigned.
	 * 
	 * <p>
	 *   This method checks the current sensor assignments, and determines whether
	 *   or not a given sensor still needs to have an assignment made. This
	 *   requires a number of checks:
	 * </p>
	 * 
	 * <ul>
	 *   <li>
	 *     If a primary sensor has been assigned, then no further assignment is needed
	 *   </li>
	 *   <li>
	 *     If the sensor is part of a required group, and none of the sensors
	 *     in that group have had a sensor assigned, then an assignment is needed.
	 *     If any of the sensors in the group have been assigned, then no
	 *     assignment is needed for this sensor.
	 *   </li>
	 *   <li>
	 *     If the sensor is not part of a required group, but its required 
	 *     flag is set, then a sensor assignment is required.
	 *   </li>
	 *   <li>
	 *     If another sensor type depends on this sensor type, then a
	 *     sensor assignment is required. This determination may be based on
	 *     the answer to {@link SensorType#getDependsQuestion()}.
	 *   </li>
	 * </ul>
	 * @param sensorType The sensor type to be checked
	 * @return {@code true} if a column must be assigned to the sensor; {@code false}
	 * if no assignment is needed.
	 * @throws SensorAssignmentException If the specified sensor type does not exist
	 */
	public boolean isAssignmentRequired(SensorType sensorType) throws SensorAssignmentException {
		boolean result = true;
		
		if (!containsKey(sensorType)) {
			throw new SensorAssignmentException("The specified sensor was not found");
		}
		
		// See if any assignments have already been made.
		// If they have, see if there's at least one primary sensor
		Set<SensorAssignment> assignments = get(sensorType);
		if (null != assignments) {
			for (SensorAssignment assignment : assignments) {
				if (assignment.isPrimary()) {
					result = false;
					break;
				}
			}
		}
		
		// If we haven't found a primary sensor assignment...
		if (result) {
			// Check the required group
			String requiredGroup = sensorType.getRequiredGroup();
			
			if (null != requiredGroup && groupAssigned(requiredGroup)) {
				result = false;
			} else {
				// If any other sensors depend on this one,
				// of this sensor is required, then assignment is needed
				result = (hasDependents(sensorType) || sensorType.isRequired());
			}
		}
		
		return result;
	}
	
	/**
	 * Determines whether or not a named Required Group has been assigned
	 * within any of its member sensor types.
	 * @param requiredGroup The Required Group
	 * @return {@code true} if a sensor has been assigned; {@code false} if no sensor has been assigned.
	 */
	private boolean groupAssigned(String requiredGroup) {
		boolean result = false;
		
		for (SensorType sensorType : keySet()) {
			String sensorGroup = sensorType.getRequiredGroup();
			if (null != sensorGroup && sensorGroup.equalsIgnoreCase(requiredGroup)) {
				Set<SensorAssignment> assignments = get(sensorType);
				if (null != assignments && assignments.size() > 0) {
					result = true;
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Determines whether or not any of the sensor types in the
	 * collection depends on the supplied sensor type.
	 * 
	 * If the sensor type has a Depends Question, this is taken into account
	 * 
	 * @param sensorType The sensor type that other sensors may depend on
	 * @return {@code true} if any other sensor types depend on the supplied sensor type; {@code false} if there are no dependents
	 * @see SensorType#getDependsQuestion()
	 */
	private boolean hasDependents(SensorType sensorType) {
		boolean result = false;
		
		for (SensorType testType : keySet()) {
			if (!testType.equals(sensorType)) {
				String dependsOn = testType.getDependsOn();
				if (null != dependsOn && dependsOn.equalsIgnoreCase(sensorType.getName())) {
					
					// If there's no Depends Question, then we require the sensor
					if (null == testType.getDependsQuestion()) {
						result = true;
					} else {
						// See what the answer to the Depends Question is
						for (SensorAssignment assignment : get(sensorType)) {
							if (assignment.getDependsQuestionAnswer()) {
								result = true;
								break;
							}
						}
					}
					
					break;
				}
			}
		}
		
		return result;
	}
}
