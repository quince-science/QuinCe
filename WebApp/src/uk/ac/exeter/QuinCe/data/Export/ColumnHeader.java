package uk.ac.exeter.QuinCe.data.Export;

public class ColumnHeader {

  private final String heading;

  private final String code;

  private final String units;

  public ColumnHeader(String header, String code, String units) {
    this.heading = header;
    this.code = code;
    this.units = units;
  }

  public String makeColumnHeading(ExportOption exportOption) {

    StringBuilder header = new StringBuilder();

    if (exportOption.useColumnCodes()) {
      header.append(code);
    } else {
      header.append(heading);
    }

    if (exportOption.includeUnits()) {
      header.append(" [");
      header.append(units);
      header.append("]");
    }

    return header.toString();
  }
}
