package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class DataReducerTest extends BaseTest {

  private static List<Long> singleSensorValueList;

  private static List<Long> emptySensorValueList;

  private static HashSet<String> emptyCommentList;

  static {
    singleSensorValueList = new ArrayList<Long>();
    singleSensorValueList.add(1L);

    emptySensorValueList = new ArrayList<Long>();

    emptyCommentList = new HashSet<String>();
  }

  @BeforeEach
  public void init() {
    initResourceManager();
  }

  protected MeasurementValue makeMeasurementValue(String sensorTypeName,
    double value) throws SensorTypeNotFoundException {

    SensorType sensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType(sensorTypeName);

    return new MeasurementValue(sensorType.getId(), singleSensorValueList,
      emptySensorValueList, 1, false, value, Flag.GOOD, emptyCommentList,
      PlotPageTableValue.MEASURED_TYPE, new Properties());
  }

  protected Measurement makeMeasurement(MeasurementValue... measurementValues) {

    HashMap<Long, MeasurementValue> measurementValuesMap = new HashMap<Long, MeasurementValue>();
    for (MeasurementValue measurementValue : measurementValues) {
      measurementValuesMap.put(measurementValue.getSensorType().getId(),
        measurementValue);
    }

    HashMap<Long, String> runTypes = new HashMap<Long, String>();
    runTypes.put(Measurement.RUN_TYPE_DEFINES_VARIABLE, "runtype");

    return new Measurement(1L, 1L, LocalDateTime.now(), runTypes,
      measurementValuesMap);
  }
}
