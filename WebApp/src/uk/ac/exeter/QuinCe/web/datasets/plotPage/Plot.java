package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;

/**
 * Information about a plot on the Plot Page
 *
 */
public class Plot {

  /**
   * Mode indicator for plot
   */
  public static final String MODE_PLOT = "plot";

  /**
   * Mode indicator for map
   */
  public static final String MODE_MAP = "map";

  /**
   * The bean to which this plot belongs
   */
  private PlotPageBean parentBean;

  /**
   * The current mode
   */
  private String mode = MODE_PLOT;

  /**
   * The variable to use on the X Axis in Plot 1.
   */
  private Field xAxis;

  /**
   * The variables to use on the Y Axis in Plot 1.
   */
  private Field yAxis;

  /**
   * The plot data
   */
  private String data;

  /**
   * The variable to show on the map
   */
  private Field mapVariable;

  /**
   * Flag to indicate that the map scale should be updated
   */
  private boolean mapUpdateScale = false;

  /**
   * The map data
   */
  private String mapData;

  /**
   * The bounds of the map display.
   */
  private GeoBounds mapBounds = null;

  /**
   * The scale limits for the left map
   */
  private List<Double> mapScaleLimits = null;

  /**
   * Basic constructor
   *
   * @param parentBean
   *          The bean to which this plot belongs
   * @param mapBounds
   *          The bounds of the map display
   * @param xAxis
   *          The x axis variable
   * @param yAxis
   *          The y axis variables
   * @param mapVariable
   *          The map variable
   */
  public Plot(PlotPageBean parentBean, GeoBounds mapBounds, Field xAxis,
    Field yAxis, Field mapVariable) {
    this.parentBean = parentBean;
    this.mapBounds = mapBounds;
    this.xAxis = xAxis;
    this.yAxis = yAxis;
    this.mapVariable = mapVariable;

    mapScaleLimits = new ArrayList<Double>(2);
    mapScaleLimits.add(0.0);
    mapScaleLimits.add(0.0);
  }

  /**
   * @return the mode
   */
  public String getMode() {
    return mode;
  }

  /**
   * @param mode
   *          the mode to set
   */
  public void setMode(String mode) {
    this.mode = mode;
  }

  /**
   * @return the X Axis ID
   */
  public long getXAxis() {
    return xAxis.getId();
  }

  public Field getXAxisField() {
    return xAxis;
  }

  /**
   * @param xAxis
   *          the xAxis to set
   */
  public void setXAxis(long xAxisId) {
    // A -1 value means no change is required
    if (-1 != xAxisId) {
      this.xAxis = parentBean.getFieldSets().getField(xAxisId);
    }
  }

  /**
   * @return the Y Axis ID
   */
  public long getYAxis() {
    return yAxis.getId();
  }

  public Field getYAxisField() {
    return yAxis;
  }

  /**
   * @param yAxis
   *          the yAxis to set
   */
  public void setYAxis(long yAxisId) {
    this.yAxis = parentBean.getFieldSets().getField(yAxisId);
  }

  /**
   * @return the mapVariable
   */
  public long getMapVariable() {
    return mapVariable.getId();
  }

  public Field getMapVariableField() {
    return mapVariable;
  }

  /**
   * @param mapVariable
   *          the mapVariable to set
   */
  public void setMapVariable(long mapVariableId) {
    this.mapVariable = parentBean.getFieldSets().getField(mapVariableId);
    ;
  }

  /**
   * Get the plot data
   *
   * @return The plot data
   */
  public String getData() {
    return data;
  }

  // TODO This is horrible. Come up with an alternative.
  // But we need a way to stop the data coming from the front end
  // Which is doable with 'execute' directives

  /**
   * Dummy method for JSF - does nothing
   *
   * @param data
   *          The data from JSF - ignored
   */
  public void setData(String data) {
    // Do nothing
  }

  // TODO This is horrible. Come up with an alternative.
  // But we need a way to stop the labels coming from the front end

  /**
   * Get the data labels for the plot
   *
   * @return The data labels
   */
  public String getLabels() {
    StringBuilder result = new StringBuilder();

    // TODO Make this through GSON
    result.append('[');

    switch (mode) {
    case MODE_PLOT: {

      // TODO Remove the magic strings.
      result.append('"');
      result.append(xAxis.getName());
      result.append("\",\"ID\",\"QC Flag\",\"");
      result.append(parentBean.getGhostDataLabel());
      result.append("\",\"");
      result.append(yAxis.getName());
      result.append('"');
      break;
    }
    case MODE_MAP: {
      // TODO Remove the magic strings.
      result.append('"');
      result.append("Longitude");
      result.append("\",\"");
      result.append("Longitude");
      result.append("\",\"");
      result.append("Date/Time");
      result.append("\",\"");
      result.append("ID");
      result.append("\",\"");
      result.append("QC Flag");
      result.append("\",\"");
      result.append(mapVariable.getName());
      result.append('"');
    }
    }

    result.append(']');

    return result.toString();
  }

  /**
   * Dummy method for the front end
   *
   * @param labels
   *          Ignored
   */
  public void setLabels(String labels) {
    // Do nothing
  }

  /**
   * Update the plot data
   */
  public void updatePlot() {
    try {
      data = parentBean.getData().getPlotData(xAxis, yAxis);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @return the updateScale
   */
  public boolean getMapUpdateScale() {
    return mapUpdateScale;
  }

  /**
   * @param mapUpdateScale
   *          the updateScale to set
   */
  public void setMapUpdateScale(boolean mapUpdateScale) {
    this.mapUpdateScale = mapUpdateScale;
  }

  /**
   * @return the mapData
   */
  public String getMapData() {
    return mapData;
  }

  /**
   * @param mapData
   *          the mapData to set; ignored
   */
  public void setMapData(String mapData) {
    // Do nothing
  }

  /**
   * @return the mapBounds
   */
  public String getMapBounds() {
    return mapBounds.toJson();
  }

  /**
   * @param mapBounds
   *          the mapBounds to set
   */
  public void setMapBounds(String mapBounds) {
    this.mapBounds = new GeoBounds(mapBounds);
  }

  /**
   * @return the mapBounds
   */
  public String getMapScaleLimits() {
    return '[' + StringUtils.collectionToDelimited(mapScaleLimits, ",") + ']';
  }

  /**
   * @param mapScaleLimits
   *          the scaleLimits to set
   */
  public void setMapScaleLimits(String mapScaleLimits) {
    // Do nothing
  }

  public void generateMapData() {
    try {
      mapScaleLimits = parentBean.getData().getValueRange(mapVariable);
      mapData = parentBean.getData().getMapData(mapVariable, mapBounds);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
