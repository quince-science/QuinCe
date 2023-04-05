package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Node data for the column assignments tree control.
 *
 * The Assignments Tree has different types of data in different nodes. The
 * PrimeFaces tree objects can't deal with this, so this is a wrapper class to
 * hold all the possible types of data and give back the right information.
 */
public class AssignmentsTreeNodeData {

  /**
   * Indicates that this node is a simple string.
   */
  protected static final int STRING_TYPE = 0;

  /**
   * Indicates that this node is for a variable.
   */
  protected static final int VARIABLE_TYPE = 1;

  /**
   * Indicates that this node is for a SensorType
   */
  protected static final int SENSORTYPE_TYPE = 2;

  /**
   * Indicates that this node is for a SensorAssignment
   */
  protected static final int SENSORASSIGNMENT_TYPE = 3;

  /**
   * Indicates that this node is for a data file
   */
  protected static final int FILE_TYPE = 4;

  /**
   * Indicates that this node is for a combined data file and SensorAssignment
   */
  protected static final int DATETIMEASSIGNMENT_TYPE = 5;

  /**
   * Indicates that this node is for a combined data file and position spec
   */
  protected static final int POSITIONSPEC_TYPE = 6;

  /**
   * The type of data held in this object
   */
  private final int dataType;

  /**
   * String data
   */
  private final String stringData;

  /**
   * Variable
   */
  private final Variable variable;

  /**
   * Sensor Type
   */
  private final SensorType sensorType;

  /**
   * All Sensor Assignments
   */
  private final SensorAssignments assignments;

  /**
   * A Sensor Assignment
   */
  private final SensorAssignment assignment;

  /**
   * Data file
   */
  private final FileDefinitionBuilder file;

  /**
   * Date/time assignment
   */
  private final DateTimeColumnAssignment dateTimeAssignment;

  /**
   * Position specification
   */
  private final PositionSpecification positionSpec;

  protected AssignmentsTreeNodeData(String string) {
    dataType = STRING_TYPE;
    this.stringData = string;
    this.variable = null;
    this.sensorType = null;
    this.assignments = null;
    this.assignment = null;
    this.file = null;
    this.dateTimeAssignment = null;
    this.positionSpec = null;
  }

  protected AssignmentsTreeNodeData(Variable variable,
    SensorAssignments assignments) {
    dataType = VARIABLE_TYPE;
    this.stringData = null;
    this.variable = variable;
    this.sensorType = null;
    this.assignments = assignments;
    this.assignment = null;
    this.file = null;
    this.dateTimeAssignment = null;
    this.positionSpec = null;
  }

  protected AssignmentsTreeNodeData(SensorType sensorType,
    SensorAssignments assignments) {
    dataType = SENSORTYPE_TYPE;
    this.stringData = null;
    this.variable = null;
    this.sensorType = sensorType;
    this.assignments = assignments;
    this.assignment = null;
    this.file = null;
    this.dateTimeAssignment = null;
    this.positionSpec = null;
  }

  protected AssignmentsTreeNodeData(SensorAssignment assignment) {
    dataType = SENSORASSIGNMENT_TYPE;
    this.stringData = null;
    this.variable = null;
    this.sensorType = null;
    this.assignments = null;
    this.assignment = assignment;
    this.file = null;
    this.dateTimeAssignment = null;
    this.positionSpec = null;
  }

  protected AssignmentsTreeNodeData(FileDefinitionBuilder file) {
    dataType = FILE_TYPE;
    this.stringData = null;
    this.variable = null;
    this.sensorType = null;
    this.assignments = null;
    this.assignment = null;
    this.file = file;
    this.dateTimeAssignment = null;
    this.positionSpec = null;
  }

  protected AssignmentsTreeNodeData(FileDefinitionBuilder file,
    DateTimeColumnAssignment assignment) {
    dataType = DATETIMEASSIGNMENT_TYPE;
    this.stringData = null;
    this.variable = null;
    this.sensorType = null;
    this.assignments = null;
    this.assignment = null;
    this.file = file;
    this.dateTimeAssignment = assignment;
    this.positionSpec = null;
  }

  protected AssignmentsTreeNodeData(FileDefinitionBuilder file,
    PositionSpecification positionSpec) {
    dataType = POSITIONSPEC_TYPE;
    this.stringData = null;
    this.variable = null;
    this.sensorType = null;
    this.assignments = null;
    this.assignment = null;
    this.file = file;
    this.dateTimeAssignment = null;
    this.positionSpec = positionSpec;
  }

  protected int getDataType() {
    return dataType;
  }

  protected SensorAssignment getAssignment() {
    return assignment;
  }
}
