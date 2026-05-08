package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.web.datasets.plotPage.DataLatLng;

/**
 * Represents an Argo cycle.
 *
 * <p>
 * A Cycle is a subset of an {@link ArgoCoordinate}, containing just the cycle
 * number, direction and profile.
 * </p>
 *
 * <p>
 * This class exists because a profile is the granularity at which users will
 * work with Argo data.
 * </p>
 */
public class ArgoProfile {

  private final int cycleNumber;

  private final char direction;

  private final int nProf;

  private LocalDateTime time = null;

  private DataLatLng position = null;

  private static Gson gson = new Gson();

  /**
   * Create a profile from an {@link ArgoCoordinate}.
   *
   * @param coordinate
   *          The source coordinate.
   */
  public ArgoProfile(ArgoCoordinate coordinate) {
    this.cycleNumber = coordinate.getCycleNumber();
    this.direction = coordinate.getDirection();
    this.nProf = coordinate.getNProf();
    this.time = coordinate.getTime();
  }

  public String toJson() {
    return gson.toJson(this);
  }

  public boolean matches(ArgoCoordinate coordinate) {
    return coordinate.getCycleNumber() == cycleNumber
      && coordinate.getDirection() == direction
      && coordinate.getNProf() == nProf;
  }

  public int getCycleNumber() {
    return cycleNumber;
  }

  public char getDirection() {
    return direction;
  }

  public int getNProf() {
    return nProf;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public void setTime(LocalDateTime time) {
    this.time = time;
  }

  public DataLatLng getPosition() {
    return position;
  }

  public void setPosition(DataLatLng position) {
    this.position = position;
  }
}
