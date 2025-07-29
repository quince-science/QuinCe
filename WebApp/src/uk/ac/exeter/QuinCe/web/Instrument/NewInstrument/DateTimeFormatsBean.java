package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * Small bean to handle date/time formats and guessing what a format is based on
 * example values.
 */
@ManagedBean
@ViewScoped
public class DateTimeFormatsBean {

  /*
   * _S = Java format _D = Human format
   */

  protected static final String DT_ISO_S = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  public static final DateTimeFormatter DT_ISO_F = DateTimeFormatter
    .ofPattern(DT_ISO_S);

  public static final String DT_ISO_D = "YYYY-MM-DDThh:mm:ssZ";

  protected static final String DT_ISO_MS_S = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  public static final DateTimeFormatter DT_ISO_MS_F = DateTimeFormatter
    .ofPattern(DT_ISO_MS_S);

  public static final String DT_ISO_MS_D = "YYYY-MM-DDThh:mm:ss.SSSZ";

  protected static final String DT_ISO_UTC_NO_SEPARATORS_S = "yyyyMMdd'T'HHmmss'Z'";

  public static final DateTimeFormatter DT_ISO_UTC_NO_SEPARATORS_F = DateTimeFormatter
    .ofPattern(DT_ISO_UTC_NO_SEPARATORS_S);

  public static final String DT_ISO_UTC_NO_SEPARATORS_D = "YYYYMMDDThhmmssZ";

  protected static final String DT_ISO_SPACE_OFFSET_S = "yyyy-MM-dd HH:mm:ssXXX";

  public static final DateTimeFormatter DT_ISO_SPACE_OFFSET_F = DateTimeFormatter
    .ofPattern(DT_ISO_SPACE_OFFSET_S);

  public static final String DT_NO_SEPARATORS_D = "YYYYMMDDhhmmss";

  public static final String DT_NO_SEPARATORS_S = "yyyyMMddHHmmss";

  public static final DateTimeFormatter DT_NO_SEPARATORS_F = DateTimeFormatter
    .ofPattern(DT_NO_SEPARATORS_S);

  public static final String DT_ISO_SPACE_OFFSET_D = "YYYY-MM-DD hh:mm:ss+00:00";

  protected static final String DT_YYYYMMDD_HYPHEN_S = "yyyy-MM-dd HH:mm:ss";

  public static final DateTimeFormatter DT_YYYYMMDD_HYPHEN_F = DateTimeFormatter
    .ofPattern(DT_YYYYMMDD_HYPHEN_S);

  public static final String DT_YYYYMMDD_HYPHEN_D = "YYYY-MM-DD hh:mm:ss";

  protected static final String DT_YYYYMMDD_HYPHEN_MS_S = "yyyy-MM-dd HH:mm:ss.SSS";

  public static final DateTimeFormatter DT_YYYYMMDD_HYPHEN_MS_F = DateTimeFormatter
    .ofPattern(DT_YYYYMMDD_HYPHEN_MS_S);

  public static final String DT_YYYYMMDD_HYPHEN_MS_D = "YYYY-MM-DD hh:mm:ss.SSS";

  protected static final String DT_YYYYMMDD_SLASH_S = "yyyy/MM/dd HH:mm:ss";

  public static final DateTimeFormatter DT_YYYYMMDD_SLASH_F = DateTimeFormatter
    .ofPattern(DT_YYYYMMDD_SLASH_S);

  public static final String DT_YYYYMMDD_SLASH_D = "YYYY/MM/DD hh:mm:ss";

  protected static final String DT_YYYYMMDD_SLASH_MS_S = "yyyy/MM/dd HH:mm:ss.SSS";

  public static final DateTimeFormatter DT_YYYYMMDD_SLASH_MS_F = DateTimeFormatter
    .ofPattern(DT_YYYYMMDD_SLASH_MS_S);

  public static final String DT_YYYYMMDD_SLASH_MS_D = "YYYY/MM/DD hh:mm:ss.SSS";

  protected static final String DT_MMDDYYYY_SLASH_S = "MM/dd/yyyy HH:mm:ss";

  public static final DateTimeFormatter DT_MMDDYYYY_SLASH_F = DateTimeFormatter
    .ofPattern(DT_MMDDYYYY_SLASH_S);

  public static final String DT_MMDDYYYY_SLASH_D = "MM/DD/YYYY hh:mm:ss";

