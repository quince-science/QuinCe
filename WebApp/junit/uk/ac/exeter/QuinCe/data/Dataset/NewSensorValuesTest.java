package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class NewSensorValuesTest extends BaseTest {

  /**
   * Ensure that when two values are added with the same {@link Coordinate},
   * they end up using the same {@link Coordinate} object.
   *
   * @throws CoordinateException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   */
  @Test
  public void repeatedCoordinatesTest() throws Exception {

    DataSet dataset = Mockito.mock(DataSet.class);
    Mockito.when(dataset.getId()).thenReturn(1L);
    Mockito.when(dataset.getFlagScheme())
      .thenReturn(IcosFlagScheme.getInstance());

    NewSensorValues values = new NewSensorValues(dataset);

    TimeCoordinate coord1 = new TimeCoordinate(
      LocalDateTime.of(2026, 5, 15, 16, 39, 00));
    SensorValue value1 = values.create(1L, coord1, "Value 1");

    TimeCoordinate coord2 = new TimeCoordinate(
      LocalDateTime.of(2026, 5, 15, 16, 39, 00));
    SensorValue value2 = values.create(2L, coord2, "Value 2");

    /*
     * The two values should have different Coordinate objects.
     */
    assertFalse(value1.getCoordinate() == value2.getCoordinate());

    /*
     * Call getSensorValues which triggers rationalisation of Coordinate
     * objects.
     */
    values.getSensorValues();

    /*
     * Now the two values should have the same Coordinate object.
     */
    assertTrue(value1.getCoordinate() == value2.getCoordinate());

  }
}
