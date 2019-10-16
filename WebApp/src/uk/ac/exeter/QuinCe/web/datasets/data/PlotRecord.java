package uk.ac.exeter.QuinCe.web.datasets.data;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class PlotRecord implements Comparable<PlotRecord> {

  private double xValue;
  private long id;
  private int flag;
  private double yValue;
  private boolean ghost;

  protected PlotRecord(double xValue, long id, FieldValue value, boolean nrt) {
    this.xValue = xValue;
    this.id = id;

    if (!nrt && value.needsFlag()) {
      this.flag = Flag.VALUE_NEEDED;
    } else {
      this.flag = value.getQcFlag().getFlagValue();
    }

    this.yValue = value.getValue();
    this.ghost = value.isGhost();
  }

  protected JSONArray toJsonArray() {
    // TODO Convert to GSON
    JSONArray json = new JSONArray();
    json.put(xValue);
    json.put(id);
    json.put(flag);

    if (ghost) {
      json.put(yValue);
      json.put(JSONObject.NULL);
    } else {
      json.put(JSONObject.NULL);
      json.put(yValue);
    }

    return json;
  }

  @Override
  public int compareTo(PlotRecord o) {
    // Sort on the X axis
    return Double.compare(xValue, o.xValue);
  }
}
