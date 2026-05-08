package uk.ac.exeter.QuinCe.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Utility methods for checking method parameters.
 */
public class MissingParam {

  /**
   * Check that a character array is not {@code null} or empty.
   *
   * @param parameter
   *          The array.
   * @param parameterName
   *          The parameter name.
   * @throws MissingParamException
   *           If the array is {@code null} or empty.
   */
  public static void checkMissing(char[] parameter, String parameterName)
    throws MissingParamException {
    checkMissing(parameter, parameterName, false);
  }

  /**
   * Check that a database connection is not null and not closed.
   *
   * @param conn
   *          The database connection.
   * @param parameterName
   *          The parameter name.
   * @throws MissingParamException
   *           If the connection is {@code null} or closed.
   */
  public static void checkMissing(Connection conn, String parameterName)
    throws MissingParamException {
    if (null == conn) {
      throw new MissingParamException(parameterName);
    } else {
      try {
        if (conn.isClosed()) {
          throw new MissingParamException(parameterName,
            "Database connection is closed");
        }
      } catch (SQLException e) {
        throw new MissingParamException(parameterName,
          "Error while checking database connection status");
      }
    }
  }

  /**
   * Ensure that a parameter value is not {@code null}.
   *
   * @param parameter
   *          The parameter value.
   * @param parameterName
   *          The parameter name.
   * @throws MissingParamException
   *           If the parameter is {@code null}.
   */
  public static void checkMissing(Object parameter, String parameterName)
    throws MissingParamException {
    if (null == parameter) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Ensure that a {@link Collection} is not {@code null} and not empty.
   *
   * @param collection
   *          The {@link Collection}.
   * @param collectionName
   *          The {@link Collection} name.
   * @throws MissingParamException
   *           If the {@link Collection} is {@code null} or empty.
   * @see Collection#isEmpty()
   */
  public static void checkMissing(Collection<?> collection,
    String collectionName) throws MissingParamException {

    checkMissing(collection, collectionName, false);
  }

  /**
   * Ensure that a {@link Collection} is not {@code null}. If {@code canBeEmpty}
   * is {@code false}, the {@link Collection} cannot be empty.
   *
   * @param collection
   *          The {@link Collection}.
   * @param collectionName
   *          The {@link Collection} name.
   * @param canBeEmpty
   *          Indicates whether the {@link Collection} is allowed to be empty.
   * @throws MissingParamException
   *           If the {@link Collection} is {@code null} or (if applicable)
   *           empty.
   * @see Collection#isEmpty()
   */
  public static void checkMissing(Collection<?> collection,
    String collectionName, boolean canBeEmpty) throws MissingParamException {

    if (null == collection) {
      throw new MissingParamException(collectionName);
    } else if (!canBeEmpty && collection.size() == 0) {
      throw new EmptyMissingParamException(collectionName);
    }
  }

  /**
   * Ensure that a {@link Map} is not null and not empty
   *
   * @param map
   *          The {@link Map}.
   * @param mapName
   *          The {@link Map} name.
   * @throws MissingParamException
   *           If the {@link Map} is {@code null} or empty.
   * @see Map#isEmpty()
   */
  public static void checkMissing(Map<?, ?> map, String mapName)
    throws MissingParamException {

    checkMissing(map, mapName, false);
  }

  /**
   * Ensure that a {@link Map} is not {@code null}. If {@code canBeEmpty} is
   * {@code false}, the {@link Map} cannot be empty.
   *
   * @param map
   *          The {@link Map}
   * @param mapName
   *          The {@link Map} name.
   * @param canBeEmpty
   *          Indicates whether the {@link Map} is allowed to be empty.
   * @throws MissingParamException
   *           If the {@link Map} is {@code null} or (if applicable) empty.
   * @see Map#isEmpty()
   */
  public static void checkMissing(Map<?, ?> map, String mapName,
    boolean canBeEmpty) throws MissingParamException {

    if (null == map) {
      throw new MissingParamException(mapName);
    } else if (!canBeEmpty && map.size() == 0) {
      throw new EmptyMissingParamException(mapName + " must not be empty");
    }
  }

  /**
   * Check that a character array is not {@code null}. If {@code canBeEmpty} is
   * {@code false}, the array cannot be empty.
   *
   * @param parameter
   *          The array.
   * @param parameterName
   *          The array name.
   * @param canBeEmpty
   *          Indicates whether or not the array can be empty.
   * @throws MissingParamException
   *           If the {@link array} is {@code null} or (if applicable) empty.
   */
  public static void checkMissing(char[] parameter, String parameterName,
    boolean canBeEmpty) throws MissingParamException {
    if (null == parameter) {
      throw new MissingParamException(parameterName);
    } else if (!canBeEmpty && parameter.length == 0) {
      throw new EmptyMissingParamException(parameterName);
    }
  }

  /**
   * Ensure that a {@link String} is not null, or its trimmed value is not
   * empty.
   *
   * @param string
   * @param stringName
   * @throws MissingParamException
   */
  public static void checkMissing(String string, String stringName)
    throws MissingParamException {

    checkMissing(string, stringName, false);
  }

  /**
   * Ensure that a {@link String} is not {@code null}. If {@code canBeEmpty} is
   * {@code false}, the {@link String} cannot be empty (after trimming).
   *
   * @param collection
   *          The {@link String}.
   * @param collectionName
   *          The {@link String} name.
   * @throws MissingParamException
   *           If the {@link String} is {@code null} or (if applicable) empty.
   * @see String#trim()
   */
  public static void checkMissing(String string, String stringName,
    boolean canBeEmpty) throws MissingParamException {

    if (null == string) {
      throw new MissingParamException(stringName);
    } else if (!canBeEmpty && string.trim().length() == 0) {
      throw new EmptyMissingParamException(stringName);
    }
  }

  /**
   * Check that an integer value is positive.
   *
   * @param parameter
   *          The value.
   * @param parameterName
   *          The parameter name.
   * @throws MissingParamException
   *           If the value is not positive.
   */
  public static void checkPositive(int parameter, String parameterName)
    throws MissingParamException {
    if (parameter <= 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that a long value is positive.
   *
   * @param parameter
   *          The value.
   * @param parameterName
   *          The parameter name.
   * @throws MissingParamException
   *           If the value is not positive.
   */
  public static void checkPositive(long parameter, String parameterName)
    throws MissingParamException {
    if (parameter <= 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that an integer value is zero or positive.
   *
   * @param parameter
   *          The value.
   * @param parameterName
   *          The parameter name.
   * @throws MissingParamException
   *           If the value is not zero or positive.
   */
  public static void checkZeroPositive(int parameter, String parameterName)
    throws MissingParamException {
    if (parameter < 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that a double value is zero or positive.
   *
   * @param parameter
   *          The value.
   * @param parameterName
   *          The parameter name.
   * @throws MissingParamException
   *           If the value is not zero or positive.
   */
  public static void checkZeroPositive(double parameter, String parameterName)
    throws MissingParamException {
    if (parameter < 0) {
      throw new MissingParamException(parameterName);
    }
  }

  /**
   * Check that an array of {code int} values is not {@code null}. If
   * {@code canBeEmpty} is {@code false}, the array cannot be empty.
   *
   * @param parameter
   *          The array.
   * @param parameterName
   *          The array name.
   * @param canBeEmpty
   *          Indicates whether or not the array can be empty.
   * @throws MissingParamException
   *           If the {@link array} is {@code null} or (if applicable) empty.
   */
  public static void checkMissing(int[] parameter, String parameterName,
    boolean canBeEmpty) throws MissingParamException {

    if (null == parameter) {
      throw new MissingParamException(parameterName);
    } else if (!canBeEmpty && parameter.length == 0) {
      throw new EmptyMissingParamException(parameterName);
    }
  }

  /**
   * Check whether a value is a valid database ID. Optionally allow the
   * {@link DatabaseUtils#NO_DATABASE_RECORD} value.
   *
   * @param parameter
   *          The parameter value.
   * @param parameterName
   *          The parameter name.
   * @param allowNoRecord
   *          Indicates whether the {@link DatabaseUtils#NO_DATABASE_RECORD}
   *          value is allowed.
   */
  public static void checkDatabaseId(long parameter, String parameterName,
    boolean allowNoRecord) {

    boolean ok = parameter > 0;
    if (!ok && allowNoRecord && parameter == DatabaseUtils.NO_DATABASE_RECORD) {
      ok = true;
    }

    if (!ok) {
      throw new MissingParamException(
        parameterName + " is not a valid database ID");
    }
  }
}
