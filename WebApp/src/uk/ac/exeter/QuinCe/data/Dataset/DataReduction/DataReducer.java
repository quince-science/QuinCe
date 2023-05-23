package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A DataReducer will perform all data reduction calculations for a given
 * variable. The output from the data reduction is an instance of the
 * DataReductionRecord class
 *
 * @author Steve Jones
 *
 */
public abstract class DataReducer {

  /**
   * The variable that this reducer works on
   */
  protected Variable variable;

  /**
   * The variable attributes
   */
  protected Map<String, Properties> properties;

  public DataReducer(Variable variable, Map<String, Properties> properties) {

    this.variable = variable;
    this.properties = properties;
  }

  /**
   * Perform the data reduction and set up the QC flags
   *
   * @param instrument
   *          The instrument that took the measurement
   * @param measurement
   *          The measurement
   * @param measurementValues
   *          The measurement's sensor values
   * @param allMeasurements
   *          All measurements for the data set
   * @return The data reduction result
   */
  public DataReductionRecord performDataReduction(Instrument instrument,
    Measurement measurement, Connection conn) throws Exception {

    DataReductionRecord record = new DataReductionRecord(measurement, variable,
      getCalculationParameterNames());

    doCalculation(instrument, measurement, record, conn);

    Flag cascadeFlag = Flag.GOOD;
    LinkedHashMap<SensorType, String> messages = new LinkedHashMap<SensorType, String>();

    // Apply QC flags to the data reduction records
    for (SensorType sensorType : variable.getAllSensorTypes(true)) {

      MeasurementValue value = measurement.getMeasurementValue(sensorType);

      if (null != value) {
        // Collect all QC messages together. Do not record the same message from
        // multiple sources.
        Flag valueFlag = variable.getCascade(value.getSensorType(),
          value.getQcFlag(), instrument.getSensorAssignments());

        if (!valueFlag.isGood()) {
          if (valueFlag.moreSignificantThan(cascadeFlag)) {
            cascadeFlag = valueFlag;
          }

          for (String qcMessage : value.getQcMessages()) {
            if (!messages.containsValue(qcMessage)) {
              messages.put(sensorType, qcMessage);
            }
          }
        }

      }
    }

    List<String> qcMessages = messages.entrySet().stream()
      .map(e -> e.getKey().getShortName() + " " + e.getValue()).toList();

    record.setQc(cascadeFlag, qcMessages);

    return record;
  }

  /**
   * Perform the data reduction calculations
   *
   * @param instrument
   *          The instrument that took the measurement
   * @param measurement
   *          The measurement
   * @param sensorValues
   *          The measurement's sensor values
   * @param allMeasurements
   *          All measurements for the data set
   * @param record
   *          The data reduction result
   */
  public abstract void doCalculation(Instrument instrument,
    Measurement measurement, DataReductionRecord record, Connection conn)
    throws Exception;

  /**
   * Set the state for a non-calculated record (used for unused run types etc)
   *
   * @param record
   *          The record
   * @throws DataReductionException
   */
  protected void makeEmptyRecord(DataReductionRecord record)
    throws DataReductionException {
    record.setQc(Flag.NO_QC, new ArrayList<String>());
  }

  /**
   * Set a data reduction record's state for a missing required parameter
   *
   * @param record
   *          The record
   * @param missingParameterName
   *          The name of the missing parameter
   * @throws DataReductionException
   */
  protected void makeMissingParameterRecord(DataReductionRecord record,
    List<SensorType> missingTypes) throws DataReductionException {

    List<String> qcMessages = new ArrayList<String>(missingTypes.size());

    for (String parameter : getCalculationParameterNames()) {
      record.put(parameter, Double.NaN);
    }

    for (SensorType type : missingTypes) {
      qcMessages.add("Missing " + type.getShortName());
    }

    record.setQc(Flag.NO_QC, qcMessages);
  }

  /**
   * Get the calculation parameters generated by the reducer, in display order
   *
   * @return The calculation parameters
   */
  public List<String> getCalculationParameterNames() {
    return getCalculationParameters().stream()
      .map(CalculationParameter::getShortName).collect(Collectors.toList());
  }

