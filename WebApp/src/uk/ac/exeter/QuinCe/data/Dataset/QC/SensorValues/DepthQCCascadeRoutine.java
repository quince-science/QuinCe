package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class DepthQCCascadeRoutine extends PositionQCCascadeRoutine {

  public DepthQCCascadeRoutine(FlagScheme flagScheme) {
    super(flagScheme);
  }

  @Override
  public String getShortMessage() {
    return "Invalid/missing depth";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Invalid/missing depth";
  }

  @Override
  protected long getSensorValueId() {
    return SensorType.DEPTH_ID;
  }

  @Override
  protected String getExpectedValue() {
    return "Any depth";
  }

  @Override
  protected boolean isPosition(SensorValue value) {
    return value.getColumnId() == FileDefinition.DEPTH_COLUMN_ID;
  }
}
