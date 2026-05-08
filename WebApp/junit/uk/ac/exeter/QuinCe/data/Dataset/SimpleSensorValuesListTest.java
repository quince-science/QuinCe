package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;

public class SimpleSensorValuesListTest extends SensorValuesListTest {

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void maintainsOrderTest() throws Exception {
    SensorValuesList list = new SimpleSensorValuesList(1L,
      getDatasetSensorValues(), false);

    // First value
    list.add(makeSensorValue(1L, 1, 5));

    // End
    list.add(makeSensorValue(1L, 1, 10));

    // Start
    list.add(makeSensorValue(1L, 1, 1));

    // Middle
    list.add(makeSensorValue(1L, 1, 3));

    assertTrue(timeCoordinatesOrdered(list.getRawCoordinates()));
  }

}
