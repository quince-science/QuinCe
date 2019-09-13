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
      header.append(exportOption.getReplacementHeader(code));
    } else {
      header.append(heading);
    }

    if (exportOption.includeUnits() && null != units) {
      header.append(" [");
      header.append(units);
      header.append("]");
    }

    return header.toString();
  }

  public String getHeading() {
    return heading;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((heading == null) ? 0 : heading.hashCode());
    result = prime * result + ((units == null) ? 0 : units.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof ColumnHeader))
      return false;
    ColumnHeader other = (ColumnHeader) obj;
    if (code == null) {
      if (other.code != null)
        return false;
    } else if (!code.equals(other.code))
      return false;
    if (heading == null) {
      if (other.heading != null)
        return false;
    } else if (!heading.equals(other.heading))
      return false;
    if (units == null) {
      if (other.units != null)
        return false;
    } else if (!units.equals(other.units))
      return false;
    return true;
  }
}
