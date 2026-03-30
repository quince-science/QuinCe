package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.MathUtils;

public class Plot {

  private static Gson Y2_GSON;

  /**
   * The source data for the plot
   */
  protected final PlotPageData data;

  /**
   * Indicates whether or not NEEDED flags should be displayed.
   *
   * If {@code false}, the automatic QC flag is used.
   */
  protected final boolean useNeededFlags;

  /**
   * The column ID of the X axis
   */
  protected PlotPageColumnHeading xAxis = null;

  /**
   * The column ID of the Y axis
   */
  protected PlotPageColumnHeading yAxis = null;

  /**
   * The column ID of the second Y axis
   */
  protected PlotPageColumnHeading y2Axis = null;

  /**
   * The plot values.
   */
  protected LinkedHashSet<PlotValue> plotValues = null;

  /**
   * Indicates whether or not values that have been flagged during QC should be
   * hidden from the plot.
   */
  private boolean hideFlags = false;

  static {
    Y2_GSON = new GsonBuilder()
      .registerTypeAdapter(PlotValue.class, new Y2AxisPlotValueSerializer())
      .create();
  }

  /**
   * Constructor with minimum information.
   *
   * @param data
   *          The data for the plot.
   * @param xAxis
   *          The initial X axis ID
   * @param yAxis
   *          The initial Y axis ID
   * @throws Exception
   */
  protected Plot(PlotPageData data, PlotPageColumnHeading xAxis,
    PlotPageColumnHeading yAxis, boolean useNeededFlags) throws Exception {
    this.data = data;
    this.xAxis = xAxis;
    this.yAxis = yAxis;
    this.useNeededFlags = useNeededFlags;
  }

  /**
   * Get the x Axis label.
   *
   * @return The x axis label.
   */
  public long getXaxis() {
    return null == xAxis ? Long.MIN_VALUE : xAxis.getId();
  }

  public void setXaxis(long xAxis) throws Exception {
    if (xAxis != 0) {
      this.xAxis = data.getColumnHeading(xAxis);
    }
  }

  /**
   * Get the y Axis.
   *
   * @return The y axis.
   */
  public long getYaxis() {
    return null == yAxis ? 0 : yAxis.getId();
  }

  /**
   * Get the variable being displayed in the plot. This is usually the Y Axis.
   *
   * @return The variable being displayed in the plot.
   */
  public long getDisplayVariable() {
    return getYaxis();
  }

  public void setYaxis(long yAxis) throws Exception {
    if (yAxis != 0) {
      this.yAxis = data.getColumnHeading(yAxis);
    }
  }

  /**
   * Get the y Axis.
   *
   * @return The y axis.
   */
  public long getY2axis() {
    return null == y2Axis ? 0 : y2Axis.getId();
  }

  public void setY2axis(long y2Axis) throws Exception {
    if (y2Axis != 0) {
      this.y2Axis = data.getColumnHeading(y2Axis);
    } else {
      this.y2Axis = null;
    }
  }

  /**
   * Get the JSON data for the main plot
   *
   * @return The main plot data.
   * @throws Exception
   */
  public String getMainData() {
    String result = "[]";

    if (null != plotValues) {
      Gson gson = new GsonBuilder().registerTypeAdapter(PlotValue.class,
        new MainPlotValueSerializer(null != y2Axis)).create();

      result = gson.toJson(plotValues.stream()
        .filter(f -> !f.xNull() && !hideFlags ? true
          : (null == f.getFlag()
            || data.getFlagScheme().isGood(f.getFlag(), true)
            || f.getFlag().equals(FlagScheme.NEEDED_FLAG)))
        .collect(Collectors.toList()));
    }

    return result;
  }

  public String getY2Data() {
    String result = "[]";

    if (null != y2Axis) {
      result = Y2_GSON.toJson(plotValues.stream()
        .filter(f -> !f.xNull() && !hideFlags ? true
          : (null != f && (null == f.getFlag2()
            || data.getFlagScheme().isGood(f.getFlag2(), true)
            || f.getFlag2().equals(FlagScheme.NEEDED_FLAG))))
        .collect(Collectors.toList()));
    }

    return result;
  }

  /**
   * Get the JSON data for the flags plot
   *
   * @return The flags data
   * @throws Exception
   */
  public String getFlagData() {

    String result = "[]";

    if (null != plotValues) {
      List<PlotValue> flagValues = plotValues.stream()
        .filter(x -> !hideFlags ? x.inFlagPlot()
          : null != x.getFlag() && x.getFlag().equals(FlagScheme.NEEDED_FLAG))
        .collect(Collectors.toList());

      Gson gson = new GsonBuilder().registerTypeAdapter(PlotValue.class,
        new FlagPlotValueSerializer(null != y2Axis)).create();

      result = gson.toJson(flagValues);
    }

    return result;
  }

