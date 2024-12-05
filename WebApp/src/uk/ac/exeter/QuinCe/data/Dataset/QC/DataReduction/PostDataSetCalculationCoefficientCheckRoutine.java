package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.sql.Connection;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class PostDataSetCalculationCoefficientCheckRoutine
  extends DataReductionQCRoutine {

  @Override
  public String getShortMessage() {
    return "Missing post-calibration";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Missing post-calibration";
  }

  @Override
  protected void qcAction(Connection conn, Instrument instrument,
    DataSet dataSet, Variable variable,
    TreeMap<Measurement, ReadOnlyDataReductionRecord> dataReductionRecords,
    DatasetSensorValues allSensorValues, FlaggedItems flaggedItems)
    throws RoutineException {

    try {
      // See if we have a post-calibration for the dataset
      CalibrationSet calculationCoefficients = CalculationCoefficientDB
        .getInstance().getCalibrationSet(conn, dataSet);
      if (!calculationCoefficients.hasCompletePost()) {
        for (DataReductionRecord record : dataReductionRecords.values()) {
          record.setQc(Flag.NOT_CALIBRATED, getShortMessage());
        }
      }

      flaggedItems.addDataReductionRecords(dataReductionRecords.values());
    } catch (DataReductionException e) {
      throw new RoutineException("Error setting QC flags", e);
    } catch (Exception e) {
      throw new RoutineException("Error getting calibration information", e);
    }
  }
}
