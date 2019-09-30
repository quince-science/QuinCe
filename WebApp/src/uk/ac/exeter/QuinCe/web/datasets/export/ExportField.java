package uk.ac.exeter.QuinCe.web.datasets.export;

import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSet;

public class ExportField extends Field {

  private ColumnHeader columnHeader;

  private ExportOption exportOption;

  private boolean diagnostic;

  private boolean hasQC;

  public ExportField(FieldSet fieldSet, SensorType sensorType,
    boolean diagnostic, boolean hasQC, ExportOption exportOption) {
    super(fieldSet, sensorType.getId(), sensorType.getName());
    this.columnHeader = sensorType.getColumnHeader();
    this.diagnostic = diagnostic;
    this.hasQC = hasQC;
    this.exportOption = exportOption;
  }

  public ExportField(FieldSet fieldSet, long id, ColumnHeader header,
    boolean diagnostic, boolean hasQC, ExportOption exportOption) {
    super(fieldSet, id, header.getHeading());
    this.columnHeader = header;
    this.diagnostic = diagnostic;
    this.hasQC = hasQC;
    this.exportOption = exportOption;
  }

  @Override
  public String getFullName() {
    return columnHeader.makeColumnHeading(exportOption);
  }

  public boolean hasQC() {
    return hasQC;
  }

  public boolean isDiagnostic() {
    return diagnostic;
  }
}
