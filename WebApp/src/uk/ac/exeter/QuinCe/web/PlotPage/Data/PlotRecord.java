package uk.ac.exeter.QuinCe.web.PlotPage.Data;

import org.primefaces.json.JSONArray;

public class PlotRecord implements Comparable<PlotRecord> {

  private double xValue;
  private long id;
  private int flag;
  private double yValue;

  protected PlotRecord(double xValue, long id, int flag, double yValue) {
    this.xValue = xValue;
    this.id = id;
    this.flag = flag;
    this.yValue = yValue;
  }

  protected JSONArray toJsonArray() {
    // TODO Convert to GSON
    JSONArray json = new JSONArray();
    json.put(xValue);
    json.put(id);
    json.put(flag);
    json.put(yValue);
    return json;
  }

  @Override
  public int compareTo(PlotRecord o) {
    // Sort on the X axis
    return Double.compare(xValue, o.xValue);
  }
}
