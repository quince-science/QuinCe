package uk.ac.exeter.QuinCe.data.Dataset.QC;

public interface Routine {
  public String getName();

  public String getShortMessage();

  public String getLongMessage(RoutineFlag flag);
}
