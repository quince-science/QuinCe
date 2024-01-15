package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

/**
 * An {@code AttributeCondition} is an attribute from a {@link Variable} whose
 * value impacts the required {@link SensorType}s for that {@link Variable}.
 *
 * <p>
 * Each {@link Variable} has a list of {@link SensorType}s that are required for
 * the data reduction to be performed. These are set up by the user when they
 * create an {@link uk.ac.exeter.QuinCe.data.Instrument}; they choose which
 * {@link Variable}(s) the {@link uk.ac.exeter.QuinCe.data.Instrument} measures,
 * and are then asked to locate the corresponding columns in their data files.
 * </p>
 *
 * <p>
 * Some {@link Variable}s have additional attributes that the user must provide,
 * and in some cases the provided attribute will have an impact on which
 * {@link SensorType}s are required for the data reduction to be performed in
 * accordance with those attributes. This class represents a {@link Variable}
 * attribute and a specific value for that attribute. The user-specified
 * {@link VariableAttributes} can then be compared to this to determine whether
 * or not certain {@link SensorType}(s) will be required.
 * </p>
 */
public class AttributeCondition {
  /**
   * The name of the attribute to be examined.
   */
  private final String attributeName;

  /**
   * The value of the named attribute that will trigger a particular variation
   * in the required {@link SensorType}s for the {@link Variable} being set up.
   */
  private final String attributeValue;

  /**
   * Builds the attribute condition using the specified attribute name and
   * value.
   *
   * @param attributeName
   *          The attribute name.
   * @param attributeValue
   *          The attribute value.
   */
  protected AttributeCondition(String attributeName, String attributeValue) {
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
  }

  /**
   * Determine whether or not the supplied set of variable attributes (as
   * entered by the user) contains this condition's attribute name with this
   * condition's value.
   *
   * @param attributes
   *          The variable attributes.
   * @return {@code true} if the attributes contain the named attribute, and
   *         that attribute has the named value.
   * @see #attributeName
   * @see #attributeValue
   */
  protected boolean matches(VariableAttributes attributes) {
    return null != attributes
      && attributes.get(attributeName).equals(attributeValue);
  }
}
