package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.UnderwayMarine12_13Pco2Reducer;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class D12D13CMeasurementValueCalculator
  extends DefaultMeasurementValueCalculator {

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    Measurement measurement, Variable variable, SensorType requiredSensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException {

    MeasurementValue result;

    String calGasType = variable.getAttributes()
      .get(UnderwayMarine12_13Pco2Reducer.CAL_GAS_TYPE_ATTR);

    try {
      switch (calGasType) {
      case UnderwayMarine12_13Pco2Reducer.TOTAL_CO2_GAS_CAL_TYPE: {

        switch (requiredSensorType.getShortName()) {
        case "x¹²CO₂ (with standards)":
        case "x¹³CO₂ (with standards)": {
          result = super.getSensorValue(instrument, dataSet, measurement,
            variable, requiredSensorType, allMeasurements, allSensorValues,
            false, conn);
          break;
        }
        case "x¹²CO₂ + x¹³CO₂ (with standards)": {
          result = super.getSensorValue(instrument, dataSet, measurement,
            variable, requiredSensorType, allMeasurements, allSensorValues,
            true, conn);
          break;
        }
        default: {
          throw new MeasurementValueCalculatorException(
            "Unrecognised Sensor Type " + requiredSensorType.getShortName());
        }
        }

        break;
      }
      case UnderwayMarine12_13Pco2Reducer.SPLIT_CO2_GAS_CAL_TYPE: {
        switch (requiredSensorType.getShortName()) {
        case "x¹²CO₂ (with standards)":
        case "x¹³CO₂ (with standards)": {
          result = super.getSensorValue(instrument, dataSet, measurement,
            variable, requiredSensorType, allMeasurements, allSensorValues,
            true, conn);
          break;
        }
        case "x¹²CO₂ + x¹³CO₂ (with standards)": {
          if (instrument.getSensorAssignments()
            .isAssigned(requiredSensorType)) {

            result = super.getSensorValue(instrument, dataSet, measurement,
              variable, requiredSensorType, allMeasurements, allSensorValues,
              false, conn);
          } else {
            result = null;
          }

          break;
        }
        default: {
          throw new MeasurementValueCalculatorException(
            "Unrecognised Sensor Type " + requiredSensorType.getShortName());
        }
        }

        break;
      }
      default: {
        throw new MeasurementValueCalculatorException("Unrecognised "
          + UnderwayMarine12_13Pco2Reducer.CAL_GAS_TYPE_ATTR + " calGasType");
      }
      }

    } catch (Exception e) {
      throw new MeasurementValueCalculatorException("Error calculating value",
        e);
    }

    return result;
  }
}
