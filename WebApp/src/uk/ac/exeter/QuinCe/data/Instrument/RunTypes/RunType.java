package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

/**
 * RunType class is used when handling missing run types in files. Run Type
 * categories can be fetched from the resource bundle. RunTypes overrides
 * equals(), hashCode() and toString(), and are considered equal if the
 * runTypeNames are equal.
 * @author Jonas F. Henriksen
 *
 */
public class RunType implements Comparable<RunType> {

  private String runTypeName;
  private String runTypeCategoryCode;

  /**
   * @param runTypeName found in the run type column in the data file
   */
  public RunType(String runTypeName) {
    this.runTypeName = runTypeName.toUpperCase();
  }
  /**
   * @param runTypeName found in the run type column in the data file
   * @param runTypeCategoryCode are given in the configuration
   */
  public RunType(String runTypeName, String runTypeCategoryCode) {
    this.runTypeName = runTypeName.toUpperCase();
    setRunTypeCategoryCode(runTypeCategoryCode);
  }
  /**
   * @return
   */
  public String getRunTypeName() {
    return runTypeName;
  }

  /**
   * @return
   */
  public String getRunTypeCategoryCode() {
    return runTypeCategoryCode;
  }

  /**
   * @param runTypeCategory
   */
  public void setRunTypeCategoryCode(String runTypeCategoryCode) {
    this.runTypeCategoryCode = runTypeCategoryCode;
  }

  @Override
  public int compareTo(RunType o) {
    return getRunTypeName().compareTo(o.getRunTypeName());
  }

  @Override
  public String toString() {
    return getRunTypeName();
  }

  @Override
  public boolean equals(Object obj) {
    return toString().equals(obj.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
