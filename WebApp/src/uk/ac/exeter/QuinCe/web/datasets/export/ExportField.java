package uk.ac.exeter.QuinCe.web.datasets.export;

import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;

public class ExportField extends Field {

  private ColumnHeader columnHeader;

  private ExportOption exportOption;

  public ExportField(ColumnHeader header, ExportOption exportOption) {
    super(header.hashCode(), header.getHeading());
    this.columnHeader = header;
    this.exportOption = exportOption;
  }

  @Override
  public String getName() {
    return columnHeader.makeColumnHeading(exportOption);
  }

}
