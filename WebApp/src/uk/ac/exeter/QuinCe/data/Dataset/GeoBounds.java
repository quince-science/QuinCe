package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.List;

import com.google.gson.JsonArray;
import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * A geographical bounding box.
 */
public class GeoBounds {

  /**
   * The western limit.
   */
  private double minLon;

  /**
   * The eastern limit.
   */
  private double maxLon;

  /**
   * The southern limit.
   */
  private double minLat;

  /**
   * The northern limit.
   */
  private double maxLat;

  /**
   * Create an 'empty' bounds.
   *
   * <p>
   * The limits are initialised to invalid limits. {@link #addLon} and
   * {@link #addLat} must each be called at least once to generate a valid
   * bounds object. Until this is done, the behaviour of the object is
   * undefined.
   * </p>
   */
  public GeoBounds() {
    this.minLon = Double.MAX_VALUE;
    this.maxLon = -Double.MAX_VALUE;
    this.minLat = Double.MAX_VALUE;
    this.maxLat = -Double.MAX_VALUE;
  }

  /**
   * Create a bounds with the specified limits.
   *
   * @param minLon
   *          The western limit.
   * @param maxLon
   *          The eastern limit.
   * @param minLat
   *          The southern limit.
   * @param maxLat
   *          The northern limit.
   */
  public GeoBounds(double minLon, double maxLon, double minLat, double maxLat) {
    this.minLon = minLon;
    this.maxLon = maxLon;
    this.minLat = minLat;
    this.maxLat = maxLat;
  }

  /**
   * Generate a bounds from a {@link String}.
   *
   * <p>
   * The string must be of the form {@code minLon, minLat, maxLon, maxLat}. Any
   * other format will result in errors or undefined behaviour.
   * </p>
   *
   * @param boundsString
   *          The bounds string.
   */
  public GeoBounds(String boundsString) {
    List<Double> boundsList = StringUtils.delimitedToDoubleList(boundsString,
      ",");
    this.minLon = boundsList.get(0);
    this.maxLon = boundsList.get(2);
    this.minLat = boundsList.get(1);
    this.maxLat = boundsList.get(3);
  }

  /**
   * Determine whether or not the specified position is within the limits of
   * this bounds object.
   *
   * <p>
   * The limits of the bounds are inclusive.
   * </p>
   *
   * @param position
   *          The position.
   * @return {@code} true if the position is within the bounds; {@code false} if
   *         it is not.
   */
  public boolean inBounds(LatLng position) {
    boolean result = true;

    if (null == position) {
      result = false;
    } else if (position.getLongitude() <= minLon
      || position.getLongitude() >= maxLon || position.getLatitude() <= minLat
      || position.getLatitude() >= maxLat) {

      result = false;
    }

    return result;
  }

  /**
   * Get the longitude of the mid-point of the bounds.
   *
   * @return The mid-point longitude.
   */
  public double getMidLon() {
    return (maxLon - minLon) / 2 + minLon;
  }

  /**
   * Get the latitude of the mid-point of the bouds.
   *
   * @return The mid-point latitude.
   */
  public double getMidLat() {
    return (maxLat - minLat) / 2 + minLat;
  }

  /**
   * Return the bounds as a JSON array.
   *
   * <p>
   * The elements are in the order {@code minLon, minLat, maxLon, maxLat}.
   * </p>
   *
   * @return The JSON array.
   */
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

  /**
   * Adjust the bounds to encompass the specified longitude.
   *
   * <p>
   * If the longitude is outside the range {@code -180:180} it is ignored.
   * </p>
   *
   * @param lon
   *          The longitude.
   */
  public void addLon(double lon) {
    if (lon >= -180D && lon < minLon) {
      minLon = lon;
    }

    if (lon <= 180D && lon > maxLon) {
      maxLon = lon;
    }
  }

  /**
   * Adjust the bounds to encompass the specified longitude.
   *
   * <p>
   * If the longitude is outside the range {@code -180:180} it is ignored.
   * </p>
   *
   * @param lon
   *          The longitude.
   */
  public void addLat(double lat) {
    if (lat >= -90D && lat < minLat) {
      minLat = lat;
    }

    if (lat <= 90D && lat > maxLat) {
      maxLat = lat;
    }
  }

  /**
   * Get the western limit.
   *
   * @return The western limit.
   */
  public double getMinLon() {
    return minLon;
  }

  /**
   * Get the eastern limit.
   *
   * @return The eastern limit.
   */
  public double getMaxLon() {
    return maxLon;
  }

  /**
   * Get the southern limit.
   *
   * @return The southern limit.
   */
  public double getMinLat() {
    return minLat;
  }

  /**
   * Get the northern limit.
   *
   * @return The northern limit.
   */
  public double getMaxLat() {
    return maxLat;
  }

  public String toString() {
    return "[[" + minLon + "," + minLat + "],[" + maxLon + "," + maxLat + "]]";
  }
}
