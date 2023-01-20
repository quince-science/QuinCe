package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.List;

import com.google.gson.JsonArray;
import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.utils.StringUtils;

public class GeoBounds {

  private double minLon;
  private double maxLon;
  private double minLat;
  private double maxLat;

  public GeoBounds(double minLon, double maxLon, double minLat, double maxLat) {
    this.minLon = minLon;
    this.maxLon = maxLon;
    this.minLat = minLat;
    this.maxLat = maxLat;
    normalise();
  }

  public GeoBounds(String boundsString) {
    List<Double> boundsList = StringUtils.delimitedToDoubleList(boundsString,
      ",");
    this.minLon = boundsList.get(0);
    this.maxLon = boundsList.get(2);
    this.minLat = boundsList.get(1);
    this.maxLat = boundsList.get(3);
    normalise();
  }

  public boolean inBounds(LatLng position) {
    boolean result = true;

    if (null == position) {
      result = false;
    } else if (position.getLongitude() < minLon
      || position.getLongitude() > maxLon || position.getLatitude() < minLat
      || position.getLatitude() > maxLat) {

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
    JsonArray json = new JsonArray();
    json.add(minLon);
    json.add(minLat);
    json.add(maxLon);
    json.add(maxLat);
    json.add(getMidLon());
    json.add(getMidLat());
    return json.toString();
  }

  public String toString() {
    return "[[" + minLon + "," + minLat + "],[" + maxLon + "," + maxLat + "]]";
  }

  private void normalise() {
    minLon = normaliseLon(minLon);
    maxLon = normaliseLon(maxLon);
    if (maxLon < minLon) {
      double temp = minLon;
      minLon = maxLon;
      maxLon = temp;
    }
  }

  private double normaliseLon(double lon) {
    return lon;
  }
}
