package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Holds the properties of a column heading on the plot page.
 *
 * @author Steve Jones
 *
 */
public class PlotPageColumnHeading extends ColumnHeading {

  /**
   * Indicates whether or not the column will hold numeric values.
   */
  private final boolean numeric;

  /**
   * The reference value for this column
   */
  private final Double referenceValue;

  /**
   * Indicates whether or not values in this column can be edited.
   */
  private final boolean editable;

  /**
   * Indicates the true selection column for this column.
   *
   * <p>
   * Some columns cannot be selected, because they are combined in the data
   * table. Most commonly, latitude is combined with longitude. Therefore
   * selections on the latitude column should instead become selections on the
   * longitude column.
   * </p>
   *
   * <p>
   * For normal columns, this field will be the same as the {@link #id} field.
   * </p>
   */
  private final long selectionColumn;

  /**
   * Simple constructor.
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public PlotPageColumnHeading(long id, String shortName, String longName,
    String codeName, String units, boolean numeric, boolean editable) {

    super(id, shortName, longName, codeName, units, true);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = id;
    this.referenceValue = null;
  }

  /**
   * Simple constructor.
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public PlotPageColumnHeading(long id, String shortName, String longName,
    String codeName, String units, boolean numeric, boolean editable,
    boolean hasQC) {

    super(id, shortName, longName, codeName, units, hasQC);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = id;
    this.referenceValue = null;
  }

  /**
   * Simple constructor with reference value
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public PlotPageColumnHeading(long id, String shortName, String longName,
    String codeName, String units, boolean numeric, boolean editable,
    Double referenceValue) {

    super(id, shortName, longName, codeName, units, true);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = id;
    this.referenceValue = referenceValue;
  }

  /**
   * Constructor with a different selection column.
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public PlotPageColumnHeading(long id, String shortName, String longName,
    String codeName, String units, boolean numeric, boolean editable,
    long selectionColumn) {

    super(id, shortName, longName, codeName, units, true);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = selectionColumn;
    this.referenceValue = null;
  }

  /**
   * Construct a plot page heading from a calculation parameter
   *
   * @param calculationParameter
   *          The calculation parameter
   */
  public PlotPageColumnHeading(CalculationParameter calculationParameter) {
    super(calculationParameter);

    this.numeric = true;
    this.editable = false;
    this.selectionColumn = calculationParameter.getId();
    this.referenceValue = null;
  }

  public PlotPageColumnHeading(SensorType sensorType) {
    super(sensorType);

    this.numeric = true;
    this.editable = false;
    this.selectionColumn = sensorType.getId();
    this.referenceValue = null;
  }

  /**
   * Construct a plot page heading from a generic column heading
   *
   * @param calculationParameter
   *          The calculation parameter
   */
  public PlotPageColumnHeading(ColumnHeading heading, boolean numeric,
    boolean editable) {

    super(heading);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = heading.getId();
    this.referenceValue = null;
  }

  /**
   * Construct a plot page heading from a generic column heading
   *
   * @param calculationParameter
   *          The calculation parameter
   */
  public PlotPageColumnHeading(ColumnHeading heading, boolean numeric,
    boolean editable, long selectionColumn) {

    super(heading);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = selectionColumn;
    this.referenceValue = null;
  }

  /**
   * Determine whether or not the column will hold numeric values.
   *
   * @return {@code true} if the column will hold numeric values; {@code false}
   *         otherwise.
   */
  public boolean isNumeric() {
    return numeric;
  }

  public boolean canEdit() {
    return editable;
  }

  public long getSelectionColumn() {
    return selectionColumn;
  }

  public Double getReferenceValue() {
    return referenceValue;
  }
}
