package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.sql.Connection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.Range;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class DeltaTRangeRoutine extends DataReductionQCRoutine {

  protected DeltaTRangeRoutine(FlagScheme flagScheme,
    Map<Flag, Range<Double>> limits) {
    super(flagScheme, limits);
  }

  @Override
  protected void qcAction(Connection conn, Instrument instrument,
    DataSet dataSet, Variable variable,
    TreeMap<Measurement, ReadOnlyDataReductionRecord> dataReductionRecords,
    DatasetSensorValues allSensorValues, FlaggedItems flaggedItems)
    throws RoutineException {

    for (Map.Entry<Measurement, ReadOnlyDataReductionRecord> entry : dataReductionRecords
      .entrySet()) {

      Measurement measurement = entry.getKey();
      ReadOnlyDataReductionRecord record = entry.getValue();

      Double value = record.getCalculationValue("ΔT");
      if (null != value) {
        RoutineFlag flag = null;

        if (value < -0.1) {
          flag = new RoutineFlag(instrument.getFlagScheme(), this,
            instrument.getFlagScheme().getBadFlag(), "-0.1",
            String.valueOf(value));
        } else {
          flag = getRangeFlag(value);
        }

        if (null != flag && !flagScheme.isGood(flag, true)) {
          flagSensors(instrument, measurement, record, allSensorValues, flag,
            flaggedItems, false);
        }
      }
    }
  }

  @Override
  public String getShortMessage() {
    return "ΔT out of range";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "ΔT out of range: is " + flag.getActualValue() + ", should be ±"
      + flag.getRequiredValue();
  }
}
