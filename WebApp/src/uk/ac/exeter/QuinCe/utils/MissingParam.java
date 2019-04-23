package uk.ac.exeter.QuinCe.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Utility methods for checking method parameters
 * @author Steve Jones
 *
 */
public class MissingParam {

  /**
   * Check that a character array is not {@code null} or empty.
   * @param parameter The character array
   * @param parameterName The parameter name
   * @throws MissingParamException If the array {@code null} or empty
   */
  public static void checkMissing(char[] parameter, String parameterName) throws MissingParamException {
    checkMissing(parameter, parameterName, false);
  }

  /**
   * Check that a database connection is not null and not closed
   * @param conn The database connection
   * @param parameterName The parameter name
   * @throws MissingParamException If the connection is null or closed
   */
  public static void checkMissing(Connection conn, String parameterName) throws MissingParamException {
    if (null == conn) {
      throw new MissingParamException(parameterName);
    } else {
      try {
        if (conn.isClosed()) {
          throw new MissingParamException(parameterName, "Database connection is closed");
        }
      } catch (SQLException e) {
        throw new MissingParamException(parameterName, "Error while checking database connection status");
      }
    }
  }

  /**
   * Ensure that a parameter value is not {@code null}
   * @param parameter The parameter value to be checked
   * @param parameterName The parameter name
   * @param canBeEmpty Indicates whether Strings and Collections can be empty
   * @throws MissingParamException If the parameter is {@code null} or empty (if {@code canBeEmpty} is {@code false})
   */
  public static void checkMissing(Object parameter, String parameterName) throws MissingParamException {
    if (null == parameter) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Ensure that a Collection is not null and not empty
   * @param collection The Collection
   * @param collectionName The Collection name
   * @throws MissingParamException If the Collection is missing
   */
  public static void checkMissing(Collection<?> collection, String collectionName)
    throws MissingParamException {

    checkMissing(collection, collectionName, false);
  }

  /**
   * Ensure that a Collection is not {@code null}. If {@code canBeEmpty} is
   * {@code false}, the Collection cannot be empty.
   *
   * @param collection The Collection
   * @param collectionName The Collection name
   * @throws MissingParamException If the Collection is missing
   */
 public static void checkMissing(Collection<?> collection, String collectionName,
    boolean canBeEmpty) throws MissingParamException {

    if (null == collection || (!canBeEmpty && collection.size() == 0)) {
      throw new MissingParamException(collectionName);
    }
  }

 /**
  * Ensure that a Map is not null and not empty
  * @param map The Map
  * @param mapName The Map name
  * @throws MissingParamException If the Map is missing
  */
 public static void checkMissing(Map<?, ?> map, String mapName)
   throws MissingParamException {

   checkMissing(map, mapName, false);
 }

 /**
  * Ensure that a Map is not {@code null}. If {@code canBeEmpty} is
  * {@code false}, the Map cannot be empty.
  *
  * @param map The Map
  * @param mapName The Map name
  * @throws MissingParamException If the Map is missing
  */
public static void checkMissing(Map<?, ?> map, String mapName,
   boolean canBeEmpty) throws MissingParamException {

   if (null == map || (!canBeEmpty && map.size() == 0)) {
     throw new MissingParamException(mapName);
   }
 }

  /**
   * Check that a character array is not {@code null}. It can be empty if
   * {@code canBeEmpty} is set to {@code true}.
   * @param parameter The character array
   * @param parameterName The parameter name
   * @param canBeEmpty Indicates whether the array can be empty
   * @throws MissingParamException If the array is {@code null} or empty (if {@code canBeEmpty} is {@code false})
   */
  public static void checkMissing(char[] parameter, String parameterName, boolean canBeEmpty) throws MissingParamException {
    boolean isMissing = false;

    if (null == parameter) {
      isMissing = true;
    } else {
      if (!canBeEmpty && parameter.length == 0) {
          isMissing = true;
      }
    }

    if (isMissing) {
      throw new MissingParamException(parameterName);
    }
  }

  public static void checkMissing(String string, String stringName)
    throws MissingParamException {

    checkMissing(string, stringName, false);
  }

  /**
   * Ensure that a String is not {@code null}. If {@code canBeEmpty} is
   * {@code false}, the String cannot be empty (after trimming).
   *
   * @param collection The String
   * @param collectionName The String name
   * @throws MissingParamException If the String is missing
   */
  public static void checkMissing(String string, String stringName,
    boolean canBeEmpty) throws MissingParamException {

    if (null == string || (!canBeEmpty && string.trim().length() == 0)) {
      throw new MissingParamException(stringName);
    }
  }

  /**
   * Check that an integer value is positive
   * @param parameter The value
   * @param parameterName The parameter name
   * @throws MissingParamException If the value is not positive
   */
  public static void checkPositive(int parameter, String parameterName) throws MissingParamException {
    if (parameter <= 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that a long value is positive
   * @param parameter The value
   * @param parameterName The parameter name
   * @throws MissingParamException If the value is not positive
   */
  public static void checkPositive(long parameter, String parameterName) throws MissingParamException {
    if (parameter <= 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that an integer value is zero or positive
   * @param parameter The value
   * @param parameterName The parameter name
   * @throws MissingParamException If the value is not zero or positive
   */
  public static void checkZeroPositive(int parameter, String parameterName) throws MissingParamException {
    if (parameter < 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that a double value is zero or positive
   * @param parameter The value
   * @param parameterName The parameter name
   * @throws MissingParamException If the value is not zero or positive
   */
  public static void checkZeroPositive(double parameter, String parameterName) throws MissingParamException {
    if (parameter < 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that a String value contains a comma-separated list of integers
   * @param list The String value
   * @param parameterName The parameter name
   * @throws ParameterException If the String format is invalid
   */
  public static void checkListOfIntegers(String list, String parameterName) throws ParameterException {

    checkMissing(list, parameterName);

    boolean ok = true;

    try {
      String[] entries = list.split(",");
      for (String entry : entries) {
        Integer.parseInt(entry);
      }
    } catch (NumberFormatException e) {
      ok = false;
    }

    if (!ok) {
      throw new ParameterException(parameterName, "is not a list of integers");
    }
  }

  /**
   * Check that a Long value is either {@code null} or positive
   * @param parameter The parameter
   * @param parameterName The parameter name
   * @throws MissingParamException If the parameter is not null and not positive
   */
  public static void checkNullPositive(Long parameter, String parameterName) throws MissingParamException {
    if (null != parameter) {
      checkPositive(parameter, parameterName);
    }
  }
}
