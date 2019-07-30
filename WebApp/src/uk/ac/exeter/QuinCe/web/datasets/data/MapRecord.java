package uk.ac.exeter.QuinCe.web.datasets.data;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QuinCe.data.Dataset.Position;
import uk.ac.exeter.QuinCe.utils.MathUtils;

public class MapRecord implements Comparable<MapRecord> {

  protected final Position position;
  protected final long id;
  protected final FieldValue value;

  protected MapRecord(Position position, long id, FieldValue value) {
    this.position = position;
    this.id = id;
    this.value = value;
  }

  @Override
  public int compareTo(MapRecord o) {
    return Long.compare(id, o.id);
  }

  public JSONArray getJsonArray() {
    JSONArray json = new JSONArray();
    json.put(MathUtils.round(position.lon, 3)); // Lon
    json.put(MathUtils.round(position.lat, 3)); // Lat
    json.put(id); // Date/Time (doubles as ID)
    json.put(value.getQcFlag().getFlagValue()); // Flag
    json.put(MathUtils.round(value.getValue(), 3)); // Value
    return json;
  }
}
