package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.List;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.data.Dataset.NewSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;

/**
 * A data extraction worker implementation, used by {@link ExtractDataSetJob}.
 */
public abstract class DataSetExtractor {

  protected Set<DataFile> usedFiles = null;

  protected NewSensorValues sensorValues = null;

  protected GeoBounds geoBounds = null;

  /**
   * Perform the extraction work on the supplied {@link DataSet}.
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The {@link Insturment} to which the {@link DataSet} belongs.
   * @param dataSet
   *          The {@link DataSet} to be processed.
   */
  public abstract void extract(Connection conn, Instrument instrument,
    DataSet dataSet) throws Exception;

  /**
   * Retrieve the source {@link DataFiles} which are used in the
   * {@link DataSet}.
   *
   * @return The used files.
   */
  public Set<DataFile> getUsedFiles() {
    return usedFiles;
  }

  /**
   * Get the extracted {@link SensorValue}s.
   *
   * @return The sensor values.
   */
  public NewSensorValues getSensorValues() {
    return sensorValues;
  }

  public abstract String getJobName();

  /**
   * Get the geographical bounds of the dataset.
   *
   * <p>
   * May be null if the dataset contains no geographical data.
   * </p>
   *
   * @return The bounds.
   */
  public GeoBounds getGeoBounds() {
    return geoBounds;
  }

  /**
   * Extract the longitude from a file line and add it to the extraction.
   *
   * <p>
   * The method automatically adds the latitude to the {@link #sensorValues} and
   * {@link #geoBounds}, so the caller does not need to do anything further. The
   * longitude is still returned in case the extractor needs it for other
   * purposes.
   * </p>
   *
   * @param dataSet
   *          The {@link DataSet} being processed.
   * @param file
   *          The file that the line belongs to.
   * @param lineNumber
   *          The line number in the file.
   * @param line
   *          The line data.
   * @param coordinate
   *          The {@link Coordinate} for the line.
   * @return The extracted longitude.
   */
  protected String extractLongitude(DataSet dataSet, DataFile file,
    int lineNumber, List<String> line, Coordinate coordinate) {

    String longitude = null;
    try {
      longitude = file.getLongitude(line);
    } catch (PositionException e) {
      dataSet.addProcessingMessage(getJobName(), file, lineNumber, e);
    }

    if (null != longitude) {
      sensorValues.create(FileDefinition.LONGITUDE_COLUMN_ID, coordinate,
        longitude);

      // Update the dataset bounds
      try {
        geoBounds.addLon(Double.parseDouble(longitude));
      } catch (NumberFormatException e) {
        // Ignore it now. QC will pick it up later.
      }
    }

    return longitude;
  }

  protected String extractLatitude(DataSet dataSet, DataFile file,
    int lineNumber, List<String> line, Coordinate coordinate) {

    String latitude = null;
    try {
      latitude = file.getLatitude(line);
    } catch (PositionException e) {
      dataSet.addProcessingMessage(getJobName(), file, lineNumber, e);
    }

    if (null != latitude) {
      sensorValues.create(FileDefinition.LATITUDE_COLUMN_ID, coordinate,
        latitude);

      // Update the dataset bounds
      try {
        geoBounds.addLat(Double.parseDouble(latitude));
      } catch (NumberFormatException e) {
        // Ignore it now. QC will pick it up later.
      }
    }

    return latitude;
  }
}