  protected static final String DT_MMDDYY_SLASH_S = "MM/dd/yy HH:mm:ss";

  public static final DateTimeFormatter DT_MMDDYY_SLASH_F = DateTimeFormatter
    .ofPattern(DT_MMDDYY_SLASH_S);

  public static final String DT_MMDDYY_SLASH_D = "MM/DD/YY hh:mm:ss";

  protected static final String DT_DDMMYYYY_SLASH_S = "dd/MM/yyyy HH:mm:ss";

  public static final DateTimeFormatter DT_DDMMYYYY_SLASH_F = DateTimeFormatter
    .ofPattern(DT_DDMMYYYY_SLASH_S);

  public static final String DT_DDMMYYYY_SLASH_D = "DD/MM/YYYY hh:mm:ss";

  protected static final String DT_DDMMYY_SLASH_S = "dd/MM/yy HH:mm:ss";

  public static final DateTimeFormatter DT_DDMMYY_SLASH_F = DateTimeFormatter
    .ofPattern(DT_DDMMYY_SLASH_S);

  public static final String DT_DDMMYY_SLASH_D = "DD/MM/YY hh:mm:ss";

  protected static final String DT_DDMMMYYYY_HYPHEN_S = "dd-MMM-yyyy HH:mm:ss";

  public static final DateTimeFormatter DT_DDMMMYYYY_HYPHEN_F = DateTimeFormatter
    .ofPattern(DT_DDMMMYYYY_HYPHEN_S);

  public static final String DT_DDMMMYYYY_HYPHEN_D = "DD-MMM-YYYY hh:mm:ss";

  protected static final String DT_MMDDYYYY_DOT_S = "MM.dd.yyyy HH:mm:ss";

  public static final DateTimeFormatter DT_MMDDYYYY_DOT_F = DateTimeFormatter
    .ofPattern(DT_MMDDYYYY_DOT_S);

  public static final String DT_MMDDYYYY_DOT_D = "MM.DD.YYYY hh:mm:ss";

  protected static final String DT_MMDDYY_DOT_S = "MM.dd.yy HH:mm:ss";

  public static final DateTimeFormatter DT_MMDDYY_DOT_F = DateTimeFormatter
    .ofPattern(DT_MMDDYY_DOT_S);

  public static final String DT_MMDDYY_DOT_D = "MM.DD.YY hh:mm:ss";

  protected static final String DT_DDMMYYYY_DOT_S = "dd.MM.yyyy HH:mm:ss";

  public static final DateTimeFormatter DT_DDMMYYYY_DOT_F = DateTimeFormatter
    .ofPattern(DT_DDMMYYYY_DOT_S);

  public static final String DT_DDMMYYYY_DOT_D = "DD.MM.YYYY hh:mm:ss";

  protected static final String DT_DDMMYY_DOT_S = "dd.MM.yy HH:mm:ss";

  public static final DateTimeFormatter DT_DDMMYY_DOT_F = DateTimeFormatter
    .ofPattern(DT_DDMMYY_DOT_S);

  public static final String DT_DDMMYY_DOT_D = "DD.MM.YY hh:mm:ss";

  protected static final String D_YYYYMMDD_HYPHEN_S = "yyyy-MM-dd";

  public static final DateTimeFormatter D_YYYYMMDD_HYPHEN_F = DateTimeFormatter
    .ofPattern(D_YYYYMMDD_HYPHEN_S);

  public static final String D_YYYYMMDD_HYPHEN_D = "YYYY-MM-DD";

  protected static final String D_DDMMYYYY_HYPHEN_S = "dd-MM-yyyy";

  public static final DateTimeFormatter D_DDMMYYYY_HYPHEN_F = DateTimeFormatter
    .ofPattern(D_DDMMYYYY_HYPHEN_S);

  public static final String D_DDMMYYYY_HYPHEN_D = "DD-MM-YYYY";

  protected static final String D_MMDDYYYY_HYPHEN_S = "MM-dd-yyyy";

  public static final DateTimeFormatter D_MMDDYYYY_HYPHEN_F = DateTimeFormatter
    .ofPattern(D_MMDDYYYY_HYPHEN_S);

  public static final String D_MMDDYYYY_HYPHEN_D = "MM-DD-YYYY";

  protected static final String D_DDMMYY_HYPHEN_S = "dd-MM-yy";