  protected void makePlotValues() throws Exception {

    TreeMap<Coordinate, PlotPageTableValue> xValues = getXValues();
    TreeMap<Coordinate, PlotPageTableValue> yValues = getYValues();
    TreeMap<Coordinate, PlotPageTableValue> y2Values = getY2Values();

    plotValues = new LinkedHashSet<>();

    for (Coordinate coordinate : xValues.keySet()) {
      if (yValues.containsKey(coordinate) || y2Values.containsKey(coordinate)) {

        PlotPageTableValue x = xValues.get(coordinate);
        PlotPageTableValue y = yValues.get(coordinate);
        PlotPageTableValue y2 = y2Values.get(coordinate);

        boolean hasYValue = null != y && null != y.getValue();
        boolean hasY2Value = null != y2 && null != y2.getValue();

        PlotValue plotValue = null;

        if (hasYValue || hasY2Value) {

          Double yValue = null;
          boolean yGhost = false;
          Flag yFlag = null;
          if (null != y) {
            yValue = scaleYValue(MathUtils.nullableParseDouble(y.getValue()));
            yGhost = y.getQcFlag(data.getAllSensorValues())
              .equals(FlagScheme.FLUSHING_FLAG);
            yFlag = y.getQcFlag(data.getAllSensorValues());
            if (useNeededFlags && y.getFlagNeeded()) {
              yFlag = FlagScheme.NEEDED_FLAG;
            }
          }

          Double y2Value = null;
          boolean y2Ghost = false;
          Flag y2Flag = null;
          if (null != y2) {
            y2Value = scaleYValue(MathUtils.nullableParseDouble(y2.getValue()));
            y2Ghost = y2.getQcFlag(data.getAllSensorValues())
              .equals(FlagScheme.FLUSHING_FLAG);
            y2Flag = y2.getQcFlag(data.getAllSensorValues());
            // We never show NEEDED flags for Y2 axis
          }

          if (xAxis.getId() == FileDefinition.TIME_COLUMN_ID) {
            plotValue = new PlotValue(coordinate.getId(),
              (TimeCoordinate) coordinate, yValue, yGhost, yFlag, y2Value,
              y2Ghost, y2Flag, data.getFlagScheme());
          } else if (null != x && null != x.getValue() && null != y) {
            plotValue = new PlotValue(coordinate.getId(),
              MathUtils.nullableParseDouble(x.getValue()), yValue, yGhost,
              yFlag, y2Value, y2Ghost, y2Flag, data.getFlagScheme());
          }
        }

        if (null != plotValue) {
          plotValues.add(plotValue);
        }
      }
    }
  }

  protected TreeMap<Coordinate, PlotPageTableValue> getXValues()
    throws Exception {

    return data.getColumnValues(xAxis);
  }

  protected TreeMap<Coordinate, PlotPageTableValue> getYValues()
    throws Exception {

    return data.getColumnValues(yAxis);
  }

  protected TreeMap<Coordinate, PlotPageTableValue> getY2Values()
    throws Exception {

    return null == y2Axis ? new TreeMap<>() : data.getColumnValues(y2Axis);
  }

  /**
   * Get the multiplier for Y values.
   *
   * <p>
   * Sometimes we may want to scale the values on the Y axis, or make another
   * adjustment (e.g. make it negative for depths. All Y values are passed
   * through this method to have the scale applied.
   * </p>
   *
   * <p>
   * The default implementation simply returns the value unchanged. Override
   * this method if scaling is required.
   * </p>
   *
   * @param yValue
   *          The original Y value.
   * @return The scaled Y value.
   */
  protected Double scaleYValue(Double yValue) {
    return yValue;
  }

  /**
   * Initialise the plot and its data. Called from the front end when the QC
   * page is loaded.
   *
   * @throws Exception
   *           If any error occurs
   */
  public void init() {
    try {
      makePlotValues();
    } catch (Exception e) {
      data.error(e);
    }
  }

  public String getDataLabels() {
    List<String> labels = new ArrayList<>(4);
    labels.add(xAxis.getShortName());
    labels.add("ID");
    labels.add("GHOST");
    labels.add(yAxis.getShortName());

    if (null != y2Axis) {
      labels.add(""); // Y2 axis label
    }

    return new Gson().toJson(labels);
  }

  public String getFlagLabels() {
    List<String> labels = new ArrayList<>(4);
    labels.add(xAxis.getShortName());
    labels.add("BAD");
    labels.add("QUESTIONABLE");
    labels.add("NEEDED");
    labels.add("NOT_CALIBRATED");

    if (null != y2Axis) {
      labels.add(""); // Y2 value column
    }

    return new Gson().toJson(labels);
  }

  public String getY2Labels() {
    String result = null;

    if (null != y2Axis) {
      List<String> labels = new ArrayList<>(5);
      labels.add(xAxis.getShortName());
      labels.add("ID");
      labels.add(""); // Y1 axis
      labels.add("BAD");
      labels.add("QUESTIONABLE");
      labels.add("NOT_CALIBRATED");
      labels.add("GHOST");
      labels.add(y2Axis.getShortName());

      result = new Gson().toJson(labels);
    }

    return result;
  }

  protected void setHideFlags(boolean hideFlags) {
    this.hideFlags = hideFlags;
  }

  protected boolean getHideFlags() {
    return hideFlags;
  }
}
