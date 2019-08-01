package uk.ac.exeter.QuinCe.web.datasets.export;

import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;

public class ExportField extends Field {

  private ColumnHeader columnHeader;

  private ExportOption exportOption;

  private boolean hasQC;

  public ExportField(SensorType sensorType, boolean hasQC, ExportOption exportOption) {
    super(sensorType.getId(), sensorType.getName());
    this.columnHeader = sensorType.getColumnHeader();
    this.exportOption = exportOption;
    this.hasQC = hasQC;
  }

  public ExportField(long id, ColumnHeader header, boolean hasQC, ExportOption exportOption) {
    super(id, header.getHeading());
    this.columnHeader = header;
    this.hasQC = hasQC;
    this.exportOption = exportOption;
  }

  @Override
  public String getName() {
    return columnHeader.makeColumnHeading(exportOption);
  }

  public boolean hasQC() {
    return hasQC;
  }
}
