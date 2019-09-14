package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.List;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QuinCe.utils.StringUtils;

public class GeoBounds {

  public final double minLon;
  public final double maxLon;
  public final double minLat;
  public final double maxLat;

  public GeoBounds(double minLon, double maxLon, double minLat, double maxLat) {
    this.minLon = minLon;
    this.maxLon = maxLon;
    this.minLat = minLat;
    this.maxLat = maxLat;
  }

  public GeoBounds(String boundsString) {
    List<Double> boundsList = StringUtils.delimitedToDoubleList(
      boundsString.substring(1, boundsString.length() - 1), ",");
    this.minLon = boundsList.get(0);
    this.maxLon = boundsList.get(2);
    this.minLat = boundsList.get(1);
    this.maxLat = boundsList.get(3);
  }

  public boolean inBounds(Position position) {
    boolean result = true;

    if (position.lon < minLon || position.lon > maxLon || position.lat < minLat
      || position.lat > maxLat) {

      result = false;
    }

    return result;
  }

  public double getMidLon() {
    return (maxLon - minLon) / 2 + minLon;
  }

  public double getMidLat() {
    return (maxLat - minLat) / 2 + minLat;
  }

  public String toJson() {
    // TODO Convert to GSON
    JSONArray json = new JSONArray();
    json.put(minLon);
    json.put(minLat);
    json.put(maxLon);
    json.put(maxLat);
    json.put(getMidLon());
    json.put(getMidLat());
    return json.toString();
  }
}
