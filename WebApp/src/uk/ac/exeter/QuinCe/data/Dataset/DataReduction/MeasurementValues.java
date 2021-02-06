package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@Deprecated
@SuppressWarnings("serial")
public class MeasurementValues
  extends HashMap<SensorType, LinkedHashSet<MeasurementValue>> {

  private final Instrument instrument;

  private final Measurement measurement;

  public MeasurementValues(Instrument instrument, Measurement measurement) {
    super();
    this.instrument = instrument;
    this.measurement = measurement;
  }

  public Instrument getInstrument() {
    return instrument;
  }

  public void put(SensorType sensorType, MeasurementValue value) {
    if (!containsKey(sensorType)) {
      put(sensorType, new LinkedHashSet<MeasurementValue>());
    }

    get(sensorType).add(value);
  }

  public Double getValue(SensorType sensorType,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer,
    ValueCalculators valueCalculators, Connection conn) throws Exception {

    return getValue(sensorType.getName(), allMeasurements, allSensorValues,
      reducer, valueCalculators, conn);
  }

  public Double getValue(String sensorType,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer,
    ValueCalculators valueCalculators, Connection conn) throws Exception {

    return valueCalculators.calculateValue(this, sensorType, allMeasurements,
      allSensorValues, reducer, conn);
  }

  public Measurement getMeasurement() {
    return measurement;
  }

  public List<MeasurementValue> getAllValues() {
    List<MeasurementValue> result = new ArrayList<MeasurementValue>();
    values().forEach(result::addAll);
    return result;
  }

  public void loadSensorValues(DatasetSensorValues allSensorValues,
    SensorType sensorType, boolean goodFlagsOnly)
    throws RoutineException, SensorTypeNotFoundException, MissingParamException,
    CloneNotSupportedException {

    Set<SensorType> children = ResourceManager.getInstance()
      .getSensorsConfiguration().getChildren(sensorType);

    if (children.size() > 0) {
      for (SensorType child : children) {
        loadSensorValuesAction(allSensorValues, child, goodFlagsOnly);
      }
    } else {
      loadSensorValuesAction(allSensorValues, sensorType, goodFlagsOnly);
    }

  }

  private void loadSensorValuesAction(DatasetSensorValues allSensorValues,
    SensorType sensorType, boolean goodFlagsOnly)
    throws RoutineException, SensorTypeNotFoundException, MissingParamException,
    CloneNotSupportedException {

    /*
     * 
     * // If we've already loaded the sensor type, don't bother doing it again
     * if (!containsKey(sensorType)) {
     * 
     * put(sensorType, new LinkedHashSet<MeasurementValue>());
     * 
     * for (long columnId : instrument.getSensorAssignments()
     * .getColumnIds(sensorType)) {
     * 
     * SearchableSensorValuesList columnValues = allSensorValues
     * .getColumnValues(columnId);
     * 
     * MeasurementValue measurementValue = new MeasurementValue(measurement,
     * sensorType, columnId);
     * columnValues.populateMeasurementValue(measurementValue, goodFlagsOnly);
     * 
     * put(sensorType, measurementValue); }
     * 
     * // If this SensorType depends on another, add that too. SensorType
     * dependsOn = instrument.getSensorAssignments() .getDependsOn(sensorType);
     * 
     * if (null != dependsOn) { loadSensorValues(allSensorValues, dependsOn,
     * goodFlagsOnly); } }
     * 
     */
  }
}
