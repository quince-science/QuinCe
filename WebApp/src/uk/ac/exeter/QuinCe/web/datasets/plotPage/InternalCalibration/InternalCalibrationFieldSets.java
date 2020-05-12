package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import uk.ac.exeter.QuinCe.web.datasets.data.DatasetMeasurementData;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSet;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSets;

/**
 * This is a special case of the {@link FieldSets} object used in the
 * {@link DatasetMeasurementData} structures.
 *
 * <p>
 * For most instances, the fields are grouped into sets of {@link Field}s. Here,
 * however, each {@link Field} is used as the name of a {@link FieldSet}, which
 * is then divided into the different Run Types (matching the different internal
 * calibration modes).
 * </p>
 *
 * <p>
 * This special version of the {@link FieldSets} adds the FieldSets to the cache
 * of known fields, so that lookups will work as for normal datasets.
 * </p>
 *
 * @author Steve Jones
 *
 */
@Deprecated
@SuppressWarnings("serial")
public class InternalCalibrationFieldSets extends FieldSets {

  /**
   * Base constructor
   *
   * @param rowIdName
   *          The name of the field that acts as the row ID
   */
  public InternalCalibrationFieldSets(String rowIdName) {
    super(rowIdName);
  }

  @Override
  public FieldSet addFieldSet(long id, String name, boolean defaultFieldSet) {

    FieldSet result = super.addFieldSet(id, name, defaultFieldSet);

    // The FieldSet is actually a proper field, so add it to the fieldsByx
    // properties
    Field fieldSetField = new Field(null, id, name);
    fieldsById.put(id, fieldSetField);
    fieldsByName.put(name, fieldSetField);

    return result;
  }
}
