package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

@SuppressWarnings("serial")
public class DiagnosticQCConfig
  extends TreeMap<SensorAssignment, DiagnosticSensorQCConfig> {

  public void setAssignedVariables(SensorAssignment diagnosticSensor,
    SensorAssignment measurementSensor, List<Variable> assignedVariables) {

    if (!containsKey(diagnosticSensor)) {
      put(diagnosticSensor, new DiagnosticSensorQCConfig());
    }

    get(diagnosticSensor).setMeasurementSensorVariables(measurementSensor,
      assignedVariables);
  }

  public TreeSet<Variable> getAssignedVariables(
    SensorAssignment diagnosticSensor, SensorAssignment measurmentSensor) {

    TreeSet<Variable> result = new TreeSet<Variable>();

    if (containsKey(diagnosticSensor)) {
      DiagnosticSensorQCConfig sensorConfig = get(diagnosticSensor);
      result = sensorConfig.getMeasurementSensorVariables(measurmentSensor);
    }

    return result;
  }

  public boolean hasAssignedVariables(SensorAssignment diagnosticSensor,
    SensorAssignment measurementSensor) {
    return getAssignedVariables(diagnosticSensor, measurementSensor).size() > 0;
  }
}
