package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

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
public class SensorAssignments extends LinkedHashMap<SensorType, Iterable<SensorAssignment>>{

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
}