  /**
   * Get the list of SensorTypes required by this data reducer. This takes the
   * minimum list of sensor types (or parent types) and determines the actual
   * required types according to the sensor types assigned to the instrument and
   * their dependents.
   *
   * @param instrumentAssignments
   *          The sensor types assigned to the instrument
   * @param sensorTypeNames
   *          The names of the bare minimum sensor types
   * @return The complete list of required SensorType objects
   * @throws SensorTypeNotFoundException
   */
  protected Set<SensorType> getRequiredSensorTypes(
    SensorAssignments instrumentAssignments)
    throws DataReductionException, SensorTypeNotFoundException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();
    List<SensorType> sensorTypes = sensorConfig
      .getSensorTypes(getRequiredTypeStrings());

    Set<SensorType> result = new HashSet<SensorType>(sensorTypes.size());

    try {
      for (SensorType baseSensorType : sensorTypes) {

        if (sensorConfig.isParent(baseSensorType)) {
          Set<SensorType> childSensorTypes = sensorConfig
            .getChildren(baseSensorType);
          if (!addAnySensorTypesAndDependsOn(result, childSensorTypes,
            instrumentAssignments)) {
            throw new DataReductionException(
              "No assignments present for children of Sensor Type "
                + baseSensorType.getShortName() + " or their dependents");
          }
        } else {
          if (!addSensorTypeAndDependsOn(result, baseSensorType,
            instrumentAssignments)) {
            throw new DataReductionException(
              "No assignments present for Sensor Type "
                + baseSensorType.getShortName() + " or its dependents");
          }
        }
      }
    } catch (SensorTypeNotFoundException e) {
      throw new DataReductionException("Named sensor type not found", e);
    } catch (SensorConfigurationException e) {
      throw new DataReductionException("Invalid sensor configuration detected",
        e);
    }

    return result;
  }

  /**
   * Add a set of Sensor Types to an existing list of Sensor Types, including
   * any dependents
   *
   * @param list
   *          The list to which the sensor types are to be added
   * @param typesToAdd
   *          The sensor types to add
   * @param instrumentAssignments
   *          The instrument's sensor assignments
   * @return {@code true} if at least one Sensor Type is added; {@code false} if
   *         none are added (unless the list is empty)
   * @throws SensorConfigurationException
   * @throws SensorTypeNotFoundException
   */
  private boolean addAnySensorTypesAndDependsOn(Set<SensorType> list,
    Set<SensorType> typesToAdd, SensorAssignments instrumentAssignments)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    boolean result = false;

    if (typesToAdd.size() == 0) {
      result = true;
    } else {
      for (SensorType add : typesToAdd) {
        if (addSensorTypeAndDependsOn(list, add, instrumentAssignments)) {
          result = true;
        }
      }
    }

    return result;
  }

  /**
   * Add a Sensor Type to an existing list of Sensor Types, including any
   * dependents
   *
   * @param list
   *          The list to which the sensor types are to be added
   * @param typesToAdd
   *          The sensor types to add
   * @param instrumentAssignments
   *          The instrument's sensor assignments
   * @throws SensorConfigurationException
   * @throws SensorTypeNotFoundException
   */
  private boolean addSensorTypeAndDependsOn(Set<SensorType> list,
    SensorType typeToAdd, SensorAssignments instrumentAssignments)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    boolean result = true;

    if (!instrumentAssignments.isAssigned(typeToAdd)) {
      result = false;
    } else {
      list.add(typeToAdd);
      SensorType dependsOn = instrumentAssignments.getDependsOn(typeToAdd);
      if (null != dependsOn) {
        if (!addSensorTypeAndDependsOn(list, dependsOn,
          instrumentAssignments)) {
          result = false;
        }
      }
    }

    return result;
  }

  public Float getFloatProperty(String property) {
    Float result = null;

    try {
      if (properties.containsKey(variable.getName())) {
        String stringProp = properties.get(variable.getName())
          .getProperty(property);
        if (null != stringProp) {
          result = Float.parseFloat(stringProp);
        }
      }
    } catch (NumberFormatException e) {
      ExceptionUtils.printStackTrace(e);
      // Swallow the exception so that the result is null
    }

    return result;
  }

  protected abstract String[] getRequiredTypeStrings();

  public abstract List<CalculationParameter> getCalculationParameters();

  protected long makeParameterId(int parameterNumber) {
    return DataReducerFactory.makeParameterId(variable, parameterNumber);
  }

  /**
   * Perform preprocessing actions on the dataset
   */
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements) throws Exception {
    // The default is to do nothing
  }
}
