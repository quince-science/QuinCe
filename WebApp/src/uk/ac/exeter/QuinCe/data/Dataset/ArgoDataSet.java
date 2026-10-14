package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.Message;

/**
 * Instance of the {@link DataSet} class for Argo data.
 * 
 * <p>
 * This class operates identically to the base {@link DataSet} class, except
 * that the {@code start} and {@code end} are defined as Argo cycle numbers
 * instead of timestamps.
 * </p>
 */
public class ArgoDataSet extends DataSet {

  /**
   * Creates an empty {@link DataSet} associated with a specified
   * {@link Instrument}.
   * 
   * @param instrument
   *          The Instrument.
   */
  public ArgoDataSet(Instrument instrument) {
    super(instrument);
    setStart("0");
    setEnd("0");
  }

  /**
   * Constructor for all fields.
   *
   * @param id
   *          Data set's database ID.
   * @param instrument
   *          The Instrument to which the data set belongs.
   * @param name
   *          Dataset name.
   * @param start
   *          Start coordinate.
   * @param end
   *          End coordinate.
   * @param status
   *          The current status.
   * @param statusDate
   *          The date that the status was set.
   * @param nrt
   *          Indicates whether or not this is a NRT dataset.
   * @param properties
   *          Additional properties.
   * @param sensorOffsets
   *          The sensor offsets for the dataset.
   * @param createdDate
   *          Date that the dataset was created.
   * @param lastTouched
   *          Date that the dataset was last accessed.
   * @param errorMessages
   *          List of error messages concerning the dataset.
   * @param processingMessages
   *          List of error messages raised during dataset processing.
   * @param userMessages
   *          Messages set by the user for the dataset.
   * @param minLon
   *          The minimum longitude of the dataset's geographical bounds.
   * @param minLat
   *          The minimum latitude of the dataset's geographical bounds.
   * @param maxLon
   *          The maximum longitude of the dataset's geographical bounds.
   * @param maxLat
   *          The maximum latitude of the dataset's geographical bounds.
   * @param exported
   *          Indicates whether or not the dataset has been exported.
   */
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

  /**
   * Get the dataset's start cycle.
   * 
   * @return The start cycle.
   */
  public int getStartCycle() {
    return Integer.parseInt(getStart());
  }

  /**
   * Get the dataset's end cycle.
   * 
   * @return The end cycle.
   */
  public int getEndCycle() {
    return Integer.parseInt(getEnd());
  }
}
