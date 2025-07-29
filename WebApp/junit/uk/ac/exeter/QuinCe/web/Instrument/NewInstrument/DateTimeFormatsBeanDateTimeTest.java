package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.utils.StringUtils;

@TestInstance(Lifecycle.PER_CLASS)
public class DateTimeFormatsBeanDateTimeTest extends TestSetTest {

  private static final int EXAMPLE_STRING_COL = 0;

  private static final int EXPECTED_FORMATS_COL = 1;

  private static final String ALL_FORMATS_STRING = "ALL";

  private static final List<String> ALL_FORMATS = Arrays.asList(new String[] {
    DateTimeFormatsBean.DT_ISO_D, DateTimeFormatsBean.DT_ISO_MS_D,
    DateTimeFormatsBean.DT_ISO_UTC_NO_SEPARATORS_D,
    DateTimeFormatsBean.DT_NO_SEPARATORS_D,
    DateTimeFormatsBean.DT_ISO_SPACE_OFFSET_D,
    DateTimeFormatsBean.DT_YYYYMMDD_HYPHEN_D,
    DateTimeFormatsBean.DT_YYYYMMDD_HYPHEN_MS_D,
    DateTimeFormatsBean.DT_YYYYMMDD_SLASH_D,
    DateTimeFormatsBean.DT_YYYYMMDD_SLASH_MS_D,
    DateTimeFormatsBean.DT_MMDDYYYY_SLASH_D,
    DateTimeFormatsBean.DT_MMDDYY_SLASH_D,
    DateTimeFormatsBean.DT_DDMMYYYY_SLASH_D,
    DateTimeFormatsBean.DT_DDMMYY_SLASH_D,
    DateTimeFormatsBean.DT_DDMMMYYYY_HYPHEN_D,
    DateTimeFormatsBean.DT_MMDDYYYY_DOT_D, DateTimeFormatsBean.DT_MMDDYY_DOT_D,
    DateTimeFormatsBean.DT_DDMMYYYY_DOT_D,
    DateTimeFormatsBean.DT_DDMMYY_DOT_D });

  @ParameterizedTest
  @MethodSource("getLines")
  public void dateTimeFormatTest(TestSetLine line) throws Exception {

    DateTimeFormatsBean bean = new DateTimeFormatsBean();

    bean.setDateTimeValue(line.getStringField(EXAMPLE_STRING_COL, true));
    Set<String> returnedFormats = bean.getDateTimeFormats().keySet();

    List<String> expectedFormats;
    String expectedFormatsRaw = line.getStringField(EXPECTED_FORMATS_COL,
      false);
    if (expectedFormatsRaw.equals(ALL_FORMATS_STRING)) {
      expectedFormats = ALL_FORMATS;
    } else {
      expectedFormats = StringUtils.delimitedToList(expectedFormatsRaw, ";");
    }

    assertTrue(expectedFormats.size() == returnedFormats.size()
      && returnedFormats.containsAll(expectedFormats));
  }

  @Override
  protected String getTestSetName() {
    return "DateTimeFormatsBeanDateTimeTest";
  }
}
