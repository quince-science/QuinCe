package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ArgoManualQCData;

public class ArgoQCMap extends QCMap {

  public ArgoQCMap(ArgoManualQCData data, PlotPageColumnHeading dataColumn,
    boolean useNeededFlags) throws Exception {

    super(data, dataColumn, useNeededFlags);
  }

  @Override
  protected boolean includePath() {
    return true;
  }
}
