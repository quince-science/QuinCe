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
public class DateTimeFormatsBeanDateTest extends TestSetTest {

  private static final int EXAMPLE_STRING_COL = 0;

  private static final int EXPECTED_FORMATS_COL = 1;

  private static final String ALL_FORMATS_STRING = "ALL";

  private static final List<String> ALL_FORMATS = Arrays.asList(new String[] {
    DateTimeFormatsBean.D_YYYYMMDD_HYPHEN_D,
    DateTimeFormatsBean.D_DDMMYYYY_HYPHEN_D,
    DateTimeFormatsBean.D_MMDDYYYY_HYPHEN_D,
    DateTimeFormatsBean.D_DDMMYY_HYPHEN_D,
    DateTimeFormatsBean.D_MMDDYY_HYPHEN_D,
    DateTimeFormatsBean.D_DDMMMYYYY_HYPHEN_D,
    DateTimeFormatsBean.D_YYYYMMDD_NOSEP_D,
    DateTimeFormatsBean.D_YYYYMMDD_SLASH_D,
    DateTimeFormatsBean.D_MMDDYYYY_SLASH_D,
    DateTimeFormatsBean.D_DDMMYYYY_SLASH_D,
    DateTimeFormatsBean.D_MMDDYY_SLASH_D, DateTimeFormatsBean.D_DDMMYY_SLASH_D,
    DateTimeFormatsBean.D_MMDDYYYY_DOT_D, DateTimeFormatsBean.D_DDMMYYYY_DOT_D,
    DateTimeFormatsBean.D_MMDDYY_DOT_D, DateTimeFormatsBean.D_DDMMYY_DOT_D });

  @ParameterizedTest
  @MethodSource("getLines")
  public void dateFormatTest(TestSetLine line) throws Exception {

    DateTimeFormatsBean bean = new DateTimeFormatsBean();

    bean.setDateValue(line.getStringField(EXAMPLE_STRING_COL, true));
    Set<String> returnedFormats = bean.getDateFormats().keySet();

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
    return "DateTimeFormatsBeanDateTest";
  }
}
