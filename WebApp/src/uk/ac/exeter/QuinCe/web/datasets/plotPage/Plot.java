package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MathUtils;

public class Plot {

  /**
   * Gson generator for the main plot data
   */
  private static Gson MAIN_DATA_GSON;

  /**
   * Gson generator for the flag data
   */
  private static Gson FLAGS_GSON;

  /**
   * The source data for the plot
   */
  private final PlotPageData data;

  /**
   * Indicates whether or not NEEDED flags should be displayed.
   *
   * If {@code false}, the automatic QC flag is used.
   */
  private final boolean useNeededFlags;

  /**
   * The column ID of the X axis
   */
  private PlotPageColumnHeading xAxis = null;

  /**
   * The column ID of the Y axis
   */
  private PlotPageColumnHeading yAxis = null;

  /**
   * The column ID of the second Y axis
   */
  private PlotPageColumnHeading y2Axis = null;

  /**
   * The plot values.
   */
  private TreeSet<PlotValue> plotValues = null;

  private boolean hideFlags = false;

  static {
    MAIN_DATA_GSON = new GsonBuilder()
      .registerTypeAdapter(PlotValue.class, new MainPlotValueSerializer())
      .create();

    FLAGS_GSON = new GsonBuilder()
      .registerTypeAdapter(PlotValue.class, new FlagPlotValueSerializer())
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
    return null == yAxis ? Long.MIN_VALUE : yAxis.getId();
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
    return null == y2Axis ? Long.MIN_VALUE : y2Axis.getId();
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
      result = MAIN_DATA_GSON.toJson(plotValues.stream()
        .filter(f -> !f.xNull() && !hideFlags ? true
          : (f.getFlag().isGood() || f.getFlag().equals(Flag.NEEDED)))
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
        .filter(
          x -> !hideFlags ? x.inFlagPlot() : x.getFlag().equals(Flag.NEEDED))
        .collect(Collectors.toList());

      result = FLAGS_GSON.toJson(flagValues);
    }

    return result;
  }

  protected void makePlotValues() throws Exception {

    TreeMap<LocalDateTime, PlotPageTableValue> xValues = data
      .getColumnValues(xAxis);

    TreeMap<LocalDateTime, PlotPageTableValue> yValues = data
      .getColumnValues(yAxis);

    TreeMap<LocalDateTime, PlotPageTableValue> y2Values = null == y2Axis
      ? new TreeMap<LocalDateTime, PlotPageTableValue>()
      : data.getColumnValues(y2Axis);

    plotValues = new TreeSet<PlotValue>();

    for (LocalDateTime time : xValues.keySet()) {
      if (yValues.containsKey(time)) {

        PlotPageTableValue x = xValues.get(time);
        PlotPageTableValue y = yValues.get(time);
        PlotPageTableValue y2 = y2Values.get(time);

        PlotValue plotValue = null;

        if (null != y && null != y.getValue()) {

          Double yValue = null;
          boolean yGhost = false;
          Flag yFlag = null;
          if (null != y) {
            yValue = MathUtils.nullableParseDouble(y.getValue());
            yGhost = y.getQcFlag().equals(Flag.FLUSHING);
            yFlag = y.getQcFlag();
            if (useNeededFlags && y.getFlagNeeded()) {
              yFlag = Flag.NEEDED;
            }
          }

          Double y2Value = null;
          boolean y2Ghost = false;
          Flag y2Flag = null;
          if (null != y2) {
            y2Value = MathUtils.nullableParseDouble(y2.getValue());
            y2Ghost = y2.getQcFlag().equals(Flag.FLUSHING);
            y2Flag = y2.getQcFlag();
            if (useNeededFlags && y2.getFlagNeeded()) {
              y2Flag = Flag.NEEDED;
            }
          }

          if (xAxis.getId() == FileDefinition.TIME_COLUMN_ID) {
            plotValue = new PlotValue(DateTimeUtils.dateToLong(time), time,
              yValue, yGhost, yFlag, y2Value, y2Ghost, y2Flag);
          } else if (null != x && null != x.getValue() && null != y) {
            plotValue = new PlotValue(DateTimeUtils.dateToLong(time),
              MathUtils.nullableParseDouble(x.getValue()), yValue, yGhost,
              yFlag, y2Value, y2Ghost, y2Flag);
          }
        }

        if (null != plotValue) {
          plotValues.add(plotValue);
        }
      }
    }
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
    List<String> labels = new ArrayList<String>(4);
    labels.add(xAxis.getShortName());
    labels.add("ID");
    labels.add("GHOST");
    labels.add(yAxis.getShortName());

    if (null != y2Axis) {
      labels.add("GHOST");
      labels.add(y2Axis.getShortName());
    }

    return new Gson().toJson(labels);

  }

  public String getFlagLabels() {
    List<String> labels = new ArrayList<String>(4);
    labels.add(xAxis.getShortName());
    labels.add("BAD");
    labels.add("QUESTIONABLE");
    labels.add("NEEDED");

    if (null != y2Axis) {
      labels.add("BAD2");
      labels.add("QUESTIONABLE2");
      labels.add("NEEDED2");
    }

    return new Gson().toJson(labels);
  }

  protected void setHideFlags(boolean hideFlags) {
    this.hideFlags = hideFlags;
  }

  protected boolean getHideFlags() {
    return hideFlags;
  }
}
