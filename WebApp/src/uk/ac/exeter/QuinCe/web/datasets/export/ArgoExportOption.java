package uk.ac.exeter.QuinCe.web.datasets.export;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Export.ExportConfigurationException;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;

public class ArgoExportOption extends ExportOption {

  protected ArgoExportOption(DataSet dataset)
    throws ExportConfigurationException {
    super(0, "Argo", ",", dataset.getInstrument().getVariables());

    setDataClass(ArgoExportData.class);
    setHeaderMode(HEADER_MODE_CODE);
    setIncludeCalculationColumns(false);
    setIncludeQCComments(false);
    setIncludeRawSensors(true);
    setIncludeUnits(false);
    setMeasurementsOnly(false);
    setMissingQCFlag("");
    setMissingValue("NaN");
    setQcFlagSuffix("_qc");
    setQcCommentSuffix("_comment");
    setSkipBad(false);
  }
}
