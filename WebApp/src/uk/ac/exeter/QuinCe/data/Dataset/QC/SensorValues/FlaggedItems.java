package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.Set;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;

public class FlaggedItems {

  private Set<SensorValue> sensorValues;

  private Set<ReadOnlyDataReductionRecord> dataReductionRecords;

  public FlaggedItems() {
    sensorValues = new TreeSet<SensorValue>();
    dataReductionRecords = new TreeSet<ReadOnlyDataReductionRecord>();
  }

  public void add(SensorValue sensorValue) {
    sensorValues.add(sensorValue);
  }

  public void add(ReadOnlyDataReductionRecord dataReductionRecord) {
    dataReductionRecords.add(dataReductionRecord);
  }

  public Set<SensorValue> getSensorValues() {
    return sensorValues;
  }

  public Set<ReadOnlyDataReductionRecord> getDataReductionRecords() {
    return dataReductionRecords;
  }

}
