package uk.ac.exeter.QuinCe.web.datasets.export;

import java.time.LocalDateTime;

import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

/**
 * An exported dataset ready to be written to a file. Contains the file contents
 * and some metadata.
 */
public class DatasetExport {

  private StringBuilder content;

  private byte[] contentBytes;

  private int recordCount = 0;

  private double minLon = Double.MAX_VALUE;

  private double maxLon = Double.MIN_VALUE;

  private double minLat = Double.MAX_VALUE;

  private double maxLat = Double.MIN_VALUE;

  private LocalDateTime startDate = null;

  private LocalDateTime endDate = null;

  protected void append(String text) {
    content.append(text);
    contentBytes = null;
  }

  protected DatasetExport() {
    this.content = new StringBuilder();
  }

  protected void append(char character) {
    content.append(character);
    contentBytes = null;
  }

  protected void append(int integer) {
    content.append(integer);
    contentBytes = null;
  }

  protected byte[] getContent() {
    if (null == contentBytes) {
      contentBytes = content.toString().getBytes();
    }

    return contentBytes;
  }

  protected int getLength() {
    return getContent().length;
  }

  /**
   * Register a value with this export. Performs any required actions.
   *
   * <p>
   * Position values are added to the geographical bounds.
   * </p>
   *
   * @param value
   *          The value being registered
   */
  protected void registerValue(long columnId, PlotPageTableValue value) {

    if (null != value && null != value.getValue()
      && !value.getQcFlag().equals(Flag.FLUSHING)) {
      if (columnId == FileDefinition.TIME_COLUMN_ID) {

        // The first date we receive is the start date
        if (null == startDate) {
          startDate = (LocalDateTime) value.getRawValue();
        }

        // Assume monotonic time, so the latest time received is also the last
        // time
        endDate = (LocalDateTime) value.getRawValue();
      } else if (columnId == FileDefinition.LONGITUDE_COLUMN_ID) {
        try {
          double doubleValue = Double.parseDouble(value.getValue());
          addLon(doubleValue);
        } catch (NumberFormatException e) {
          // Ignore
        }
      } else if (columnId == FileDefinition.LATITUDE_COLUMN_ID) {
        try {
          double doubleValue = Double.parseDouble(value.getValue());
          addLat(doubleValue);
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
  }

  private void addLon(double lon) {
    if (!Double.isNaN(lon)) {
      if (lon < minLon) {
        minLon = lon;
        if (maxLon == Double.MIN_VALUE) {
          maxLon = lon;
        }
      }

      if (lon > maxLon) {
        maxLon = lon;
        if (minLon == Double.MAX_VALUE) {
          minLon = lon;
        }
      }
    }
  }

  private void addLat(double lat) {
    if (!Double.isNaN(lat)) {
      if (lat < minLat) {
        minLat = lat;
        if (maxLat == Double.MIN_VALUE) {
          maxLat = lat;
        }
      }

      if (lat > maxLat) {
        maxLat = lat;
        if (minLat == Double.MAX_VALUE) {
          minLat = lat;
        }
      }
    }
  }

  protected int getRecordCount() {
    return recordCount;
  }

  protected void addRecord() {
    recordCount++;
  }

  protected LocalDateTime getStartDate() {
    return startDate;
  }

  protected LocalDateTime getEndDate() {
    return endDate;
  }

  protected JsonObject getBoundsJson() {
    JsonObject boundsObject = new JsonObject();
    boundsObject.addProperty("south", minLat);
    boundsObject.addProperty("west", minLon);
    boundsObject.addProperty("east", maxLon);
    boundsObject.addProperty("north", maxLat);
    return boundsObject;
  }
}
