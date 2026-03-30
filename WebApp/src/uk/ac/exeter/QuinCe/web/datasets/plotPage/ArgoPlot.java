package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.ArgoCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.ArgoProfile;
import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.MathUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ArgoManualQCData;

public class ArgoPlot extends Plot {

  /**
   * Stores the data object locally as the correct class.
   */
  ArgoManualQCData data;

  public ArgoPlot(ArgoManualQCData data, PlotPageColumnHeading xAxis,
    PlotPageColumnHeading yAxis, boolean useNeededFlags) throws Exception {
    super(data, xAxis, yAxis, useNeededFlags);

    this.data = data;
  }

  @Override
  public void setYaxis(long yAxis) throws Exception {
    // We ignore any requests to change Y axis
  }

  @Override
  public void setY2axis(long y2Axis) throws Exception {
    // We ignore any requests to change Y2 axis
  }

  @Override
  protected Double scaleYValue(Double yValue) {
    // Depths are displayed as negative in plots.
    return yValue * -1D;
  }

  /**
   * Get the values for a given column in the specified profile.
   *
   * <p>
   * This calls {@link PlotPageData#getColumnValues(PlotPageColumnHeading)} to
   * retrieve the data.
   * </p>
   *
   * @param column
   *          The column.
   * @param profile
   *          The profile.
   * @return The profile's column values.
   * @throws Exception
   *           If the values cannot be retreived.
   */
  private TreeMap<Coordinate, PlotPageTableValue> getColumnValues(
    PlotPageColumnHeading column, ArgoProfile profile) throws Exception {

    TreeMap<Coordinate, PlotPageTableValue> allValues = data
      .getColumnValues(column);

    return allValues.entrySet().stream()
      .filter(e -> profile.matches((ArgoCoordinate) e.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
        (e1, e2) -> e1, TreeMap::new));
  }

  @Override
  protected TreeMap<Coordinate, PlotPageTableValue> getXValues()
    throws Exception {

    ArgoProfile profile = data.getProfiles().get(data.getSelectedProfile());
    return getColumnValues(xAxis, profile);
  }

  @Override
  protected TreeMap<Coordinate, PlotPageTableValue> getYValues()
    throws Exception {

    ArgoProfile profile = data.getProfiles().get(data.getSelectedProfile());
    return getColumnValues(yAxis, profile);
  }

  @Override
  protected TreeMap<Coordinate, PlotPageTableValue> getY2Values()
    throws Exception {

    ArgoProfile profile = data.getProfiles().get(data.getSelectedProfile());
    return null == y2Axis ? new TreeMap<>() : getColumnValues(y2Axis, profile);
  }

  @Override
  public long getDisplayVariable() {
    return getXaxis();
  }

  @Override
  protected void makePlotValues() throws Exception {

    TreeMap<Coordinate, PlotPageTableValue> xValues = getXValues();
    TreeMap<Coordinate, PlotPageTableValue> yValues = getYValues();
    TreeMap<Coordinate, PlotPageTableValue> y2Values = getY2Values();

    // Re-sort the y values by their ascending entry value.
    LinkedHashMap<Coordinate, PlotPageTableValue> sortedYValues = sortValues(
      yValues);

    plotValues = new LinkedHashSet<>();

    for (Coordinate coordinate : sortedYValues.keySet()) {
      PlotPageTableValue x = xValues.get(coordinate);
      PlotPageTableValue y = yValues.get(coordinate);
      PlotPageTableValue y2 = y2Values.get(coordinate);

      PlotValue plotValue = null;

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
          (TimeCoordinate) coordinate, yValue, yGhost, yFlag, y2Value, y2Ghost,
          y2Flag, data.getFlagScheme());
      } else if (null != x && null != x.getValue() && null != y) {
        plotValue = new PlotValue(coordinate.getId(),
          MathUtils.nullableParseDouble(x.getValue()), yValue, yGhost, yFlag,
          y2Value, y2Ghost, y2Flag, data.getFlagScheme());

      }

      if (null != plotValue) {
        plotValues.add(plotValue);
      }
    }
  }

  private LinkedHashMap<Coordinate, PlotPageTableValue> sortValues(
    Map<Coordinate, PlotPageTableValue> source) {

    return source.entrySet().stream()
      .sorted(
        Map.Entry.comparingByValue(new PlotPageTableValueNumericComparator()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
        (e1, e2) -> e1, LinkedHashMap::new));
  }
}
