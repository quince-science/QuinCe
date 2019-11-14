package junit.uk.ac.exeter.QuinCe.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.User.UserExistsException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Tests for the {@link UserDB} class.
 *
 * <p>
 * These tests exclusively use {@link User} objects created in this class. They
 * <b>do not</b> make use of the dummy user defined in
 * {@code WebApp/junit/resources/sql/testbase/user}.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class UserDBTest extends BaseTest {

  /**
   * Create a user in the database.
   *
   * <p>
   * The user has the following properties:
   * <p>
   * <table>
   * <tr>
   * <td>Email:</td>
   * <td>test@test.com</td>
   * </tr>
   * <tr>
   * <td>Password:</td>
   * <td>test</td>
   * </tr>
   * <tr>
   * <td>Given Name:</td>
   * <td>Testy</td>
   * </tr>
   * <tr>
   * <td>Surname:</td>
   * <td>McTestFace</td>
   * </tr>
   * </table>
   *
   * <p>
   * The user has default permissions ({@code 0}), and an email verification
   * code is not generated.
   * </p>
   *
   * @param emailCode
   *          Indicates whether or not the email verification code should be set
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  private void createUser(boolean emailCode)
    throws MissingParamException, UserExistsException, DatabaseException {
    UserDB.createUser(getDataSource(), "test@test.com", "test".toCharArray(),
      "Testy", "McTestFace", emailCode);
  }

  /**
   * Test that an exception is thrown when authenticating user without providing
   * an email address
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the test user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void authenticateMissingEmailTest(String email)
    throws MissingParamException, UserExistsException, DatabaseException {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED,
      UserDB.authenticate(getDataSource(), email, "test".toCharArray()));
  }

  /**
   * Test that an exception is thrown when authenticating user without providing
   * a password
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the test user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void authenticateMissingPasswordTest(String password)
    throws MissingParamException, UserExistsException, DatabaseException {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED,
      UserDB.authenticate(getDataSource(), "test@test.com",
        null == password ? null : password.toCharArray()));
  }

  /**
   * Test that an exception is thrown when authenticating user without providing
   * a {@link DataSource}.
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the test user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  @FlywayTest
  @Test
  public void authenticateMissingDataSourceTest()
    throws MissingParamException, UserExistsException, DatabaseException {
    createUser(false);
    assertThrows(MissingParamException.class, () -> {
      UserDB.authenticate(null, "test@test.com", "test".toCharArray());
    });
  }

  /**
   * Test that a user can be authenticated with the correct email and password.
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the test user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  @FlywayTest
  @Test
  public void authenticateSuccessfulTest()
    throws MissingParamException, UserExistsException, DatabaseException {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(),
      "test@test.com", "test".toCharArray()));
  }

  /**
   * Test that authentication fails for an email address that isn't in the
   * database.
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the test user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  @FlywayTest
  @Test
  public void authenticateBadEmailTest()
    throws MissingParamException, UserExistsException, DatabaseException {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB
      .authenticate(getDataSource(), "flib@flibble.com", "test".toCharArray()));
  }

  /**
   * Test that authentication fails when an incorrect password is supplied.
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the test user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  @FlywayTest
  @Test
  public void authenticateBadPasswordTest()
    throws MissingParamException, UserExistsException, DatabaseException {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB
      .authenticate(getDataSource(), "test@test.com", "flibble".toCharArray()));
  }

  /**
   * Test that a user can be authenticated with the correct email and password.
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws UserExistsException
   *           If the test user has already been created
   * @throws MissingParamException
   *           If the method fails to pass required information to the back end.
   */
  @FlywayTest
  @Test
  public void authenticateVerificationCodeSetTest()
    throws MissingParamException, UserExistsException, DatabaseException {
    createUser(true);
    assertEquals(UserDB.AUTHENTICATE_EMAIL_CODE_SET, UserDB
      .authenticate(getDataSource(), "test@test.com", "test".toCharArray()));
  }
}
