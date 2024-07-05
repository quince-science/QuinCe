package uk.ac.exeter.QuinCe.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Tests for the {@link UserDB} class.
 *
 * <p>
 * These tests exclusively use {@link User} objects created in this class. They
 * <b>do not</b> make use of the dummy user defined in
 * {@code WebApp/junit/resources/sql/testbase/user}.
 * </p>
 */
public class UserDBTest extends BaseTest {

  /**
   * The email address of the test user created for these tests.
   *
   * @see #createUser(boolean)
   */
  private static final String TEST_USER_EMAIL = "test@test.com";

  /**
   * The password for the test user created for these tests.
   *
   * @see #createUser(boolean)
   */
  private static final char[] TEST_USER_PASSWORD = "test".toCharArray();

  /**
   * Create a user in the database.
   *
   * <p>
   * The user has the following properties:
   * </p>
   * <table>
   * <caption>User properties</caption>
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
   * @return The {@link User} object for the created user.
   * @throws Exception
   *           If any internal errors are encountered.
   */
  private User createUser(boolean emailCode) throws Exception {
    return UserDB.createUser(getDataSource(), TEST_USER_EMAIL,
      TEST_USER_PASSWORD, "Testy", "McTestFace", emailCode);
  }

  /**
   * Test that an exception is thrown when authenticating user without providing
   * an email address
   *
   * @param email
   *          The empty email address (generated)
   * @throws Exception
   *           If any internal errors are encountered.
   *
   * @see #createNullEmptyStrings()
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void authenticateMissingEmailTest(String email) throws Exception {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED,
      UserDB.authenticate(getDataSource(), email, TEST_USER_PASSWORD));
  }

  /**
   * Test that an exception is thrown when authenticating user without providing
   * a password.
   *
   * @param password
   *          The empty password (generated).
   *
   * @throws Exception
   *           If any internal errors are encountered.
   *
   * @see #createNullEmptyStrings()
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void authenticateMissingPasswordTest(String password)
    throws Exception {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED,
      UserDB.authenticate(getDataSource(), TEST_USER_EMAIL,
        null == password ? null : password.toCharArray()));
  }

  /**
   * Test that an exception is thrown when authenticating user without providing
   * a {@link javax.sql.DataSource}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void authenticateMissingDataSourceTest() throws Exception {
    createUser(false);
    assertThrows(MissingParamException.class, () -> {
      UserDB.authenticate(null, TEST_USER_EMAIL, TEST_USER_PASSWORD);
    });
  }

  /**
   * Test that a user can be authenticated with the correct email and password.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void authenticateSuccessfulTest() throws Exception {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(),
      TEST_USER_EMAIL, TEST_USER_PASSWORD));
  }

  /**
   * Test that authentication fails for an email address that isn't in the
   * database.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void authenticateBadEmailTest() throws Exception {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB
      .authenticate(getDataSource(), "flib@flibble.com", TEST_USER_PASSWORD));
  }

  /**
   * Test that authentication fails when an incorrect password is supplied.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void authenticateBadPasswordTest() throws Exception {
    createUser(false);
    assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB
      .authenticate(getDataSource(), TEST_USER_EMAIL, "flibble".toCharArray()));
  }

  /**
   * Test that a user can be authenticated with the correct email and password.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void authenticateVerificationCodeSetTest() throws Exception {
    createUser(true);
    assertEquals(UserDB.AUTHENTICATE_EMAIL_CODE_SET, UserDB
      .authenticate(getDataSource(), TEST_USER_EMAIL, TEST_USER_PASSWORD));
  }

  /**
   * Test that the password reset code is removed if the user authenticates with
   * their existing password.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void authenticateWithPasswordCodeTest() throws Exception {
    User user = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), user);

    assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(),
      TEST_USER_EMAIL, TEST_USER_PASSWORD));

    User postAuthenticationUser = UserDB.getUser(getDataSource(),
      user.getDatabaseID());
    assertNull(postAuthenticationUser.getPasswordResetCode());
    assertNull(postAuthenticationUser.getPasswordResetCodeTime());

  }

  /**
   * Test that retrieving a user via a {@link javax.sql.DataSource} with an
   * email address throws an throws an exception when the address is missing or
   * empty.
   *
   * <p>
   * This does not test valid {@link String}s that are not in the database.
   * </p>
   *
   * @param email
   *          The email address (auto-generated).
   *
   * @see #createNullEmptyStrings()
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void getUserDataSourceEmptyEmailTest(String email) {
    assertThrows(MissingParamException.class, () -> {
      UserDB.getUser(getDataSource(), email);
    });
  }

  /**
   * Test that retrieving a user via a {@link java.sql.Connection} with an email
   * address throws an throws an exception when the address is missing or empty.
   *
   * <p>
   * This does not test valid {@link String}s that are not in the database.
   * </p>
   *
   * @param email
   *          The email address (auto-generated).
   *
   * @see #createNullEmptyStrings()
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void getUserConnectionEmptyEmailTest(String email) {
    assertThrows(MissingParamException.class, () -> {
      UserDB.getUser(getDataSource().getConnection(), email);
    });
  }

  /**
   * Test that retrieving a user via a {@link javax.sql.DataSource} with an ID
   * throws an exception when the ID is invalid.
   *
   * <p>
   * This does not test valid IDs that do not exist in the database.
   * </p>
   *
   * @param id
   *          The database ID (auto-generated).
   *
   * @see #createInvalidReferences()
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void getUserDataSourceInvalidIDTest(long id) {
    assertThrows(MissingParamException.class, () -> {
      UserDB.getUser(getDataSource(), id);
    });
  }

  /**
   * Test that retrieving a user via a {@link java.sql.Connection} with an ID
   * throws an exception when the ID is invalid.
   *
   * <p>
   * This does not test valid IDs that do not exist in the database.
   * </p>
   *
   * @param id
   *          The database ID (auto-generated).
   *
   * @see #createInvalidReferences()
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void getUserConnectionInvalidIDTest(long id) {
    assertThrows(MissingParamException.class, () -> {
      UserDB.getUser(getDataSource().getConnection(), id);
    });
  }

  /**
   * Test that retrieving a user via a {@link javax.sql.DataSource} and email
   * address that doesn't exist returns {@code null}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getNonExistentUserEmailDataSourceTest() throws Exception {
    createUser(false);
    assertNull(UserDB.getUser(getDataSource(), "flib@flibble.com"));
  }

  /**
   * Test that retrieving a user via a {@link java.sql.Connection} and email
   * address that doesn't exist returns {@code null}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getNonExistentUserEmailConnectionTest() throws Exception {
    createUser(false);
    assertNull(
      UserDB.getUser(getDataSource().getConnection(), "flib@flibble.com"));
  }

  /**
   * Test that retrieving a user via a {@link javax.sql.DataSource} and ID that
   * doesn't exist returns {@code null}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getNonExistentUserIDDataSourceTest() throws Exception {
    createUser(false);
    assertNull(UserDB.getUser(getDataSource(), 1000L));
  }

  /**
   * Test that retrieving a user via a {@link java.sql.Connection} and ID that
   * doesn't exist returns {@code null}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getNonExistentUserIDConnectionTest() throws Exception {
    createUser(false);
    assertNull(UserDB.getUser(getDataSource().getConnection(), 1000L));
  }

  /**
   * Test that a user with no email verification or password reset code can be
   * correctly retrieved from the database via a {@link javax.sql.DataSource}
   * and email address.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getBasicUserDataSourceEmailTest() throws Exception {

    createUser(false);
    User user = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    testBaseUserDetails(user);
    testEmailVerificationCode(user, false);
    testPasswordResetCode(user, false);
  }

  /**
   * Test that a user with no email verification or password reset code can be
   * correctly retrieved from the database via a {@link java.sql.Connection} and
   * email address.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getBasicUserConnectionEmailTest() throws Exception {

    createUser(false);
    User user = UserDB.getUser(getDataSource().getConnection(),
      TEST_USER_EMAIL);
    testBaseUserDetails(user);
    testEmailVerificationCode(user, false);
    testPasswordResetCode(user, false);
  }

  /**
   * Test that a user with no email verification or password reset code can be
   * correctly retrieved from the database via a {@link javax.sql.DataSource}
   * and email address.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getBasicUserDataSourceIDTest() throws Exception {

    User testUser = createUser(false);
    User user = UserDB.getUser(getDataSource(), testUser.getDatabaseID());
    testBaseUserDetails(user);
    testEmailVerificationCode(user, false);
    testPasswordResetCode(user, false);
  }

  /**
   * Test that a user with no email verification or password reset code can be
   * correctly retrieved from the database via a {@link java.sql.Connection} and
   * email address.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getBasicUserConnectionIDTest() throws Exception {

    User testUser = createUser(false);
    User user = UserDB.getUser(getDataSource().getConnection(),
      testUser.getDatabaseID());
    testBaseUserDetails(user);
    testEmailVerificationCode(user, false);
    testPasswordResetCode(user, false);
  }

  /**
   * Test that a user created with
   * {@link UserDB#createUser(javax.sql.DataSource, String, char[], String, String, boolean)}
   * and the email verification code set has the email code set in the returned
   * object.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithVerificationCodeTest() throws Exception {
    User user = createUser(true);
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that a user created with
   * {@link UserDB#createUser(javax.sql.DataSource, String, char[], String, String, boolean)}
   * and the email verification not code set has no code set in the returned
   * object.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithoutVerificationCodeTest() throws Exception {
    User user = createUser(false);
    testEmailVerificationCode(user, false);
  }

  /**
   * Test that a user created with a verification code has that code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(javax.sql.DataSource, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithVerificationCodeRetrieveByDataSourceEmailTest()
    throws Exception {

    createUser(true);
    User user = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that a user created with a verification code has that code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(javax.sql.DataSource, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithVerificationCodeRetrieveByDataSourceIDTest()
    throws Exception {

    User testUser = createUser(true);
    User user = UserDB.getUser(getDataSource(), testUser.getDatabaseID());
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that a user created with a verification code has that code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(java.sql.Connection, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithVerificationCodeRetrieveByConnectionEmailTest()
    throws Exception {

    createUser(true);
    User user = UserDB.getUser(getDataSource().getConnection(),
      TEST_USER_EMAIL);
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that a user created with a verification code has that code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(javax.sql.DataSource, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithVerificationCodeRetrieveByConnectionIDTest()
    throws Exception {

    User testUser = createUser(true);
    User user = UserDB.getUser(getDataSource().getConnection(),
      testUser.getDatabaseID());
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that a user created without a verification code has no code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(javax.sql.DataSource, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithoutVerificationCodeRetrieveByDataSourceEmailTest()
    throws Exception {

    createUser(false);
    User user = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    testEmailVerificationCode(user, false);
  }

  /**
   * Test that a user created without a verification code has no code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(javax.sql.DataSource, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithoutVerificationCodeRetrieveByDataSourceIDTest()
    throws Exception {

    User testUser = createUser(false);
    User user = UserDB.getUser(getDataSource(), testUser.getDatabaseID());
    testEmailVerificationCode(user, false);
  }

  /**
   * Test that a user created without a verification code has no code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(java.sql.Connection, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithoutVerificationCodeRetrieveByConnectionEmailTest()
    throws Exception {

    createUser(false);
    User user = UserDB.getUser(getDataSource().getConnection(),
      TEST_USER_EMAIL);
    testEmailVerificationCode(user, false);
  }

  /**
   * Test that a user created with a verification code has that code in the
   * {@link User} object returned by
   * {@link UserDB#getUser(java.sql.Connection, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void createWithoutVerificationCodeRetrieveByConnectionIDTest()
    throws Exception {

    User testUser = createUser(false);
    User user = UserDB.getUser(getDataSource().getConnection(),
      testUser.getDatabaseID());
    testEmailVerificationCode(user, false);
  }

  /**
   * Test that setting a password reset code updates the {@link User} object.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addPasswordCodeCheckObjectTest() throws Exception {
    User testUser = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), testUser);
    testPasswordResetCode(testUser, true);
  }

  /**
   * Test that adding a password reset code to a user updates the database and
   * can be retrieved with {@link UserDB#getUser(javax.sql.DataSource, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addPasswordCodeRetrieveByDataSourceEmailTest() throws Exception {

    User testUser = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    testPasswordResetCode(user, true);
  }

  /**
   * Test that adding a password reset code to a user updates the database and
   * can be retrieved with {@link UserDB#getUser(javax.sql.DataSource, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addPasswordCodeRetrieveByDataSourceIDTest() throws Exception {

    User testUser = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource(), testUser.getDatabaseID());
    testPasswordResetCode(user, true);
  }

  /**
   * Test that adding a password reset code to a user updates the database and
   * can be retrieved with {@link UserDB#getUser(java.sql.Connection, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addPasswordCodeRetrieveByConnectionEmailTest() throws Exception {

    User testUser = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource().getConnection(),
      TEST_USER_EMAIL);
    testPasswordResetCode(user, true);
  }

  /**
   * Test that adding a password reset code to a user updates the database and
   * can be retrieved with {@link UserDB#getUser(java.sql.Connection, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addPasswordCodeRetrieveByConnectionIDTest() throws Exception {

    User testUser = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource().getConnection(),
      testUser.getDatabaseID());
    testPasswordResetCode(user, true);
  }

  /**
   * Test that setting an email verification code updates the {@link User}
   * object.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addVerificationCodeCheckObjectTest() throws Exception {
    User testUser = createUser(false);
    UserDB.generateEmailVerificationCode(getDataSource(), testUser);
    testEmailVerificationCode(testUser, true);
  }

  /**
   * Test that adding a verification code to a user updates the database and can
   * be retrieved with {@link UserDB#getUser(javax.sql.DataSource, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addVerificationCodeRetrieveByDataSourceEmailTest()
    throws Exception {

    User testUser = createUser(false);
    UserDB.generateEmailVerificationCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that adding a verification code to a user updates the database and can
   * be retrieved with {@link UserDB#getUser(javax.sql.DataSource, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addVerificationCodeRetrieveByDataSourceIDTest() throws Exception {

    User testUser = createUser(false);
    UserDB.generateEmailVerificationCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource(), testUser.getDatabaseID());
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that adding a verification code to a user updates the database and can
   * be retrieved with {@link UserDB#getUser(java.sql.Connection, String)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addVerificationCodeRetrieveByConnectionEmailTest()
    throws Exception {

    User testUser = createUser(false);
    UserDB.generateEmailVerificationCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource().getConnection(),
      TEST_USER_EMAIL);
    testEmailVerificationCode(user, true);
  }

  /**
   * Test that adding a verification code to a user updates the database and can
   * be retrieved with {@link UserDB#getUser(java.sql.Connection, long)}.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void addVerificationCodeRetrieveByConnectionIDTest() throws Exception {

    User testUser = createUser(false);
    UserDB.generateEmailVerificationCode(getDataSource(), testUser);

    User user = UserDB.getUser(getDataSource().getConnection(),
      testUser.getDatabaseID());
    testEmailVerificationCode(user, true);
  }

  /**
   * Check that a {@link User} object contains the correct email address and
   * name for the test user.
   *
   * @param user
   *          The {@link User} object to be tested
   *
   * @see #createUser(boolean)
   */
  private void testBaseUserDetails(User user) {
    assertTrue(user.getEmailAddress().equals(TEST_USER_EMAIL));
    assertTrue(user.getGivenName().equals("Testy"));
    assertTrue(user.getSurname().equals("McTestFace"));
  }

