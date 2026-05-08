package uk.ac.exeter.QuinCe.User;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestLineException;
import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;

/**
 * Complex combination tests for the
 * {@link UserDB#checkEmailVerificationCode(javax.sql.DataSource, String, String)}
 * and
 * {@link UserDB#checkPasswordResetCode(javax.sql.DataSource, String, String)}
 * tests.
 *
 * <p>
 * Some of the tests for these methods are in the main {@link UserDBTest} class,
 * but some are more complex and required a Test Set.
 * </p>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class UserDBCodeCheckTest extends TestSetTest {

  /**
   * A column in the Test Set file for {@link #codeCheckTests(TestSetLine)}.
   */
  private static final int EMAIL_FIELD = 0;

  /**
   * A column in the Test Set file for {@link #codeCheckTests(TestSetLine)}.
   */
  private static final int EMAIL_CODE_FIELD = 1;

  /**
   * A column in the Test Set file for {@link #codeCheckTests(TestSetLine)}.
   */
  private static final int EMAIL_RESULT_FIELD = 2;

  /**
   * A column in the Test Set file for {@link #codeCheckTests(TestSetLine)}.
   */
  private static final int PASSWORD_CODE_FIELD = 3;

  /**
   * A column in the Test Set file for {@link #codeCheckTests(TestSetLine)}.
   */
  private static final int PASSWORD_RESULT_FIELD = 4;

  /**
   * Tests for code checks on users with various combinations of email and
   * password reset codes.
   *
   * @param line
   *          The line from the test file.
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/User/UserDBTest/codeCheckTests" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void codeCheckTests(TestSetLine line) throws Exception {

    User user = UserDB.getUser(getDataSource(), getEmail(line));
    assertEquals(getEmailCodeResult(line), UserDB.checkEmailVerificationCode(
      getDataSource(), getEmail(line), getEmailCode(user, line)));

    assertEquals(getPasswordCodeResult(line), UserDB.checkPasswordResetCode(
      getDataSource(), getEmail(line), getPasswordCode(user, line)));
  }

  /**
   * Get the email address from a {@link TestSetLine}
   *
   * @param line
   *          The line
   * @return The email address
   */
  private String getEmail(TestSetLine line) {
    return line.getStringField(EMAIL_FIELD, true);
  }

  /**
   * Get the email verification code to send from a {@link TestSetLine}.
   *
   * @param user
   *          The user being tested
   * @param line
   *          The test set line
   * @return The email verification code
   * @throws Exception
   *           If any internal errors are encountered.
   * @see #getCode(User, TestSetLine, int)
   */
  private String getEmailCode(User user, TestSetLine line) throws Exception {
    return getCode(user, line, EMAIL_CODE_FIELD);
  }

  /**
   * Get the expected email code check result from a {@link TestSetLine}
   *
   * @param line
   *          The line
   * @return The expected result
   */
  private int getEmailCodeResult(TestSetLine line) {
    return line.getIntField(EMAIL_RESULT_FIELD);
  }

  /**
   * Get the password reset code to send from a {@link TestSetLine}.
   *
   * @param user
   *          The user being tested
   * @param line
   *          The test set line
   * @return The password reset code
   * @throws Exception
   *           If any internal errors are encountered.
   * @see #getCode(User, TestSetLine, int)
   */
  private String getPasswordCode(User user, TestSetLine line) throws Exception {
    return getCode(user, line, PASSWORD_CODE_FIELD);
  }

  /**
   * Get the expected password code check result from a {@link TestSetLine}
   *
   * @param line
   *          The line
   * @return The expected result
   */
  private int getPasswordCodeResult(TestSetLine line) {
    return line.getIntField(PASSWORD_RESULT_FIELD);
  }

  /**
   * Get the code to send from a field in a {@link TestSetLine}.
   *
   * <p>
   * The field will contain one of the following values:
   * </p>
   * <table>
   * <caption>Possible values for the code field in the test set.</caption>
   * <tr>
   * <td><b>Value</b></td>
   * <td><b>Code sent</b></td>
   * </tr>
   * <tr>
   * <td>{@code NULL}</td>
   * <td>{@code null}</td>
   * </tr>
   * <tr>
   * <td>{@code RANDOM}</td>
   * <td>A random string</td>
   * </tr>
   * <tr>
   * <td>{@code USER}</td>
   * <td>The code stored in the supplied {@link User} object</td>
   * </tr>
   * </table>
   *
   * <p>
   * Note that tests for empty strings are in separate tests.
   * </p>
   *
   * @param user
   *          The user object under test.
   * @param line
   *          The test set line.
   * @param field
   *          The zero-based field number.
   * @return The code to pass in the test.
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private String getCode(User user, TestSetLine line, int field)
    throws Exception {

    String result = null;

    switch (line.getStringField(field, false)) {
    case "NULL": {
      result = null;
      break;
    }
    case "RANDOM": {
      result = "RANDOMCODESTRING";
      break;
    }
    case "USER": {
      if (field == EMAIL_CODE_FIELD) {
        result = user.getEmailVerificationCode();
      } else if (field == PASSWORD_CODE_FIELD) {
        result = user.getPasswordResetCode();
      } else {
        throw new TestLineException(line,
          "Invalid field " + field + "for Code To Send lookup");
      }
      break;
    }
    default: {
      throw new TestLineException(line, "Unrecognised code specifier '"
        + line.getStringField(field, false) + "'");
    }
    }

    return result;
  }

  @Override
  protected String getTestSetName() {
    return "userCodeCheckTests";
  }
}
