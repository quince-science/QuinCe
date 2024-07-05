package uk.ac.exeter.QuinCe.web.Instrument;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.SensorCalibrationDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public abstract class CalibrationBeanEditCalibrationsTest extends TestSetTest {

  protected static final String PRIORS_REQUIRED_TARGET = "TARGET1";

  protected static final String PRIORS_NOT_REQUIRED_TARGET = "1001";

  protected static final int NO_POSITION = -1;

  protected static final TreeSet<String> NO_EXPECTED_FAILED_DATASETS = new TreeSet<String>();

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  protected void runTest(int action, long calibrationId, int position,
    boolean affectFollowingOnly, TreeSet<String> expectedAffectedDatasets,
    TreeSet<String> expectedFailedDatasets, String target) throws Exception {

    CalibrationDB dbInstance = null;

    switch (target) {
    case PRIORS_NOT_REQUIRED_TARGET: {
      dbInstance = SensorCalibrationDB.getInstance();
      break;
    }
    case PRIORS_REQUIRED_TARGET: {
      dbInstance = ExternalStandardDB.getInstance();
      break;
    }
    }

    CalibrationBean bean = CalibrationBeanTest.initBean(dbInstance, action,
      calibrationId, getDate(dbInstance, calibrationId, position), target,
      affectFollowingOnly);

    // The affected data sets and boolean flags should all match
    bean.calcAffectedDataSets();
    TreeMap<String, Boolean> affectedDatasets = CalibrationBeanTest
      .getDatasetNamesMap(bean.getAffectedDatasets());

    // Make sure the set of affected datasets is correct
    assertArrayEquals(expectedAffectedDatasets.toArray(),
      affectedDatasets.keySet().toArray());

    List<Boolean> expectedFailedStates = makeExpectedFailedSet(
      affectedDatasets.keySet(), expectedFailedDatasets);

    assertArrayEquals(affectedDatasets.values().toArray(),
      expectedFailedStates.toArray());

  }

  private List<Boolean> makeExpectedFailedSet(Set<String> expectedAffected,
    TreeSet<String> expectedFailed) {

    // The affectedDatasets map contains true if the datasets can be processed,
    // and false if not. So if expectedFails contains a dataset it should be set
    // to false in the map we're comparing to the output from the bean
    return expectedAffected.stream().map(a -> !expectedFailed.contains(a))
      .collect(Collectors.toList());
  }

  private LocalDateTime getDate(CalibrationDB dbInstance, long calibrationId,
    int position) throws Exception {
    LocalDateTime result;

    if (position != NO_POSITION) {
      result = getPositionDate(position);
    } else {
      result = getCalibrationDate(dbInstance, calibrationId);
    }

    return result;
  }

  private LocalDateTime getPositionDate(int position) {

    switch (position) {
    case 1: {
      return LocalDateTime.of(2019, 6, 1, 0, 5, 0);
    }
    case 2: {
      return LocalDateTime.of(2019, 6, 1, 0, 15, 0);
    }
    case 3: {
      return LocalDateTime.of(2019, 6, 1, 1, 5, 0);
    }
    case 4: {
      return LocalDateTime.of(2019, 6, 1, 1, 15, 0);
    }
    case 5: {
      return LocalDateTime.of(2019, 6, 1, 3, 5, 0);
    }
    case 6: {
      return LocalDateTime.of(2019, 6, 1, 3, 15, 0);
    }
    case 7: {
      return LocalDateTime.of(2019, 6, 1, 5, 5, 0);
    }
    case 8: {
      return LocalDateTime.of(2019, 6, 1, 5, 15, 0);
    }
    case 9: {
      return LocalDateTime.of(2019, 6, 1, 6, 5, 0);
    }
    case 10: {
      return LocalDateTime.of(2019, 6, 1, 6, 15, 0);
    }
    default: {
      return null;
    }
    }
  }

  private LocalDateTime getCalibrationDate(CalibrationDB dbInstance,
    long calibrationId) throws Exception {
    CalibrationBean tempBean = CalibrationBeanTest.initBean(dbInstance, true);

    tempBean.setSelectedCalibrationId(calibrationId);
    tempBean.loadSelectedCalibration();
    return tempBean.getCalibration().getDeploymentDate();
  }

  protected long getCalibrationId(char calibrationName, boolean priorRequired) {

    switch (calibrationName) {
    case 'A': {
      return priorRequired ? 2001L : 1001L;
    }
    case 'B': {
      return priorRequired ? 2002L : 1002L;
    }
    case 'C': {
      return priorRequired ? 2003L : 1003L;
    }
    case 'D': {
      return priorRequired ? 2004L : 1004L;
    }
    case 'E': {
      return priorRequired ? 2005L : 1005L;
    }
    default: {
      return Long.MIN_VALUE;
    }
    }
  }

  protected String getTarget(boolean priorRequired) {
    return priorRequired ? PRIORS_REQUIRED_TARGET : PRIORS_NOT_REQUIRED_TARGET;
  }
}
