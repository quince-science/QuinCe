package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;

public class QCMap {

  private PlotPageData data;

  /**
   * The column ID of the Y axis
   */
  private PlotPageColumnHeading dataColumn = null;

  /**
   * Indicates whether or not NEEDED flags should be displayed.
   *
   * If {@code false}, the automatic QC flag is used.
   */
  private final boolean useNeededFlags;

  /**
   * The maximum bounds of the dataset (including all data points).
   */
  private GeoBounds viewBounds;

  /**
   * The bounds of the data currently being viewed.
   *
   * <p>
   * This may not include all data points depending on the configuration of the
   * map view.
   * </p>
   */
  private GeoBounds dataBounds = null;

  private boolean updateScale;

  private Double[] mapScaleLimits = null;

  private String mapData;

  private static Gson gson;

  private boolean hideFlags = false;

  static {
    gson = new Gson();
  }

  protected QCMap(PlotPageData data, PlotPageColumnHeading dataColumn,
    boolean useNeededFlags) throws Exception {

    this.data = data;
    this.dataColumn = dataColumn;
    this.useNeededFlags = useNeededFlags;
    this.viewBounds = data.getDataset().getBounds();
  }

  public long getColumn() {
    return null == dataColumn ? Long.MIN_VALUE : dataColumn.getId();
  }

  public void setColumn(long column) throws Exception {
    this.dataColumn = data.getColumnHeading(column);
  }

  public String getData() {
    return mapData;
  }

  public String getScaleLimits() {

    String result;

    if (null == mapScaleLimits || mapScaleLimits[0].isNaN()
      || mapScaleLimits[1].isNaN()) {
      result = "[0, 0]";
    } else {
      result = gson.toJson(mapScaleLimits);
    }

    return result;
  }

  /**
   * @return the mapBounds
   */
  public String getViewBounds() {
    return viewBounds.toJson();
  }

  /**
   * @param mapBounds
   *          the mapBounds to set
   */
  public void setViewBounds(String bounds) {
    if (!StringUtils.isEmpty(bounds)) {
      this.viewBounds = new GeoBounds(bounds);
    }
  }

  public String getDataBounds() {
    if (null == dataBounds) {
      try {
        dataBounds = data.getMapBounds(dataColumn, hideFlags);
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
        dataBounds = data.getDataset().getBounds();
      }
    }

    return dataBounds.toJson();
  }

  /**
   * @return the updateScale
   */
  public boolean getUpdateScale() {
    return updateScale;
  }

  /**
   * @param mapUpdateScale
   *          the updateScale to set
   */
  public void setUpdateScale(boolean updateScale) {
    this.updateScale = updateScale;
  }

  public void generateMapData() {
    try {
      mapScaleLimits = data.getValueRange(dataColumn, hideFlags);
      mapData = data.getMapData(dataColumn, viewBounds, useNeededFlags,
        hideFlags, includePath(), data.getAllSensorValues());
      dataBounds = null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setHideFlags(boolean hideFlags) {
    this.hideFlags = hideFlags;
  }

  public boolean getHideFlags() {
    return hideFlags;
  }

  protected boolean includePath() {
    return false;
  }
}
