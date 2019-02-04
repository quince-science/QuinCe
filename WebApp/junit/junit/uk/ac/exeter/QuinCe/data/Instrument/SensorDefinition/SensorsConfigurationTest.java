package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.DBTest;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;

/**
 * Tests in this file assume that there are some valid sensor types already
 * configured in the database, but create their own types for specific testing
 *
 * @author Steve Jones
 *
 */
public class SensorsConfigurationTest extends DBTest {

  @FlywayTest
  @Test
  public void testLoadConfiguration() throws Exception {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());
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
    "resources/sql/data/Instrument/SensorDefinition/nonExistentParent"
  })
  @Test
  public void nonExistentParentTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorsConfiguration(getDataSource());
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/nonExistentDependsOn"
  })
  @Test
  public void nonExistentDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorsConfiguration(getDataSource());
    });
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/sensorTypesList"
  })
  @Test
  public void sensorTypesListTest() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());
    List<SensorType> types = config.getSensorTypes();

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
    "resources/sql/data/Instrument/SensorDefinition/newSensorAssignments"
  })
  @Test
  public void newSensorAssignments() throws Exception {
    // Mock an Instrument that matches the partial instrument stored in the database
    Instrument mockInstrument = Mockito.mock(Instrument.class);
    Mockito.when(mockInstrument.getDatabaseId()).thenReturn(1L);

    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

    // We're not actually testing the SensorAssignments object - that's done
    // in other tests. Here we just need to know that we get something
    assertNotNull(config.getNewSensorAssigments(getDataSource(), mockInstrument));
  }

  @FlywayTest
  @Test
  public void missingSensorNameTest() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

    List<String> names = new ArrayList<String>(2);
    names.add("Intake Temperature");
    names.add("Flurble");

    assertThrows(SensorConfigurationException.class, () -> {
      config.validateSensorNames(names);
    });
  }


  @FlywayTest
  @Test
  public void validSensorNameTest() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

    List<String> names = new ArrayList<String>(2);
    names.add("Intake Temperature");
    names.add("Salinity");
    names.add("Run Type"); // Include the special Run Type for giggles
    config.validateSensorNames(names);
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/Instrument/SensorDefinition/getParentWithoutParent"
  })
  @Test
  public void getParentWithoutParent() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

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
    "resources/sql/data/Instrument/SensorDefinition/getParentWithParent"
  })
  @Test
  public void getParentWithParent() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

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
    "resources/sql/data/Instrument/SensorDefinition/getSiblingWithNoParent"
  })
  @Test
  public void getSiblingsWithNoParent() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

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
    "resources/sql/data/Instrument/SensorDefinition/getSiblingWithParentNoSiblings"
  })
  @Test
  public void getSiblingsWithParentNoSiblings() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

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
    "resources/sql/data/Instrument/SensorDefinition/getSiblingWithParentAndSiblings"
  })
  @Test
  public void getSiblingsWithParentAndSiblings() throws SensorConfigurationException {
    SensorsConfiguration config = new SensorsConfiguration(getDataSource());

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
}
