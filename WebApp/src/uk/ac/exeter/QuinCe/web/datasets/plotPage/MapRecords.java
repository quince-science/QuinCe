package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.apache.commons.lang3.NotImplementedException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;

@SuppressWarnings("serial")
public class MapRecords extends ArrayList<MapRecord> {

  private static final int DECIMATION_LIMIT = 1000;

  private Gson valueGson;

  private Gson flagGson;

  private Gson flagNrtGson;

  private Gson selectionGson;

  private boolean valueRangeCalculated = false;

  private Double min = Double.NaN;

  private Double max = Double.NaN;

  private Double minNoFlags = Double.NaN;

  private Double maxNoFlags = Double.NaN;

  public MapRecords(int size, DatasetSensorValues allSensorValues) {
    super(size);

    valueGson = new GsonBuilder().registerTypeHierarchyAdapter(MapRecord.class,
      new MapRecordJsonSerializer(MapRecordJsonSerializer.VALUE,
        allSensorValues))
      .create();
    flagGson = new GsonBuilder().registerTypeHierarchyAdapter(MapRecord.class,
      new MapRecordJsonSerializer(MapRecordJsonSerializer.FLAG,
        allSensorValues))
      .create();
    flagNrtGson = new GsonBuilder()
      .registerTypeHierarchyAdapter(MapRecord.class,
        new MapRecordJsonSerializer(MapRecordJsonSerializer.FLAG_IGNORE_NEEDED,
          allSensorValues))
      .create();
    selectionGson = new GsonBuilder()
      .registerTypeHierarchyAdapter(MapRecord.class,
        new MapRecordJsonSerializer(MapRecordJsonSerializer.SELECTION,
          allSensorValues))
      .create();
  }

  public String getDisplayJson(GeoBounds bounds, List<Long> selectedRows,
    boolean useNeededFlags, boolean hideNonGoodFlags,
    DatasetSensorValues allSensorValues) {

    Set<MapRecord> boundedRecords = new TreeSet<MapRecord>();
    List<MapRecord> data = new ArrayList<MapRecord>();
    Set<MapRecord> flags = new TreeSet<MapRecord>();
    List<MapRecord> selection = new ArrayList<MapRecord>();

    if (size() > 0) {
      MapRecord minLon = get(0);
      MapRecord maxLon = get(0);
      MapRecord minLat = get(0);
      MapRecord maxLat = get(0);

      // Find the records within the specified bounds, and also record the
      // records
      // closest to the bound limits.
      for (MapRecord record : this) {

        if (!hideNonGoodFlags || record.isGood(allSensorValues)
          || record.flagNeeded()) {
          if (bounds.inBounds(record.position)) {
            if (record.position.getLongitude() < minLon.position
              .getLongitude()) {
              minLon = record;
            } else if (record.position.getLongitude() > maxLon.position
              .getLongitude()) {
              maxLon = record;
            }

            if (record.position.getLatitude() < minLat.position.getLatitude()) {
              minLat = record;
            } else if (record.position.getLatitude() > maxLat.position
              .getLatitude()) {
              maxLat = record;
            }

            boundedRecords.add(record);
          }
        }
      }

      // Decimate the chosen records
      Set<MapRecord> decimated = new HashSet<MapRecord>();
      List<MapRecord> selected = new ArrayList<MapRecord>();

      if (boundedRecords.size() <= DECIMATION_LIMIT) {
        decimated.addAll(boundedRecords);
      } else {
        int nth = (int) Math.floor(boundedRecords.size() / DECIMATION_LIMIT);
        int count = 0;
        for (MapRecord record : boundedRecords) {
          count++;

          if (count % nth == 0 || !record.isGood(allSensorValues)) {
            decimated.add(record);
          }

          if (selectedRows.contains(record.getRowId())) {
            selected.add(record);
          }
        }
      }

      decimated.add(minLon);
      decimated.add(maxLon);
      decimated.add(minLat);
      decimated.add(maxLat);

      for (MapRecord record : decimated) {
        data.add(record);
        if (showAsFlag(record, useNeededFlags, allSensorValues)) {
          flags.add(record);
        }
      }

      for (MapRecord record : selected) {
        selection.add(record);
        if (showAsFlag(record, useNeededFlags, allSensorValues)) {
          flags.add(record);
        }
      }
    }

    JsonArray json = new JsonArray();

    json.add(valueGson.toJsonTree(makeFeatureCollection(valueGson, data)));

    if (useNeededFlags) {
      json.add(flagGson.toJsonTree(makeFeatureCollection(flagGson, flags)));
    } else {
      json
        .add(flagNrtGson.toJsonTree(makeFeatureCollection(flagNrtGson, flags)));
    }

    json.add(selectionGson
      .toJsonTree(makeFeatureCollection(selectionGson, selection)));

    return json.toString();
  }