  public static final DateTimeFormatter D_DDMMYY_HYPHEN_F = DateTimeFormatter
    .ofPattern(D_DDMMYY_HYPHEN_S);

  public static final String D_DDMMYY_HYPHEN_D = "DD-MM-YY";

  protected static final String D_MMDDYY_HYPHEN_S = "MM-dd-yy";

  public static final DateTimeFormatter D_MMDDYY_HYPHEN_F = DateTimeFormatter
    .ofPattern(D_MMDDYY_HYPHEN_S);

  public static final String D_MMDDYY_HYPHEN_D = "MM-DD-YY";

  protected static final String D_DDMMMYYYY_HYPHEN_S = "dd-MMM-yyyy";

  public static final DateTimeFormatter D_DDMMMYYYY_HYPHEN_F = DateTimeFormatter
    .ofPattern(D_DDMMMYYYY_HYPHEN_S);

  public static final String D_DDMMMYYYY_HYPHEN_D = "DD-MMM-YYYY";

  protected static final String D_YYYYMMDD_NOSEP_S = "yyyyMMdd";

  public static final DateTimeFormatter D_YYYYMMDD_NOSEP_F = DateTimeFormatter
    .ofPattern(D_YYYYMMDD_NOSEP_S);

  public static final String D_YYYYMMDD_NOSEP_D = "YYYYMMDD";

  protected static final String D_YYYYMMDD_SLASH_S = "yyyy/MM/dd";

  public static final DateTimeFormatter D_YYYYMMDD_SLASH_F = DateTimeFormatter
    .ofPattern(D_YYYYMMDD_SLASH_S);

  public static final String D_YYYYMMDD_SLASH_D = "YYYY/MM/DD";

  protected static final String D_MMDDYYYY_SLASH_S = "MM/dd/yyyy";

  public static final DateTimeFormatter D_MMDDYYYY_SLASH_F = DateTimeFormatter
    .ofPattern(D_MMDDYYYY_SLASH_S);

  public static final String D_MMDDYYYY_SLASH_D = "MM/DD/YYYY";

  protected static final String D_DDMMYYYY_SLASH_S = "dd/MM/yyyy";

  public static final DateTimeFormatter D_DDMMYYYY_SLASH_F = DateTimeFormatter
    .ofPattern(D_DDMMYYYY_SLASH_S);

  public static final String D_DDMMYYYY_SLASH_D = "DD/MM/YYYY";

  protected static final String D_MMDDYY_SLASH_S = "MM/dd/yy";

  public static final DateTimeFormatter D_MMDDYY_SLASH_F = DateTimeFormatter
    .ofPattern(D_MMDDYY_SLASH_S);

  public static final String D_MMDDYY_SLASH_D = "MM/DD/YY";

  protected static final String D_DDMMYY_SLASH_S = "dd/MM/yy";

  public static final DateTimeFormatter D_DDMMYY_SLASH_F = DateTimeFormatter
    .ofPattern(D_DDMMYY_SLASH_S);

  public static final String D_DDMMYY_SLASH_D = "DD/MM/YY";

  protected static final String D_MMDDYYYY_DOT_S = "MM.dd.yyyy";

  public static final DateTimeFormatter D_MMDDYYYY_DOT_F = DateTimeFormatter
    .ofPattern(D_MMDDYYYY_DOT_S);

  public static final String D_MMDDYYYY_DOT_D = "MM.DD.YYYY";

  protected static final String D_DDMMYYYY_DOT_S = "dd.MM.yyyy";

  public static final DateTimeFormatter D_DDMMYYYY_DOT_F = DateTimeFormatter
    .ofPattern(D_DDMMYYYY_DOT_S);

  public static final String D_DDMMYYYY_DOT_D = "DD.MM.YYYY";

  protected static final String D_MMDDYY_DOT_S = "MM.dd.yy";

  public static final DateTimeFormatter D_MMDDYY_DOT_F = DateTimeFormatter
    .ofPattern(D_MMDDYY_DOT_S);

  public static final String D_MMDDYY_DOT_D = "MM.DD.YY";

  protected static final String D_DDMMYY_DOT_S = "dd.MM.yy";

  public static final DateTimeFormatter D_DDMMYY_DOT_F = DateTimeFormatter
    .ofPattern(D_DDMMYY_DOT_S);

  public static final String D_DDMMYY_DOT_D = "DD.MM.YY";

