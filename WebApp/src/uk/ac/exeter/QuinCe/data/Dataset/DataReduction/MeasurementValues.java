package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@SuppressWarnings("serial")
public class MeasurementValues
  extends HashMap<SensorType, LinkedHashSet<MeasurementValue>> {

  private Instrument instrument;

  private Measurement measurement;

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
    Map<Long, SearchableSensorValuesList> allSensorValues, DataReducer reducer,
    Connection conn) throws Exception {

    return getValue(sensorType.getName(), allMeasurements, allSensorValues,
      reducer, conn);
  }

  public Double getValue(String sensorType,
    Map<String, ArrayList<Measurement>> allMeasurements,
    Map<Long, SearchableSensorValuesList> allSensorValues, DataReducer reducer,
    Connection conn) throws Exception {

    return ValueCalculators.getInstance().calculateValue(this, sensorType,
      allMeasurements, allSensorValues, reducer, conn);
  }

  public Measurement getMeasurement() {
    return measurement;
  }

  public List<MeasurementValue> getAllValues() {
    List<MeasurementValue> result = new ArrayList<MeasurementValue>();
    values().forEach(result::addAll);
    return result;
  }

  public void loadSensorValues(
    Map<Long, SearchableSensorValuesList> allSensorValues,
    SensorType sensorType)
    throws RoutineException, SensorTypeNotFoundException {

    Set<SensorType> children = ResourceManager.getInstance()
      .getSensorsConfiguration().getChildren(sensorType);

    if (children.size() > 0) {
      for (SensorType child : children) {
        loadSensorValuesAction(allSensorValues, child);
      }
    } else {
      loadSensorValuesAction(allSensorValues, sensorType);
    }

  }

  private void loadSensorValuesAction(
    Map<Long, SearchableSensorValuesList> allSensorValues,
    SensorType sensorType)
    throws RoutineException, SensorTypeNotFoundException {
    // If we've already loaded the sensor type, don't bother doing it again
    if (!containsKey(sensorType)) {

      put(sensorType, new LinkedHashSet<MeasurementValue>());

      for (long columnId : instrument.getSensorAssignments()
        .getColumnIds(sensorType)) {

        SearchableSensorValuesList columnValues = allSensorValues.get(columnId);

        MeasurementValue measurementValue = columnValues
          .getMeasurementValue(measurement, sensorType, columnId);

        put(sensorType, measurementValue);
      }

      // If this SensorType depends on another, add that too.
      SensorType dependsOn = instrument.getSensorAssignments()
        .getDependsOn(sensorType);

      if (null != dependsOn) {
        loadSensorValues(allSensorValues, dependsOn);
      }

    }
  }

}
