package uk.ac.exeter.QuinCe.web.datasets.export;

import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;

public class ExportField extends Field {

  private ColumnHeader columnHeader;

  private ExportOption exportOption;

  public ExportField(SensorType sensorType, ExportOption exportOption) {
    super(sensorType.getId(), sensorType.getName());
    this.columnHeader = sensorType.getColumnHeader();
    this.exportOption = exportOption;
  }

  public ExportField(long id, ColumnHeader header, ExportOption exportOption) {
    super(id, header.getHeading());
    this.columnHeader = header;
    this.exportOption = exportOption;
  }

  @Override
  public String getName() {
    return columnHeader.makeColumnHeading(exportOption);
  }

}
