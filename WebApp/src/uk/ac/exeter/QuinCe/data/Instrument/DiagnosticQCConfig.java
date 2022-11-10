package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;

@SuppressWarnings("serial")
public class DiagnosticQCConfig
  extends TreeMap<SensorAssignment, DiagnosticSensorQCConfig> {

  public void setAssignedRunTypes(SensorAssignment diagnosticSensor,
    SensorAssignment measurementSensor, List<String> assignedRunTypes) {

    if (!containsKey(diagnosticSensor)) {
      put(diagnosticSensor, new DiagnosticSensorQCConfig());
    }

    get(diagnosticSensor).setMeasurementSensorRunTypes(measurementSensor,
      assignedRunTypes);
  }

  public List<String> getAssignedRunTypes(SensorAssignment diagnosticSensor,
    SensorAssignment measurmentSensor) {

    List<String> result = new ArrayList<String>();

    if (containsKey(diagnosticSensor)) {
      DiagnosticSensorQCConfig sensorConfig = get(diagnosticSensor);
      result = sensorConfig.getMeasurementSensorRunTypes(measurmentSensor);
    }

    return result;
  }

  public boolean hasAssignedRunTypes(SensorAssignment diagnosticSensor,
    SensorAssignment measurementSensor) {
    return getAssignedRunTypes(diagnosticSensor, measurementSensor).size() > 0;
  }

  public boolean hasAssignedRunTypes(SensorAssignment diagnosticSensor) {
    DiagnosticSensorQCConfig sensorConfig = get(diagnosticSensor);
    return null == sensorConfig ? false : sensorConfig.anyRunTypeAssigned();
  }

  public Double getRangeMin(SensorAssignment diagnosticSensor) {
    return containsKey(diagnosticSensor) ? get(diagnosticSensor).getRangeMin()
      : null;
  }

  public void setRangeMin(SensorAssignment diagnosticSensor, Double min) {
    if (!containsKey(diagnosticSensor)) {
      put(diagnosticSensor, new DiagnosticSensorQCConfig());
    }

    get(diagnosticSensor).setRangeMin(min);
  }

  public Double getRangeMax(SensorAssignment diagnosticSensor) {
    return containsKey(diagnosticSensor) ? get(diagnosticSensor).getRangeMax()
      : null;
  }

  public void setRangeMax(SensorAssignment diagnosticSensor, Double max) {
    if (!containsKey(diagnosticSensor)) {
      put(diagnosticSensor, new DiagnosticSensorQCConfig());
    }

    get(diagnosticSensor).setRangeMax(max);
  }

  public boolean isInRange(SensorAssignment diagnosticSensor, double value) {
    boolean result;

    if (!containsKey(diagnosticSensor)) {
      result = true;
    } else {
      result = get(diagnosticSensor).rangeOK(value);
    }

    return result;
  }
}
