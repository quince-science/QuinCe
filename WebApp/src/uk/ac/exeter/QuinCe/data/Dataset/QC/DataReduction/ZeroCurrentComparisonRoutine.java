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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ZeroCurrentComparisonRoutine extends DataReductionQCRoutine {

  @Override
  public String getShortMessage() {
    return "Current count above zero count";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Current count should be below zero count";
  }

  @Override
  protected void qcAction(Connection conn, Instrument instrument,
    DataSet dataSet, Variable variable,
    TreeMap<Measurement, ReadOnlyDataReductionRecord> dataReductionRecords,
    DatasetSensorValues allSensorValues, FlaggedItems flaggedItems)
    throws RoutineException {

    try {
      // All values detected by this routine get the same flag
      RoutineFlag flag = new RoutineFlag(this, Flag.BAD, null, null);

      SensorType zeroCountType = ResourceManager.getInstance()
        .getSensorsConfiguration().getSensorType("ProOceanus Zero Count");
      SensorType currentCountType = ResourceManager.getInstance()
        .getSensorsConfiguration().getSensorType("ProOceanus Current Count");

      for (Map.Entry<Measurement, ReadOnlyDataReductionRecord> entry : dataReductionRecords
        .entrySet()) {

        Measurement measurement = entry.getKey();

        MeasurementValue zeroCount = measurement
          .getMeasurementValue(zeroCountType);
        MeasurementValue currentValue = measurement
          .getMeasurementValue(currentCountType);

        if (currentValue.getCalculatedValue() >= zeroCount
          .getCalculatedValue()) {
          flagSensors(instrument, measurement, entry.getValue(),
            allSensorValues, flag, flaggedItems,
            MeasurementValue.interpolatesAroundFlag(zeroCount, currentValue));
        }
      }

    } catch (Exception e) {
      throw new RoutineException(e);
    }
  }

}
