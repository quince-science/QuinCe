package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.sql.Connection;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class DeltaTRangeRoutine extends DataReductionQCRoutine {

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

        if (value < 0) {
          flag = new RoutineFlag(this, Flag.BAD, "0", String.valueOf(value));
        } else if (value > settings.getDoubleOption("bad_limit")) {
          flag = new RoutineFlag(this, Flag.BAD,
            settings.getOption("bad_limit"), String.valueOf(value));

          // flag = Flag.BAD;
        } else if (value > settings.getDoubleOption("questionable_limit")) {
          flag = new RoutineFlag(this, Flag.QUESTIONABLE,
            settings.getOption("questionable_limit"), String.valueOf(value));
        }

        if (null != flag) {
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