  /**
   * Check that the email verification code in a {@link User} object is set or
   * not set, according to the passed in parameter.
   *
   * @param user
   *          The {@link User} object to be tested
   * @param set
   *          Indicates whether or not the code should be set.
   */
  private void testEmailVerificationCode(User user, boolean set) {
    if (set) {
      assertNotNull(user.getEmailVerificationCode());
      assertNotNull(user.getEmailVerificationCodeTime());
    } else {
      assertNull(user.getEmailVerificationCode());
      assertNull(user.getEmailVerificationCodeTime());
    }
  }

  /**
   * Check that the password reset code in a {@link User} object is set or not
   * set, according to the passed in parameter.
   *
   * @param user
   *          The {@link User} object to be tested
   * @param set
   *          Indicates whether or not the code should be set.
   */
  private void testPasswordResetCode(User user, boolean set) {
    if (set) {
      assertNotNull(user.getPasswordResetCode());
      assertNotNull(user.getPasswordResetCodeTime());
    } else {
      assertNull(user.getPasswordResetCode());
      assertNull(user.getPasswordResetCodeTime());
    }
  }

  /**
   * Test that verifying an email code for a non-existent user fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void checkEmailCodeNonExistentUser() throws Exception {
    assertEquals(UserDB.CODE_FAILED, UserDB.checkEmailVerificationCode(
      getDataSource(), "idontexist@test.com", "anything"));
  }

  /**
   * Test that verifying a password reset code for a non-existent user fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void checkPasswordCodeNonExistentUser() throws Exception {
    assertEquals(UserDB.CODE_FAILED, UserDB.checkPasswordResetCode(
      getDataSource(), "idontexist@test.com", "anything"));
  }

  /**
   * Test that verifying an empty email verification code fails when the code
   * has been set.
   *
   * @param code
   *          The empty code
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void checkEmptyEmailCodeSetTest(String code) throws Exception {
    createUser(true);
    assertEquals(UserDB.CODE_FAILED, UserDB
      .checkEmailVerificationCode(getDataSource(), TEST_USER_EMAIL, code));
  }

  /**
   * Test that verifying an empty email verification code fails when the code
   * has not been set.
   *
   * @param code
   *          The empty code
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void checkEmptyEmailCodeNotSetTest(String code) throws Exception {
    createUser(false);
    assertEquals(UserDB.CODE_FAILED, UserDB
      .checkEmailVerificationCode(getDataSource(), TEST_USER_EMAIL, code));
  }

  /**
   * Test that verifying an empty password reset code fails when the code has
   * been set.
   *
   * @param code
   *          The empty code
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void checkEmptyPasswordCodeSetTest(String code) throws Exception {
    User user = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), user);
    assertEquals(UserDB.CODE_FAILED,
      UserDB.checkPasswordResetCode(getDataSource(), TEST_USER_EMAIL, code));
  }

  /**
   * Test that verifying an empty password reset code fails when the code has
   * not been set.
   *
   * @param code
   *          The empty code
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void checkEmptyPasswordCodeNotSetTest(String code) throws Exception {
    createUser(false);
    assertEquals(UserDB.CODE_FAILED,
      UserDB.checkPasswordResetCode(getDataSource(), TEST_USER_EMAIL, code));
  }

  /**
   * Test that checking a valid email verification code that has expired will
   * fail.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/User/UserDBTest/expiredCodes" })
  @Test
  public void expiredValidEmailCodeTest() throws Exception {
    assertEquals(UserDB.CODE_EXPIRED, UserDB.checkEmailVerificationCode(
      getDataSource(), "expiredcodes@test.com", "IAMACODE"));
  }

  /**
   * Test that checking an invalid email verification code that has expired will
   * fail.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/User/UserDBTest/expiredCodes" })
  @Test
  public void expiredInvalidEmailCodeTest() throws Exception {
    assertNotEquals(UserDB.CODE_OK, UserDB.checkEmailVerificationCode(
      getDataSource(), "expiredcodes@test.com", "IAMACODE"));
  }

  /**
   * Test that checking a valid email verification code that has expired will
   * fail.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/User/UserDBTest/expiredCodes" })
  @Test
  public void expiredValidPasswordCodeTest() throws Exception {
    assertEquals(UserDB.CODE_EXPIRED, UserDB.checkPasswordResetCode(
      getDataSource(), "expiredcodes@test.com", "IAMACODE"));
  }

  /**
   * Test that checking an invalid email verification code that has expired will
   * fail.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/data/User/UserDBTest/expiredCodes" })
  @Test
  public void expiredInvalidPasswordCodeTest() throws Exception {
    assertNotEquals(UserDB.CODE_OK, UserDB.checkPasswordResetCode(
      getDataSource(), "expiredcodes@test.com", "IAMACODE"));
  }

  /**
   * Test that changing a password correctly changes the authentication results
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void changePasswordTest() throws Exception {

    User user = createUser(false);
    char[] newPassword = "NewPassword".toCharArray();

    // Original password succeeds
    assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(),
      TEST_USER_EMAIL, TEST_USER_PASSWORD));

    // New password fails
    assertEquals(UserDB.AUTHENTICATE_FAILED,
      UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, newPassword));

    UserDB.changePassword(getDataSource().getConnection(), user, newPassword);

    // Original password fails
    assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB
      .authenticate(getDataSource(), TEST_USER_EMAIL, TEST_USER_PASSWORD));

    // New password succeeds
    assertEquals(UserDB.AUTHENTICATE_OK,
      UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, newPassword));
  }

  /**
   * Test that clearing the email verification code works.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void clearEmailVerificationCodeTest() throws Exception {
    User user = createUser(false);
    UserDB.generateEmailVerificationCode(getDataSource(), user);

    // Verify that the code has been set
    User codeUser = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    assertNotNull(codeUser.getEmailVerificationCode());
    assertNotNull(codeUser.getEmailVerificationCodeTime());

    UserDB.clearEmailVerificationCode(getDataSource(), TEST_USER_EMAIL);

    // Verify that the code has been cleared
    User clearedUser = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    assertNull(clearedUser.getEmailVerificationCode());
    assertNull(clearedUser.getEmailVerificationCodeTime());
  }

  /**
   * Test that clearing the password reset code works.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void clearPasswordResetCodeTest() throws Exception {
    User user = createUser(false);
    UserDB.generatePasswordResetCode(getDataSource(), user);

    // Verify that the code has been set
    User codeUser = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    assertNotNull(codeUser.getPasswordResetCode());
    assertNotNull(codeUser.getPasswordResetCodeTime());

    UserDB.clearPasswordResetCode(getDataSource().getConnection(),
      TEST_USER_EMAIL);

    // Verify that the code has been cleared
    User clearedUser = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
    assertNull(clearedUser.getPasswordResetCode());
    assertNull(clearedUser.getPasswordResetCodeTime());
  }

  /**
   * Test that trying to retrieve user prefs with a missing
   * {@link javax.sql.DataSource} fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getPrefsMissingDataSourceTest() throws Exception {
    User user = createUser(false);
    assertThrows(MissingParamException.class, () -> {
      UserDB.getPreferences(null, user.getDatabaseID());
    });
  }

  /**
   * Test that trying to retrieve user prefs with a an invalid user ID fails.
   *
   * @param id
   *          The test user ID.
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void getPrefsInvalidIdTest(long id) throws Exception {

    assertThrows(MissingParamException.class, () -> {
      UserDB.getPreferences(getDataSource(), id);
    });
  }

  /**
   * Test that trying to retrieve user prefs with a missing
   * {@link javax.sql.DataSource} fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getPrefsNonExistentUserTest() throws Exception {

    assertThrows(NoSuchUserException.class, () -> {
      UserDB.getPreferences(getDataSource(), 1000000L);
    });
  }

  /**
   * Test that retrieving the preferences for a user with no stored preferences
   * returns an empty {@link UserPreferences} object.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getPrefsUserWithNoPrefs() throws Exception {
    // A newly created user has no preferences
    User user = createUser(false);
    UserPreferences prefs = UserDB.getPreferences(getDataSource(),
      user.getDatabaseID());
    assertEquals(0, prefs.size());
  }

  /**
   * Test that user preferences can be correctly stored and retrieved.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void setSetAndRetrievePrefs() throws Exception {
    User user = createUser(false);
    UserPreferences prefs = UserDB.getPreferences(getDataSource(),
      user.getDatabaseID());
    prefs.setLastInstrument(1000L);

    UserDB.savePreferences(getDataSource(), prefs);

    UserPreferences storedPrefs = UserDB.getPreferences(getDataSource(),
      user.getDatabaseID());

    assertEquals(1000L, storedPrefs.getLastInstrument());
  }

  /**
   * Test that setting user preferences with a missing
   * {@link javax.sql.DataSource} fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void setPrefsMissingDataSourceTest() throws Exception {
    User user = createUser(false);
    assertThrows(MissingParamException.class, () -> UserDB.savePreferences(null,
      new UserPreferences(user.getDatabaseID())));
  }

  /**
   * Test that setting user preferences with a missing {@link UserPreferences}
   * object fails.
   */
  @FlywayTest
  @Test
  public void setPrefsMissingPrefsTest() {
    assertThrows(MissingParamException.class,
      () -> UserDB.savePreferences(getDataSource(), null));
  }

  /**
   * Test that setting user preferences with a missing {@link UserPreferences}
   * object fails.
   */
  @FlywayTest
  @Test
  public void setPrefsForNonExistentUserTest() {
    assertThrows(NoSuchUserException.class, () -> UserDB
      .savePreferences(getDataSource(), new UserPreferences(1000L)));
  }
}
