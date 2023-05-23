package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Instrument.DiagnosticQCConfig;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Automatic QC routine for diagnostic sensors.
 *
 * <p>
 * Currently this routine just checks values against the range set by the user.
 * </p>
 */
public class DiagnosticsQCRoutine {

  public void run(Instrument instrument, DatasetSensorValues sensorValues,
    RunTypePeriods runTypePeriods)
    throws InvalidFlagException, RecordNotFoundException {

    DiagnosticQCConfig diagnosticConfig = instrument.getDiagnosticQCConfig();

    for (SensorAssignment sensor : instrument.getSensorAssignments()
      .getDiagnosticSensors()) {

      if (diagnosticConfig.hasRange(sensor)) {

        Double rangeMin = diagnosticConfig.getRangeMin(sensor);
        Double rangeMax = diagnosticConfig.getRangeMax(sensor);

        SearchableSensorValuesList values = sensorValues
          .getColumnValues(sensor.getDatabaseId());

        for (SensorValue value : values) {
          boolean bad = false;

          if (!value.isNaN()) {
            if (null != rangeMin && value.getDoubleValue() < rangeMin) {
              bad = true;
            } else if (null != rangeMax && value.getDoubleValue() > rangeMax) {
              bad = true;
            }
          }

          if (bad) {
            value.setUserQC(Flag.BAD,
              instrument.getSensorAssignments().getSensorTypeForDBColumn(
                sensor.getDatabaseId()) + " out of range");
            sensorValues.applyQCCascade(value, runTypePeriods);
          }
        }
      }
    }
  }
}
