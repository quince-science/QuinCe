package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.Message;

public class ArgoDataSet extends DataSet {

  public ArgoDataSet(Instrument instrument) {
    super(instrument);
    setStart("0");
    setEnd("0");
  }

  public ArgoDataSet(Instrument instrument, String name, int startProfile,
    String start, String end, boolean nrt) {
    super(instrument, name, start, end, nrt);
  }

  public ArgoDataSet(long id, Instrument instrument, String name, String start,
    String end, int status, LocalDateTime statusDate, boolean nrt,
    Map<String, Properties> properties, SensorOffsets sensorOffsets,
    LocalDateTime createdDate, LocalDateTime lastTouched,
    List<Message> errorMessages, DatasetProcessingMessages processingMessages,
    DatasetUserMessages userMessages, double minLon, double minLat,
    double maxLon, double maxLat, boolean exported) {

    super(id, instrument, name, start, end, status, statusDate, nrt, properties,
      sensorOffsets, createdDate, lastTouched, errorMessages,
      processingMessages, userMessages, minLon, minLat, maxLon, maxLat,
      exported);
  }

  public int getStartCycle() {
    return Integer.parseInt(getStart());
  }

  public int getEndCycle() {
    return Integer.parseInt(getEnd());
  }
}
