package junit.uk.ac.exeter.QuinCe.TestBase;

import java.util.stream.Stream;

/**
 * Useful methods for tests. Extend this class to use them
 * @author Steve Jones
 *
 */
public class BaseTest {

  @SuppressWarnings("unused")
  private static Stream<String> createNullEmptyStrings() {
    return Stream.of(null, "", " ");
  }
}
