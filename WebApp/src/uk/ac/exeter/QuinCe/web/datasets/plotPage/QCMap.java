package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;

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

  private GeoBounds bounds;

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
    this.bounds = data.getDataset().getBounds();
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
    return null == mapScaleLimits ? "[0, 1]" : gson.toJson(mapScaleLimits);
  }

  /**
   * @return the mapBounds
   */
  public String getViewBounds() {
    return bounds.toJson();
  }

  /**
   * @param mapBounds
   *          the mapBounds to set
   */
  public void setViewBounds(String bounds) {
    if (!StringUtils.isEmpty(bounds)) {
      this.bounds = new GeoBounds(bounds);
    }
  }

  public String getDataBounds() {
    return data.getDataset().getBounds().toJson();
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
      mapScaleLimits = data.getValueRange(dataColumn);
      mapData = data.getMapData(dataColumn, bounds, useNeededFlags, hideFlags);
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
}
