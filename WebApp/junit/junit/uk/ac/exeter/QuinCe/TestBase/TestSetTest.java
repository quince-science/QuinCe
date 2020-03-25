package junit.uk.ac.exeter.QuinCe.TestBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;

public abstract class TestSetTest extends BaseTest {

  private List<TestSetLine> lines = null;

  protected List<TestSetLine> getLines() throws TestSetException {
    if (null == lines) {
      lines = getTestSetLines();
    }

    return lines;
  }

  /**
   * Get the name of the test set
   *
   * @return
   */
  protected abstract String getTestSetName();

  /**
   * Get the contents of a Test Set file as a {@link Stream} of
   * {@link TestSetLine} objects.
   *
   * <p>
   * Some tests take a large combination of inputs and have a corresponding
   * number of expected results. Instead of providing all these in code, it can
   * be easier to build an input file containing the test criteria.
   * </p>
   *
   * <p>
   * This method provides a means for a test to have its criteria defined in a
   * {@code .csv} file. It reads a given file, and provides a {@link Stream} of
   * {@link TestSetLine} objects each representing a single line in the file,
   * which can be used as input to a {@link ParameterizedTest}.
   * </p>
   *
   * <p>
   * Note that this functionality knows nothing about the structure of any given
   * {@code .csv} file - it is up to the test to know which columns it needs to
   * read for its own purposes.
   * </p>
   *
   * <p>
   * The input parameter for this method is the base filename of the test set
   * file (without extension). This is converted to the path
   * {@code resources/testsets/<parameter>.csv}.
   * </p>
   *
   * @see TestSetLine
   *
   * @param testSet
   *          The base name of the test set file
   * @return The {@link Stream} of lines from the file as {@link TestSetLine}
   *         objects
   * @throws IOException
   *           If the file cannot be read
   */
  private List<TestSetLine> getTestSetLines() throws TestSetException {

    File testSetFile = null;
    List<TestSetLine> lines = new ArrayList<TestSetLine>();
    BufferedReader in = null;

    try {
      testSetFile = context
        .getResource(
          "classpath:resources/testsets/" + getTestSetName() + ".csv")
        .getFile();

      in = new BufferedReader(new FileReader(testSetFile));
      // Skip the first line
      in.readLine();

      int lineNumber = 1;
      String line;
      while ((line = in.readLine()) != null) {
        lineNumber++;
        lines.add(new TestSetLine(lineNumber, line.split(",", -1)));
      }
    } catch (Exception e) {
      throw new TestSetException(getTestSetName(), e);
    } finally {
      try {
        if (null != in) {
          in.close();
        }
      } catch (Exception e) {
        // Meh
      }
    }

    return lines;
  }

}
