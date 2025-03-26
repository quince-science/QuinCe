package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.sql.Connection;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class D12D13TotalCO2CheckRoutine extends DataReductionQCRoutine {

  @Override
  public String getShortMessage() {
    return "Total CO₂ value incorrect";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Total CO₂ is not the sum of ¹²CO₂ and ¹³CO₂";
  }

  @Override
  protected void qcAction(Connection conn, Instrument instrument,
    DataSet dataSet, Variable variable,
    TreeMap<Measurement, ReadOnlyDataReductionRecord> dataReductionRecords,
    DatasetSensorValues allSensorValues, FlaggedItems flaggedItems)
    throws RoutineException {

    try {
      SensorType d12CO2SensorType = ResourceManager.getInstance()
        .getSensorsConfiguration().getSensorType("x¹²CO₂ (with standards)");

      SensorType d13CO2SensorType = ResourceManager.getInstance()
        .getSensorsConfiguration().getSensorType("x¹³CO₂ (with standards)");

      SensorType totalCO2SensorType = ResourceManager.getInstance()
        .getSensorsConfiguration()
        .getSensorType("x¹²CO₂ + x¹³CO₂ (with standards)");

      if (instrument.getSensorAssignments().isAssigned(totalCO2SensorType)) {

        for (Map.Entry<Measurement, ReadOnlyDataReductionRecord> entry : dataReductionRecords
          .entrySet()) {

          Measurement measurement = entry.getKey();
          ReadOnlyDataReductionRecord record = entry.getValue();

          MeasurementValue d12CO2 = measurement
            .getMeasurementValue(d12CO2SensorType);
          MeasurementValue d13CO2 = measurement
            .getMeasurementValue(d13CO2SensorType);

          Double totalCO2 = measurement.getMeasurementValue(totalCO2SensorType)
            .getCalculatedValue();

          Double difference = totalCO2
            - (d12CO2.getCalculatedValue() + d13CO2.getCalculatedValue());

          if (Math.abs(difference) > 0.001D) {
            flagSensors(instrument, measurement, record, allSensorValues,
              new RoutineFlag(this, Flag.BAD, "", ""), flaggedItems,
              MeasurementValue.interpolatesAroundFlag(d12CO2, d13CO2));
          }
        }
      }
    } catch (SensorTypeNotFoundException e) {
      throw new RoutineException("Unable to retrieve SensorType details", e);
    }
  }
}
