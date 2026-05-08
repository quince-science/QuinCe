package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Holds the properties of a column heading on the plot page.
 */
public class PlotPageColumnHeading extends ColumnHeading {

  /**
   * Indicates whether or not the column will hold numeric values.
   */
  private final boolean numeric;

  /**
   * The reference value for this column - used primarily in gas standards page
   * to show reference gas concentration.
   */
  private final TreeMap<LocalDateTime, Double> referenceValues;

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
   * Indicates whether only <i>Bad</i> QC values can be set on values for this
   * column.
   *
   * @see SensorType#badFlagOnly()
   * @see FlagScheme#getBadFlag()
   */
  private final boolean badFlagOnly;

  /**
   * Simple constructor.
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public PlotPageColumnHeading(long id, String shortName, String longName,
    String codeName, String units, boolean includeType, boolean numeric,
    boolean editable, boolean badFlagOnly) {

    super(id, shortName, longName, codeName, units, true, includeType);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = id;
    this.referenceValues = null;
    this.badFlagOnly = badFlagOnly;
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
    String codeName, String units, boolean includeType, boolean numeric,
    boolean editable, TreeMap<LocalDateTime, Double> referenceValue,
    boolean badFlagOnly) {

    super(id, shortName, longName, codeName, units, true, includeType);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = id;
    this.referenceValues = referenceValue;
    this.badFlagOnly = badFlagOnly;
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
    this.referenceValues = null;
    this.badFlagOnly = true;
  }

  public PlotPageColumnHeading(SensorType sensorType) {
    super(sensorType);

    this.numeric = true;
    this.editable = false;
    this.selectionColumn = sensorType.getId();
    this.referenceValues = null;
    this.badFlagOnly = sensorType.badFlagOnly();
  }

  /**
   * Construct a plot page heading from a generic column heading
   *
   * @param calculationParameter
   *          The calculation parameter
   */
  public PlotPageColumnHeading(ColumnHeading heading, boolean numeric,
    boolean editable, boolean badFlagOnly) {

    super(heading);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = heading.getId();
    this.referenceValues = null;
    this.badFlagOnly = badFlagOnly;
  }

  /**
   * Construct a plot page heading from a generic column heading
   *
   * @param calculationParameter
   *          The calculation parameter
   */
  public PlotPageColumnHeading(ColumnHeading heading, boolean numeric,
    boolean editable, boolean badFlagOnly, long selectionColumn) {

    super(heading);
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = selectionColumn;
    this.referenceValues = null;
    this.badFlagOnly = badFlagOnly;
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

  public TreeMap<LocalDateTime, Double> getReferenceValues() {
    return referenceValues;
  }

  public boolean badFlagOnly() {
    return badFlagOnly;
  }

  /**
   * See if a {@link Collection} of {@link PlotPageColumnHeading}s contains the
   * specified {@link ColumnHeading}.
   *
   * <p>
   * The entries in the {@link Collection} are cast to {@link ColumnHeading} for
   * comparison.
   * </p>
   *
   * @param headings
   *          The collection of headings.
   * @param heading
   *          The heading to be located.
   * @return {@code true} if the collection contains the heading; {@code false}
   *         if not.
   */
  public static boolean contains(Collection<PlotPageColumnHeading> headings,
    ColumnHeading heading) {

    boolean result = false;

    for (PlotPageColumnHeading test : headings) {
      if (((ColumnHeading) test).equals(heading)) {
        result = true;
        break;
      }
    }

    return result;
  }
}
