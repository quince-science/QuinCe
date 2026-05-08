package uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class MissingParamTest extends BaseTest {

  /**
   * Test the {@code char} array checker (with no empty option) with a populated
   * array.
   */
  @Test
  public void charArrayNoEmptyFlagNotEmptyTest() {
    char[] array = new char[] { 'a' };
    assertDoesNotThrow(() -> MissingParam.checkMissing(array, "ARRAY"));
  }

  /**
   * Test the {@code char} array checker (with no empty option) with a
   * {@code null} value.
   */
  @Test
  public void charArrayNullTest() {
    char[] array = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY"));
  }

  /**
   * Test the {@code char} array checker (with no empty option) with an empty
   * array.
   */
  @Test
  public void charArrayEmptyTest() {
    char[] array = new char[] {};
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY"));
  }

  /**
   * Test the {@code char} array checker (with the empty option set) with a
   * populated array.
   */
  @Test
  public void charArrayOptionalEmptyFlagNotSetNotEmptyTest() {
    char[] array = new char[] { 'a' };
    assertDoesNotThrow(() -> MissingParam.checkMissing(array, "ARRAY", false));
  }

  /**
   * Test the {@code char} array checker (with the empty option not set) with a
   * populated array.
   */
  @Test
  public void charArrayOptionalEmptyFlagSetNotEmptyTest() {
    char[] array = new char[] { 'a' };
    assertDoesNotThrow(() -> MissingParam.checkMissing(array, "ARRAY", true));
  }

  /**
   * Test the {@code char} array checker (with the empty option set) with a
   * {@code null} value.
   */
  @Test
  public void charArrayOptionalEmptyFlagNotSetNullTest() {
    char[] array = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY"));
  }

  /**
   * Test the {@code char} array checker (with the empty option not set) with a
   * {@code null} value.
   */
  @Test
  public void charArrayOptionalEmptyFlagSetNullTest() {
    char[] array = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY"));
  }

  /**
   * Test the {@code char} array checker (with the empty option set) with an
   * empty array.
   */
  @Test
  public void charArrayOptionalEmptyFlagNotSetEmptyTest() {
    char[] array = new char[] {};
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY"));
  }

  /**
   * Test the {@code char} array checker (with the empty option not set) with an
   * empty array.
   */
  @Test
  public void charArrayOptionalEmptyFlagSetEmptyTest() {
    char[] array = new char[] {};
    assertDoesNotThrow(() -> MissingParam.checkMissing(array, "ARRAY", true));
  }

  /**
   * Test the {@code int} array checker (with the empty option set) with a
   * populated array.
   */
  @Test
  public void intArrayOptionalEmptyFlagNotSetNotEmptyTest() {
    int[] array = new int[] { 21 };
    assertDoesNotThrow(() -> MissingParam.checkMissing(array, "ARRAY", false));
  }

  /**
   * Test the {@code int} array checker (with the empty option not set) with a
   * populated array.
   */
  @Test
  public void intArrayOptionalEmptyFlagSetNotEmptyTest() {
    int[] array = new int[] { 21 };
    assertDoesNotThrow(() -> MissingParam.checkMissing(array, "ARRAY", true));
  }

  /**
   * Test the {@code int} array checker (with the empty option set) with a
   * {@code null} value.
   */
  @Test
  public void intArrayOptionalEmptyFlagNotSetNullTest() {
    int[] array = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY", true));
  }

  /**
   * Test the {@code int} array checker (with the empty option not set) with a
   * {@code null} value.
   */
  @Test
  public void intArrayOptionalEmptyFlagSetNullTest() {
    int[] array = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY", false));
  }

  /**
   * Test the {@code int} array checker (with the empty option set) with an
   * empty array.
   */
  @Test
  public void intArrayOptionalEmptyFlagSetEmptyTest() {
    int[] array = new int[] {};
    assertDoesNotThrow(() -> MissingParam.checkMissing(array, "ARRAY", true));
  }

  /**
   * Test the {@code int} array checker (with the empty option not set) with an
   * empty array.
   */
  @Test
  public void intArrayOptionalEmptyFlagNotSetEmptyTest() {
    int[] array = new int[] {};
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(array, "ARRAY", false));
  }

  /**
   * Test the {@link String} checker (with no empty option) with a populated
   * array.
   */
  @Test
  public void stringNoEmptyFlagNotEmptyTest() {
    String str = "a";
    assertDoesNotThrow(() -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with no empty option) with a populated
   * array.
   */
  @Test
  public void stringNoEmptyFlagNotEmptyTrimmedTest() {
    String str = "a ";
    assertDoesNotThrow(() -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with no empty option) with a {@code null}
   * value.
   */
  @Test
  public void stringNullTest() {
    String str = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with no empty option) with an empty array.
   */
  @Test
  public void stringEmptyTest() {
    String str = "";
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with no empty option) with an empty array.
   */
  @Test
  public void stringEmptyTrimmedTest() {
    String str = "  \t";
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with the empty option set) with a
   * populated array.
   */
  @Test
  public void stringOptionalEmptyFlagNotSetNotEmptyTest() {
    String str = "a";
    assertDoesNotThrow(() -> MissingParam.checkMissing(str, "STRING", false));
  }

  /**
   * Test the {@link String} checker (with the empty option not set) with a
   * populated array.
   */
  @Test
  public void stringOptionalEmptyFlagSetNotEmptyTest() {
    String str = "a";
    assertDoesNotThrow(() -> MissingParam.checkMissing(str, "STRING", true));
  }

  /**
   * Test the {@link String} checker (with the empty option set) with a
   * {@code null} value.
   */
  @Test
  public void stringOptionalEmptyFlagNotSetNullTest() {
    String str = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with the empty option not set) with a
   * {@code null} value.
   */
  @Test
  public void stringOptionalEmptyFlagSetNullTest() {
    String str = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with the empty option set) with an empty
   * array.
   */
  @Test
  public void stringOptionalEmptyFlagNotSetEmptyTest() {
    String str = "";
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with the empty option set) with an empty
   * array.
   */
  @Test
  public void stringOptionalEmptyFlagNotSetEmptyTrimmedTest() {
    String str = "  \t";
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(str, "STRING"));
  }

  /**
   * Test the {@link String} checker (with the empty option not set) with an
   * empty array.
   */
  @Test
  public void stringOptionalEmptyFlagSetEmptyTest() {
    String str = "";
    assertDoesNotThrow(() -> MissingParam.checkMissing(str, "STRING", true));
  }

  /**
   * Test the {@link String} checker (with the empty option not set) with an
   * empty array.
   */
  @Test
  public void stringOptionalEmptyFlagSetEmptyTrimmedTest() {
    String str = "  \t";
    assertDoesNotThrow(() -> MissingParam.checkMissing(str, "STRING", true));
  }

  /**
   * Check a {@code null} database connection.
   */
  @Test
  public void connectionNullTest() {
    Connection conn = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(conn, "CONN"));
  }

  /**
   * Check an active database connection.
   */
  @FlywayTest
  @Test
  public void connectionValidTest() throws SQLException {
    initResourceManager();
    Connection conn = ResourceManager.getInstance().getDBDataSource()
      .getConnection();
    assertDoesNotThrow(() -> MissingParam.checkMissing(conn, "CONN"));
  }

  /**
   * Check a closed database connection.
   */
  @FlywayTest
  @Test
  public void connectionClosedTest() throws SQLException {
    initResourceManager();
    Connection conn = ResourceManager.getInstance().getDBDataSource()
      .getConnection();
    conn.close();
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(conn, "CONN"));
  }

  /**
   * Test a random {@code null} object.
   */
  @Test
  public void nullObjectTest() {
    LocalDateTime time = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(time, "OBJECT"));
  }

  /**
   * Test a random non-{@code null} object.
   */
  @Test
  public void nonNullObjectTest() {
    LocalDateTime time = LocalDateTime.now();
    assertDoesNotThrow(() -> MissingParam.checkMissing(time, "OBJECT"));
  }

  /**
   * Test the {@link Collection} array checker (with no empty option) with a
   * populated array.
   */
  @Test
  public void collectionNoEmptyFlagNotEmptyTest() {
    Set<String> set = new TreeSet<String>();
    set.add("a");
    assertDoesNotThrow(() -> MissingParam.checkMissing(set, "SET"));
  }

  /**
   * Test the {@link Collection} array checker (with no empty option) with a
   * {@code null} value.
   */
  @Test
  public void collectionNullTest() {
    Set<String> set = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(set, "SET"));
  }

  /**
   * Test the {@link Collection} array checker (with no empty option) with an
   * empty array.
   */
  @Test
  public void collectionEmptyTest() {
    Set<String> set = new TreeSet<String>();
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(set, "SET"));
  }

  /**
   * Test the {@link Collection} array checker (with the empty option set) with
   * a populated array.
   */
  @Test
  public void collectionOptionalEmptyFlagNotSetNotEmptyTest() {
    Set<String> set = new TreeSet<String>();
    set.add("a");
    assertDoesNotThrow(() -> MissingParam.checkMissing(set, "SET", false));
  }

  /**
   * Test the {@link Collection} array checker (with the empty option not set)
   * with a populated array.
   */
  @Test
  public void collectionOptionalEmptyFlagSetNotEmptyTest() {
    Set<String> set = new TreeSet<String>();
    set.add("a");
    assertDoesNotThrow(() -> MissingParam.checkMissing(set, "SET", true));
  }

  /**
   * Test the {@link Collection} array checker (with the empty option set) with
   * a {@code null} value.
   */
  @Test
  public void collectionOptionalEmptyFlagNotSetNullTest() {
    Set<String> set = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(set, "SET"));
  }

  /**
   * Test the {@link Collection} array checker (with the empty option not set)
   * with a {@code null} value.
   */
  @Test
  public void collectionOptionalEmptyFlagSetNullTest() {
    Set<String> set = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(set, "SET"));
  }

  /**
   * Test the {@link Collection} array checker (with the empty option set) with
   * an empty array.
   */
  @Test
  public void collectionOptionalEmptyFlagNotSetEmptyTest() {
    Set<String> set = new TreeSet<String>();
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(set, "SET"));
  }

  /**
   * Test the {@link Collection} array checker (with the empty option not set)
   * with an empty array.
   */
  @Test
  public void collectionOptionalEmptyFlagSetEmptyTest() {
    Set<String> set = new TreeSet<String>();
    assertDoesNotThrow(() -> MissingParam.checkMissing(set, "SET", true));
  }

  /**
   * Test the {@link Map} array checker (with no empty option) with a populated
   * array.
   */
  @Test
  public void mapNoEmptyFlagNotEmptyTest() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("a", "a");
    assertDoesNotThrow(() -> MissingParam.checkMissing(map, "MAP"));
  }

  /**
   * Test the {@link Map} array checker (with no empty option) with a
   * {@code null} value.
   */
  @Test
  public void mapNullTest() {
    Map<String, String> map = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(map, "MAP"));
  }

  /**
   * Test the {@link Map} array checker (with no empty option) with an empty
   * array.
   */
  @Test
  public void mapEmptyTest() {
    Map<String, String> map = new HashMap<String, String>();
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(map, "MAP"));
  }

  /**
   * Test the {@link Map} array checker (with the empty option set) with a
   * populated array.
   */
  @Test
  public void mapOptionalEmptyFlagNotSetNotEmptyTest() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("a", "a");
    assertDoesNotThrow(() -> MissingParam.checkMissing(map, "MAP", false));
  }

  /**
   * Test the {@link Map} array checker (with the empty option not set) with a
   * populated array.
   */
  @Test
  public void mapOptionalEmptyFlagSetNotEmptyTest() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("a", "a");
    assertDoesNotThrow(() -> MissingParam.checkMissing(map, "MAP", true));
  }

  /**
   * Test the {@link Map} array checker (with the empty option set) with a
   * {@code null} value.
   */
  @Test
  public void mapOptionalEmptyFlagNotSetNullTest() {
    Map<String, String> map = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(map, "MAP"));
  }

  /**
   * Test the {@link Map} array checker (with the empty option not set) with a
   * {@code null} value.
   */
  @Test
  public void mapOptionalEmptyFlagSetNullTest() {
    Map<String, String> map = null;
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(map, "MAP"));
  }

  /**
   * Test the {@link Map} array checker (with the empty option set) with an
   * empty array.
   */
  @Test
  public void mapOptionalEmptyFlagNotSetEmptyTest() {
    Map<String, String> map = new HashMap<String, String>();
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkMissing(map, "MAP"));
  }

  /**
   * Test the {@link Map} array checker (with the empty option not set) with an
   * empty array.
   */
  @Test
  public void mapOptionalEmptyFlagSetEmptyTest() {
    Map<String, String> map = new HashMap<String, String>();
    assertDoesNotThrow(() -> MissingParam.checkMissing(map, "MAP", true));
  }

  /**
   * Test the positive {@code int} checker with a negative value.
   */
  @Test
  public void intPositiveNegativeTest() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkPositive(-21, "INT"));
  }

  /**
   * Test the positive {@code int} checker with a zero value.
   */
  @Test
  public void intPositiveZeroTest() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkPositive(0, "INT"));
  }

  /**
   * Test the positive {@code int} checker with a positive value.
   */
  @Test
  public void intPositivePositiveTest() {
    assertDoesNotThrow(() -> MissingParam.checkPositive(21, "INT"));
  }

  /**
   * Test the positive {@code int} checker with a negative value.
   */
  @Test
  public void longPositiveNegativeTest() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkPositive(-21L, "LONG"));
  }

  /**
   * Test the positive {@code int} checker with a zero value.
   */
  @Test
  public void longPositiveZeroTest() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkPositive(0L, "LONG"));
  }

  /**
   * Test the positive {@code int} checker with a positive value.
   */
  @Test
  public void longPositivePositiveTest() {
    assertDoesNotThrow(() -> MissingParam.checkPositive(21L, "LONG"));
  }

  /**
   * Test the zero-positive {@code int} checker with a negative value.
   */
  @Test
  public void intZeroPositiveNegativeTest() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkZeroPositive(-21, "INT"));
  }

  /**
   * Test the zero-positive {@code int} checker with a zero value.
   */
  @Test
  public void intZeroPositiveZeroTest() {
    assertDoesNotThrow(() -> MissingParam.checkZeroPositive(0, "INT"));
  }

  /**
   * Test the zero-positive {@code int} checker with a positive value.
   */
  @Test
  public void intZeroPositivePositiveTest() {
    assertDoesNotThrow(() -> MissingParam.checkZeroPositive(21, "INT"));
  }

  /**
   * Test the zero-positive {@code int} checker with a negative value.
   */
  @Test
  public void doubleZeroPositiveNegativeTest() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkZeroPositive(-21D, "DOUBLE"));
  }

  /**
   * Test the zero-positive {@code int} checker with a zero value.
   */
  @Test
  public void doubleZeroPositiveZeroTest() {
    assertDoesNotThrow(() -> MissingParam.checkZeroPositive(0D, "DOUBLE"));
  }

  /**
   * Test the zero-positive {@code int} checker with a positive value.
   */
  @Test
  public void doubleZeroPositivePositiveTest() {
    assertDoesNotThrow(() -> MissingParam.checkZeroPositive(21D, "DOUBLE"));
  }

  /**
   * Test the database ID check (with NO RECORD option not set) with a negative
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordNotAllowedNegative() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkDatabaseId(-21L, "RECORD", false));
  }

  /**
   * Test the database ID check (with NO RECORD option not set) with a NO RECORD
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordNotAllowedNoRecord() {
    assertThrows(MissingParamException.class, () -> MissingParam
      .checkDatabaseId(DatabaseUtils.NO_DATABASE_RECORD, "RECORD", false));
  }

  /**
   * Test the database ID check (with NO RECORD option not set) with a zero
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordNotAllowedZero() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkDatabaseId(0L, "RECORD", false));
  }

  /**
   * Test the database ID check (with NO RECORD option not set) with a positive
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordNotAllowedPositive() {
    assertDoesNotThrow(
      () -> MissingParam.checkDatabaseId(21L, "RECORD", false));
  }

  /**
   * Test the database ID check (with NO RECORD option set) with a negative
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordAllowedNegative() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkDatabaseId(-21L, "RECORD", true));
  }

  /**
   * Test the database ID check (with NO RECORD option set) with a NO RECORD
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordAllowedNoRecord() {
    assertDoesNotThrow(() -> MissingParam
      .checkDatabaseId(DatabaseUtils.NO_DATABASE_RECORD, "RECORD", true));
  }

  /**
   * Test the database ID check (with NO RECORD option not set) with a zero
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordAllowedZero() {
    assertThrows(MissingParamException.class,
      () -> MissingParam.checkDatabaseId(0L, "RECORD", true));
  }

  /**
   * Test the database ID check (with NO RECORD option set) with a positive
   * value.
   */
  @Test
  public void databaseIdCheckNoRecordAllowedPositive() {
    assertDoesNotThrow(() -> MissingParam.checkDatabaseId(21L, "RECORD", true));
  }
}
