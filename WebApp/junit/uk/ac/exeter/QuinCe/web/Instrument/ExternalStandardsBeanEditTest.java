package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;

public abstract class ExternalStandardsBeanEditTest extends TestSetTest {

  private static final long USER_ID = 1L;

  private static final long INSTRUMENT_ID = 1L;

  protected static final String REPLACEMENT_VALUE = "1000";

  protected ExternalStandardsBean init() throws Exception {
    initResourceManager();
    loginUser(USER_ID);
    ExternalStandardsBean bean = new ExternalStandardsBean();
    bean.setInstrumentId(INSTRUMENT_ID);
    bean.start();
    return bean;
  }

  protected String makeTestString(List<Long> ids, List<Boolean> canReprocess) {
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < ids.size(); i++) {
      result.append(ids.get(i));
      result.append(':');
      result.append(canReprocess.get(i));
      result.append(';');
    }

    return result.toString();
  }

  protected String makeTestString(TreeMap<Long, Boolean> input) {
    StringBuilder result = new StringBuilder();

    for (Map.Entry<Long, Boolean> entry : input.entrySet()) {
      result.append(entry.getKey());
      result.append(':');
      result.append(entry.getValue());
      result.append(';');
    }

    return result.toString();
  }

  protected int getAction(TestSetLine line, int actionCol) {
    int result;

    switch (line.getStringField(actionCol, false)) {
    case "ADD": {
      result = CalibrationEdit.ADD;
      break;
    }
    case "EDIT": {
      result = CalibrationEdit.EDIT;
      break;
    }
    case "DELETE": {
      result = CalibrationEdit.DELETE;
      break;
    }
    default: {
      throw new IllegalArgumentException(
        "Invalid action '" + line.getStringField(actionCol, false));
    }
    }

    return result;
  }

  protected LocalDateTime getCalibrationTime(int month) {
    return month == 0 ? null : LocalDateTime.of(2023, month, 1, 0, 0, 0);
  }
}
