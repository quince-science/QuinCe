package uk.ac.exeter.QuinCe.web;

import java.util.ArrayList;
import java.util.List;

/**
 * An entry in a variable tree
 * @see VariableList
 * @author Steve Jones
 *
 */
public class Variable {

	/**
	 * Indicator for base types (date, lon, lat)
	 */
	public static final int TYPE_BASE = 0;
	
	/**
	 * Indicator for Sensor types
	 */
	public static final int TYPE_SENSOR = 1;
	
	/**
	 * Indicator for Calculation types
	 */
	public static final int TYPE_CALCULATION = 2;
	
	/**
	 * The ID of this tree entry
	 */
	private int id = -1;
	
	/**
	 * The entry type - either SENSOR or CALCULATION
	 */
	private int type;
	
	/**
	 * The human-readable label
	 */
	private String label;
	
	/**
	 * The database field name
	 */
	private String fieldName;
	
	/**
	 * Constructor for a top-level tree entry
	 * @param type The entry type
	 * @param label The label
	 * @param fieldName The database field name
	 */
	public Variable(int type, String label, String fieldName) {
		this.type = type;
		this.label = label;
		this.fieldName = fieldName;
	}
	
	/**
	 * Set the entry's ID
	 * @param id The ID
	 */
	protected void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Get the entry's ID
	 * @return The ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Get the human-readable label
	 * @return The label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Get the database field name
	 * @return The field name
	 */
	public String getFieldName() {
		return fieldName;
	}
	
	/**
	 * Get the type
	 * @return The type
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Get a list of variable IDs from a list of variables
	 * @param variables The variables
	 * @return The variable IDs
	 */
	public static List<Integer> getIds(List<Variable> variables) {
		
		List<Integer> ids = new ArrayList<Integer>(variables.size());
		
		for (Variable variable : variables) {
			ids.add(variable.getId());
		}
		
		return ids;
	}
}
