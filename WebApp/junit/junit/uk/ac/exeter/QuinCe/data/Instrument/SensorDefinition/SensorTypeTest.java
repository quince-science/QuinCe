package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Tests for the {@link SensorType} class.
 *
 * @see SensorType
 *
 * @author Steve Jones
 *
 */
public class SensorTypeTest extends BaseTest {

  /**
   * Create a simple, valid {@link SensorType}. It has an ID (fixed to
   * {@code 1}), a name, and a group; all other information is {@code null}.
   *
   * @return The {@link SensorType}
   * @throws Exception
   *           If the object cannot be constructed
   */
  private SensorType getBasicSensorType() throws Exception {
    SensorType result = Mockito.mock(SensorType.class);
    Mockito.when(result.getId()).thenReturn(1L);
    Mockito.when(result.getShortName()).thenReturn("Name");
    Mockito.when(result.getGroup()).thenReturn("Group");
    Mockito.when(result.getUnits()).thenReturn("ewe knit");
    Mockito.when(result.getLongName()).thenReturn("Test Sensor (long name)");
    Mockito.when(result.getCodeName()).thenReturn("Code");

    return result;
  }

  /**
   * Tests that the {@link SensorType#getId()} method works.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void idTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals(1L, type.getId());
  }

  /**
   * Tests that the {@link SensorType#getName()} method works.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void nameTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals("Name", type.getShortName());
  }

  /**
   * Tests that the {@link SensorType#getGroup()} method works.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void groupTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals("Group", type.getGroup());
  }

  /**
   * Tests that the {@link SensorType#equals(Object)} method returns
   * {@code false} when invoked with a non-{@link SensorType} object.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsRandomObjectTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals(ZoneOffset.UTC));
  }

  /**
   * Tests that the {@link SensorType#equals(Object)} method returns
   * {@code false} when invoked with a the name of a different
   * {@link SensorType} object.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsWrongNameStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals("Flurble"));
  }

  /**
   * Tests that the {@link SensorType#equals(Object)} method returns
   * {@code false} when invoked with a the name of that object (the name is not
   * sufficient for {@code equals(Object) == true}).
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsActualNameStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals("Name"));
  }
}
