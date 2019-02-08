package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.DBTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;

/**
 * Tests in this file assume that there are some valid sensor types already
 * configured in the database, but create their own types for specific testing
 *
 * @author Steve Jones
 *
 */
public class SensorsConfigurationTest extends DBTest {

  private SensorsConfiguration getConfig() throws SensorConfigurationException {
    return new SensorsConfiguration(getDataSource());
  }

  @FlywayTest
  @Test
  public void testLoadConfiguration() throws SensorConfigurationException  {
    SensorsConfiguration config = getConfig();
    assertNotNull(config);

    boolean runTypeFound = false;
    for (SensorType type : config.getSensorTypes()) {
      if (type.getId() == SensorType.RUN_TYPE_ID) {
        if (type.getName().equals("Run Type")) {
          runTypeFound = true;
          break;
        }
      }
    }

    assertTrue(runTypeFound, "Run Type not added to sensors configuration");
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/nonExistentParent"
  })
  @Test
  public void nonExistentParentTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/nonExistentDependsOn"
  })
  @Test
  public void nonExistentDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      getConfig();
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/sensorTypesList"
  })
  @Test
  public void sensorTypesListTest() throws SensorConfigurationException {
    List<SensorType> types = getConfig().getSensorTypes();

    // NOTE that the special "Run Type" type will be at the end of the list

    boolean listOrdered = true;
    if (!types.get(0).getName().equals("AAA")) {
      listOrdered = false;
    }
    if (!types.get(1).getName().equals("BBB")) {
      listOrdered = false;
    }
    if (!types.get(2).getName().equals("CCC")) {
      listOrdered = false;
    }
    if (!types.get(3).getName().equals("DDD")) {
      listOrdered = false;
    }

    assertTrue(listOrdered, "Sensors list not in alphabetical order");
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/user",
    "resources/sql/testbase/instrument",
  })
  @Test
  public void newSensorAssignments() throws Exception {
    // We're not actually testing the SensorAssignments object - that's done
    // in other tests. Here we just need to know that we get something
    assertNotNull(getConfig().getNewSensorAssigments(getDataSource(), 1));
  }

  @FlywayTest
  @Test
  public void missingSensorNameTest() throws SensorConfigurationException {
    List<String> names = new ArrayList<String>(2);
    names.add("Intake Temperature");
    names.add("Flurble");

    assertThrows(SensorConfigurationException.class, () -> {
      getConfig().validateSensorNames(names);
    });
  }


  @FlywayTest
  @Test
  public void validSensorNameTest() throws SensorConfigurationException {
    List<String> names = new ArrayList<String>(2);
    names.add("Intake Temperature");
    names.add("Salinity");
    names.add("Run Type"); // Include the special Run Type for giggles
    getConfig().validateSensorNames(names);
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getParentWithoutParent"
  })
  @Test
  public void getParentWithoutParent() throws SensorConfigurationException {
    SensorsConfiguration config = getConfig();
    SensorType orphan = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Orphan")) {
        orphan = type;
        break;
      }
    }

    assertNull(config.getParent(orphan));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getParentWithParent"
  })
  @Test
  public void getParentWithParent() throws SensorConfigurationException {
    SensorsConfiguration config = getConfig();
    SensorType child = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Child")) {
        child = type;
        break;
      }
    }

    assertNotNull(config.getParent(child));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getSiblingWithNoParent"
  })
  @Test
  public void getSiblingsWithNoParent() throws SensorConfigurationException {
    SensorsConfiguration config = getConfig();
    SensorType orphan = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Orphan")) {
        orphan = type;
        break;
      }
    }

    assertEquals(0, config.getSiblings(orphan).size());
  }


  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getSiblingWithParentNoSiblings"
  })
  @Test
  public void getSiblingsWithParentNoSiblings() throws SensorConfigurationException {
    SensorsConfiguration config = getConfig();
    SensorType child = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Child")) {
        child = type;
        break;
      }
    }

    assertEquals(0, config.getSiblings(child).size());
  }


  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/SensorsConfigurationTest/getSiblingWithParentAndSiblings"
  })
  @Test
  public void getSiblingsWithParentAndSiblings() throws SensorConfigurationException {
   SensorsConfiguration config = getConfig();
   SensorType child = null;

    // Get the 'child' type
    for (SensorType type : config.getSensorTypes()) {
      if (type.getName().equals("Test Child")) {
        child = type;
        break;
      }
    }

    assertEquals(2, config.getSiblings(child).size());
  }

  @Test
  public void getSensorTypeTest() throws SensorConfigurationException {
    try {
      getConfig().getSensorType(1);
    } catch (SensorTypeNotFoundException e) {
      // This exception should not be thrown
      fail("SensorTypeNotFoundException thrown when it shouldn't have been");
    }
  }

  @Test
  public void getInvalidSensorTypeTest() {
    assertThrows(SensorTypeNotFoundException.class, () -> {
      getConfig().getSensorType(-1000);
    });
  }
}
