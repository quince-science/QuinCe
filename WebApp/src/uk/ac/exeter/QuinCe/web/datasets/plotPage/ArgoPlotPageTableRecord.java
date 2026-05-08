package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.data.Dataset.ArgoCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;

public class ArgoPlotPageTableRecord extends PlotPageTableRecord {

  public ArgoPlotPageTableRecord(long id, FlagScheme flagScheme) {
    super(id, flagScheme);
  }

  public ArgoPlotPageTableRecord(ArgoCoordinate id, FlagScheme flagScheme) {
    super(id, flagScheme);
  }

  @Override
  public void addCoordinate(Coordinate coordinate) {
    ArgoCoordinate castCoordinate = (ArgoCoordinate) coordinate;
    addColumn(new SimplePlotPageTableValue(
      String.valueOf(castCoordinate.getNLevel()), flagScheme.getGoodFlag(), "",
      false, PlotPageTableValue.MEASURED_TYPE, -1L));
    addColumn(new SimplePlotPageTableValue(
      String.valueOf(castCoordinate.getPres()), flagScheme.getGoodFlag(), "",
      false, PlotPageTableValue.MEASURED_TYPE, -1L));
  }
}
