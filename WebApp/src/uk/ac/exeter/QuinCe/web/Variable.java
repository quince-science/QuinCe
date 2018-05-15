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
   * Indicator for Diagnostic types
   */
  public static final int TYPE_DIAGNOSTIC = 3;

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
   * Indicates whether or not this variable can be used on the X Axis of plots
   */
  private boolean canUseOnXAxis = true;

  /**
   * Indicates whether or not this variable can be used on the Y Axis of plots
   */
  private boolean canUseOnYAxis = true;

  /**
   * Indicates whether or not this variable will be visible in the selection
   * dialog. This has limited uses at this time, so use with care!
   */
  private boolean visible = true;

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
   * Constructor for a top-level tree entry
   * @param type The entry type
   * @param label The label
   * @param fieldName The database field name
   * @param canUseOnXAxis Indicates whether or not this variable can be used on the X Axis of plots
   * @param canUseOnYAxis Indicates whether or not this variable can be used on the Y Axis of plots
   */
  public Variable(int type, String label, String fieldName, boolean canUseOnXAxis, boolean canUseOnYAxis, boolean visible) {
    this.type = type;
    this.label = label;
    this.fieldName = fieldName;
    this.canUseOnXAxis = canUseOnXAxis;
    this.canUseOnYAxis = canUseOnYAxis;
    this.visible = visible;
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

  /**
   * Determine whether or not this variable can be shown on the X axis of plots
   * @return {@code true} if the variable can be shown on the X axis; {@code false} if it cannot
   */
  public boolean getCanUseOnXAxis() {
    return canUseOnXAxis;
  }

  /**
   * Determine whether or not this variable can be shown on the Y axis of plots
   * @return {@code true} if the variable can be shown on the Y axis; {@code false} if it cannot
   */
  public boolean getCanUseOnYAxis() {
    return canUseOnYAxis;
  }

  /**
   * Determine whether or not this variable should be visible in the
   * selection dialog
   * @return {@code true} if the variable is visible; {@code false} if it is not
   */
  public boolean getVisible() {
    return visible;
  }
}