  private boolean showAsFlag(MapRecord record, boolean useNeededFlag,
    DatasetSensorValues allSensorValues) {
    return (useNeededFlag && record.flagNeeded())
      || !record.isGood(allSensorValues);
  }

  private JsonObject makeFeatureCollection(Gson gson,
    Collection<MapRecord> points) {
    JsonObject object = new JsonObject();
    object.addProperty("type", "FeatureCollection");
    object.add("features", gson.toJsonTree(points));
    return object;
  }

  private void resetRange() {
    valueRangeCalculated = false;
  }

  private void calculateValueRange(DatasetSensorValues allSensorValues) {
    min = Double.NaN;
    max = Double.NaN;
    minNoFlags = Double.NaN;
    maxNoFlags = Double.NaN;

    forEach(r -> {
      Double value = r.getValue();

      if (!value.isNaN()) {

        if (min.isNaN()) {
          min = value;
          max = value;
        }

        if (value < min) {
          min = value;
        }

        if (value > max) {
          max = value;
        }

        if (r.isGood(allSensorValues)) {
          if (minNoFlags.isNaN()) {
            minNoFlags = value;
            maxNoFlags = value;
          }

          if (value < minNoFlags) {
            minNoFlags = value;
          }

          if (value > maxNoFlags) {
            maxNoFlags = value;
          }

        }

        if (min.isNaN()) {
          min = value;
          max = value;
        } else {
          if (r.getValue() < min) {
            min = r.getValue();
          }
          if (r.getValue() > max) {
            max = r.getValue();
          }
        }
      }
    });

    valueRangeCalculated = true;
  }

  /**
   * Get the minimum and maximum value.
   *
   * <p>
   * If the minimum and maximum are equal, they are offset by a minimum amount
   * (±0.001) to ensure the map colour scale is rendered properly.
   * </p>
   *
   * @return The minimum and maximum value.
   */
  public Double[] getValueRange(DatasetSensorValues allSensorValues,
    boolean hideFlags) {
    if (!valueRangeCalculated) {
      calculateValueRange(allSensorValues);
    }

    Double outMin;
    Double outMax;

    if (!hideFlags) {
      outMin = min;
      outMax = max;
    } else {
      outMin = minNoFlags;
      outMax = maxNoFlags;
    }

    if (outMin == outMax) {
      outMin = outMin - 0.001D;
      outMax = outMax + 0.001D;
    }

    return new Double[] { outMin, outMax };
  }

  public GeoBounds getBounds(DatasetSensorValues allSensorValues,
    boolean hideNonGoodFlags) {
    double minLon = Double.MAX_VALUE;
    double maxLon = -Double.MAX_VALUE;
    double minLat = Double.MAX_VALUE;
    double maxLat = -Double.MAX_VALUE;

    for (MapRecord record : this) {
      if (!hideNonGoodFlags || record.isGood(allSensorValues)
        || record.flagNeeded()) {
        double lat = record.position.getLatitude();
        double lon = record.position.getLongitude();

        if (lon < minLon) {
          minLon = lon;
        }

        if (lon > maxLon) {
          maxLon = lon;
        }

        if (lat < minLat) {
          minLat = lat;
        }

        if (lat > maxLat) {
          maxLat = lat;
        }
      }
    }

    return new GeoBounds(minLon, maxLon, minLat, maxLat);
  }

  @Override
  public void add(int index, MapRecord record) {
    throw new NotImplementedException();
  }

  @Override
  public boolean add(MapRecord record) {
    resetRange();
    boolean result;

    // Don't add NaN values
    if (record.isNaN()) {
      result = false;
    } else {
      result = super.add(record);
    }

    return result;
  }

  @Override
  public boolean addAll(int index, Collection<? extends MapRecord> records) {
    throw new NotImplementedException();
  }

  @Override
  public boolean addAll(Collection<? extends MapRecord> records) {
    throw new NotImplementedException();
  }

  @Override
  public MapRecord remove(int index) {
    throw new NotImplementedException();
  }

  @Override
  public boolean remove(Object o) {
    throw new NotImplementedException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new NotImplementedException();
  }

  @Override
  public boolean removeIf(Predicate<? super MapRecord> filter) {
    throw new NotImplementedException();
  }

  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    throw new NotImplementedException();
  }
}