  protected static final String T_HHMMSS_COLON_S = "HH:mm:ss";

  public static final DateTimeFormatter T_HHMMSS_COLON_F = DateTimeFormatter
    .ofPattern(T_HHMMSS_COLON_S);

  public static final String T_HHMMSS_COLON_D = "hh:mm:ss";

  protected static final String T_HHMMSS_COLON_MS_S = "HH:mm:ss.SSS";

  public static final DateTimeFormatter T_HHMMSS_COLON_MS_F = DateTimeFormatter
    .ofPattern(T_HHMMSS_COLON_MS_S);

  public static final String T_HHMMSS_COLON_MS_D = "hh:mm:ss.SSS";

  protected static final String T_HHMMSS_NOSEP_S = "HHmmss";

  public static final DateTimeFormatter T_HHMMSS_NOSEP_F = DateTimeFormatter
    .ofPattern(T_HHMMSS_NOSEP_S);

  public static final String T_HHMMSS_NOSEP_D = "hhmmss";

  /**
   * The example date/time value used to guess the date/time format.
   */
  private String dateTimeValue = null;

  /**
   * The example date value used to guess the date format.
   */
  private String dateValue = null;

  /**
   * The example time value used to guess the date format.
   */
  private String timeValue = null;

  public Map<String, String> getDateTimeFormats() {

    LinkedHashMap<String, String> formats = new LinkedHashMap<String, String>();

    if (null != dateTimeValue) {
      // Find all formats that can parse the example value and include those

      try {
        LocalDateTime.parse(dateTimeValue, DT_ISO_F);
        formats.put(DT_ISO_D, DT_ISO_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_ISO_MS_F);
        formats.put(DT_ISO_MS_D, DT_ISO_MS_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_ISO_UTC_NO_SEPARATORS_F);
        formats.put(DT_ISO_UTC_NO_SEPARATORS_D, DT_ISO_UTC_NO_SEPARATORS_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_NO_SEPARATORS_F);
        formats.put(DT_NO_SEPARATORS_D, DT_NO_SEPARATORS_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_ISO_SPACE_OFFSET_F);
        formats.put(DT_ISO_SPACE_OFFSET_D, DT_ISO_SPACE_OFFSET_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_YYYYMMDD_HYPHEN_F);
        formats.put(DT_YYYYMMDD_HYPHEN_D, DT_YYYYMMDD_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_YYYYMMDD_HYPHEN_MS_F);
        formats.put(DT_YYYYMMDD_HYPHEN_MS_D, DT_YYYYMMDD_HYPHEN_MS_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_YYYYMMDD_SLASH_F);
        formats.put(DT_YYYYMMDD_SLASH_D, DT_YYYYMMDD_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_YYYYMMDD_SLASH_MS_F);
        formats.put(DT_YYYYMMDD_SLASH_MS_D, DT_YYYYMMDD_SLASH_MS_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_MMDDYYYY_SLASH_F);
        formats.put(DT_MMDDYYYY_SLASH_D, DT_MMDDYYYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_MMDDYY_SLASH_F);
        formats.put(DT_MMDDYY_SLASH_D, DT_MMDDYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_DDMMYYYY_SLASH_F);
        formats.put(DT_DDMMYYYY_SLASH_D, DT_DDMMYYYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_DDMMYY_SLASH_F);
        formats.put(DT_DDMMYY_SLASH_D, DT_DDMMYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_DDMMMYYYY_HYPHEN_F);
        formats.put(DT_DDMMMYYYY_HYPHEN_D, DT_DDMMMYYYY_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_MMDDYYYY_DOT_F);
        formats.put(DT_MMDDYYYY_DOT_D, DT_MMDDYYYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_MMDDYY_DOT_F);
        formats.put(DT_MMDDYY_DOT_D, DT_MMDDYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_DDMMYYYY_DOT_F);
        formats.put(DT_DDMMYYYY_DOT_D, DT_DDMMYYYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDateTime.parse(dateTimeValue, DT_DDMMYY_DOT_F);
        formats.put(DT_DDMMYY_DOT_D, DT_DDMMYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }
    }

    // If we couldn't get a format, list all of them.
    if (formats.size() == 0) {
      formats.put(DT_ISO_D, DT_ISO_S);
      formats.put(DT_ISO_MS_D, DT_ISO_MS_S);
      formats.put(DT_ISO_UTC_NO_SEPARATORS_D, DT_ISO_UTC_NO_SEPARATORS_S);
      formats.put(DT_NO_SEPARATORS_D, DT_NO_SEPARATORS_S);
      formats.put(DT_ISO_SPACE_OFFSET_D, DT_ISO_SPACE_OFFSET_S);
      formats.put(DT_YYYYMMDD_HYPHEN_D, DT_YYYYMMDD_HYPHEN_S);
      formats.put(DT_YYYYMMDD_HYPHEN_MS_D, DT_YYYYMMDD_HYPHEN_MS_S);
      formats.put(DT_YYYYMMDD_SLASH_D, DT_YYYYMMDD_SLASH_S);
      formats.put(DT_YYYYMMDD_SLASH_MS_D, DT_YYYYMMDD_SLASH_MS_S);
      formats.put(DT_MMDDYYYY_SLASH_D, DT_MMDDYYYY_SLASH_S);
      formats.put(DT_MMDDYY_SLASH_D, DT_MMDDYY_SLASH_S);
      formats.put(DT_DDMMYYYY_SLASH_D, DT_DDMMYYYY_SLASH_S);
      formats.put(DT_DDMMYY_SLASH_D, DT_DDMMYY_SLASH_S);
      formats.put(DT_DDMMMYYYY_HYPHEN_D, DT_DDMMMYYYY_HYPHEN_S);
      formats.put(DT_MMDDYYYY_DOT_D, DT_MMDDYYYY_DOT_S);
      formats.put(DT_MMDDYY_DOT_D, DT_MMDDYY_DOT_S);
      formats.put(DT_DDMMYYYY_DOT_D, DT_DDMMYYYY_DOT_S);
      formats.put(DT_DDMMYY_DOT_D, DT_DDMMYY_DOT_S);
    }

    return formats;
  }

  public Map<String, String> getDateFormats() {

    LinkedHashMap<String, String> formats = new LinkedHashMap<String, String>();
    if (null != dateValue) {
      // Find all formats that can parse the example value and include those

      try {
        LocalDate.parse(dateValue, D_YYYYMMDD_HYPHEN_F);
        formats.put("YYYY-MM-DD", D_YYYYMMDD_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_DDMMYYYY_HYPHEN_F);
        formats.put("DD-MM-YYYY", D_DDMMYYYY_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_MMDDYYYY_HYPHEN_F);
        formats.put(D_MMDDYYYY_HYPHEN_D, D_MMDDYYYY_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_DDMMYY_HYPHEN_F);
        formats.put(D_DDMMYY_HYPHEN_D, D_DDMMYY_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_MMDDYY_HYPHEN_F);
        formats.put("MM-DD-YY", D_MMDDYY_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_DDMMMYYYY_HYPHEN_F);
        formats.put(D_DDMMMYYYY_HYPHEN_D, D_DDMMMYYYY_HYPHEN_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_YYYYMMDD_NOSEP_F);
        formats.put(D_YYYYMMDD_NOSEP_D, D_YYYYMMDD_NOSEP_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_YYYYMMDD_SLASH_F);
        formats.put(D_YYYYMMDD_SLASH_D, D_YYYYMMDD_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_MMDDYYYY_SLASH_F);
        formats.put(D_MMDDYYYY_SLASH_D, D_MMDDYYYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_DDMMYYYY_SLASH_F);
        formats.put(D_DDMMYYYY_SLASH_D, D_DDMMYYYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_MMDDYY_SLASH_F);
        formats.put(D_MMDDYY_SLASH_D, D_MMDDYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_DDMMYY_SLASH_F);
        formats.put(D_DDMMYY_SLASH_D, D_DDMMYY_SLASH_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_MMDDYYYY_DOT_F);
        formats.put(D_MMDDYYYY_DOT_D, D_MMDDYYYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_DDMMYYYY_DOT_F);
        formats.put(D_DDMMYYYY_DOT_D, D_DDMMYYYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_MMDDYY_DOT_F);
        formats.put(D_MMDDYY_DOT_D, D_MMDDYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalDate.parse(dateValue, D_DDMMYY_DOT_F);
        formats.put(D_DDMMYY_DOT_D, D_DDMMYY_DOT_S);
      } catch (DateTimeParseException e) {
        // noop
      }
    }

    // If we couldn't get a format, list all of them.
    if (formats.size() == 0) {
      formats.put(D_YYYYMMDD_HYPHEN_D, D_YYYYMMDD_HYPHEN_S);
      formats.put(D_DDMMYYYY_HYPHEN_D, D_DDMMYYYY_HYPHEN_S);
      formats.put(D_MMDDYYYY_HYPHEN_D, D_MMDDYYYY_HYPHEN_S);
      formats.put(D_DDMMYY_HYPHEN_D, D_DDMMYY_HYPHEN_S);
      formats.put(D_MMDDYY_HYPHEN_D, D_MMDDYY_HYPHEN_S);
      formats.put(D_DDMMMYYYY_HYPHEN_D, D_DDMMMYYYY_HYPHEN_S);
      formats.put(D_YYYYMMDD_NOSEP_D, D_YYYYMMDD_NOSEP_S);
      formats.put(D_YYYYMMDD_SLASH_D, D_YYYYMMDD_SLASH_S);
      formats.put(D_MMDDYYYY_SLASH_D, D_MMDDYYYY_SLASH_S);
      formats.put(D_DDMMYYYY_SLASH_D, D_DDMMYYYY_SLASH_S);
      formats.put(D_MMDDYY_SLASH_D, D_MMDDYY_SLASH_S);
      formats.put(D_DDMMYY_SLASH_D, D_DDMMYY_SLASH_S);
      formats.put(D_MMDDYYYY_DOT_D, D_MMDDYYYY_DOT_S);
      formats.put(D_DDMMYYYY_DOT_D, D_DDMMYYYY_DOT_S);
      formats.put(D_MMDDYY_DOT_D, D_MMDDYY_DOT_S);
      formats.put(D_DDMMYY_DOT_D, D_DDMMYY_DOT_S);
    }

    return formats;
  }

  public Map<String, String> getTimeFormats() {

    LinkedHashMap<String, String> formats = new LinkedHashMap<String, String>();
    if (null != timeValue) {
      // Find all formats that can parse the example value and include those

      try {
        LocalTime.parse(timeValue, T_HHMMSS_COLON_F);
        formats.put(T_HHMMSS_COLON_D, T_HHMMSS_COLON_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalTime.parse(timeValue, T_HHMMSS_COLON_MS_F);
        formats.put(T_HHMMSS_COLON_MS_D, T_HHMMSS_COLON_MS_S);
      } catch (DateTimeParseException e) {
        // noop
      }

      try {
        LocalTime.parse(timeValue, T_HHMMSS_NOSEP_F);
        formats.put(T_HHMMSS_NOSEP_D, T_HHMMSS_NOSEP_S);
      } catch (DateTimeParseException e) {
        // noop
      }
    }

    if (formats.size() == 0) {
      formats.put(T_HHMMSS_COLON_D, T_HHMMSS_COLON_S);
      formats.put(T_HHMMSS_COLON_MS_D, T_HHMMSS_COLON_MS_S);
      formats.put(T_HHMMSS_NOSEP_D, T_HHMMSS_NOSEP_S);

    }

    return formats;
  }

  /**
   * Get the example date/time value.
   *
   * <p>
   * The value should be treated as write-only, to be set on the front end.
   * </p>
   *
   * @return The example date/time value.
   */
  public String getDateTimeValue() {
    return dateTimeValue;
  }

  /**
   * Set the example date/time value.
   *
   * @param dateTimeValue
   *          The date/time value.
   */
  public void setDateTimeValue(String dateTimeValue) {
    this.dateTimeValue = dateTimeValue;
  }

  /**
   * Get the example date value.
   *
   * <p>
   * The value should be treated as write-only, to be set on the front end.
   * </p>
   *
   * @return The example date value.
   */
  public String getDateValue() {
    return dateValue;
  }

  /**
   * Set the example date value.
   *
   * @param dateTimeValue
   *          The date value.
   */
  public void setDateValue(String dateValue) {
    this.dateValue = dateValue;
  }

  /**
   * Get the example time value.
   *
   * <p>
   * The value should be treated as write-only, to be set on the front end.
   * </p>
   *
   * @return The example time value.
   */
  public String getTimeValue() {
    return timeValue;
  }

  /**
   * Set the example time value.
   *
   * @param timeValue
   *          The time value.
   */
  public void setTimeValue(String timeValue) {
    this.timeValue = timeValue;
  }
}
