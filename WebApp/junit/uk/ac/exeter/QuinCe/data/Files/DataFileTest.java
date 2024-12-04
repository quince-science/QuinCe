package uk.ac.exeter.QuinCe.data.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Tests for the {@link DataFile} class.
 */
public class DataFileTest {

  /**
   * Create a mock {@link FileDefinition} with the specified ID and name.
   *
   * @param id
   *          The ID.
   * @param name
   *          The name.
   * @return The mock {@link FileDefinition}.
   */
  private FileDefinition makeFileDefinition(long id, String name) {
    FileDefinition fileDef = Mockito.mock(FileDefinition.class);
    Mockito.when(fileDef.getDatabaseId()).thenReturn(id);
    Mockito.when(fileDef.getFileDescription()).thenReturn(name);
    return fileDef;
  }

  /**
   * Create an {@link InstrumentFileSet} containing the specified
   * {@link FileDefinition}s.
   *
   * @param definitions
   *          The {@link FileDefinition}s.
   * @return The {@link InstrumentFileSet}.
   */
  private InstrumentFileSet makeFileDefinitions(FileDefinition... definitions) {
    InstrumentFileSet result = new InstrumentFileSet();
    for (FileDefinition def : definitions) {
      result.add(def);
    }
    return result;
  }

  /**
   * Create a mock {@link Instrument} with the specified details.
   *
   * @param fileDefinitions
   *          The file definitions used by the instrument.
   * @return The {@link Instrument} object.
   */
  private Instrument makeInstrument(InstrumentFileSet fileDefinitions) {
    Instrument result = Mockito.mock(Instrument.class);
    Mockito.when(result.getFileDefinitions()).thenReturn(fileDefinitions);
    return result;
  }

  /**
   * Make a {@link LocalDateTime} object with the specified day of the month,
   * and all other values identical.
   *
   * @param day
   *          The day of the month.
   * @return The {@link LocalDateTime}.
   */
  private LocalDateTime makeTime(int day) {
    return LocalDateTime.of(2024, 12, day, 0, 0, 0);
  }

  @Test
  public void hasConcurrentFilesSingleDefMissBeforeTest()
    throws MissingParamException, DataFileException {

    FileDefinition fileDef = makeFileDefinition(1L, "One");

    InstrumentFileSet fileDefinitions = makeFileDefinitions(fileDef);

    List<DataFile> files = new ArrayList<DataFile>();
    files.add(new TestDataFile(1L, fileDef, "file", makeTime(5), makeTime(10)));

    assertFalse(DataFile.hasConcurrentFiles(makeInstrument(fileDefinitions),
      files, makeTime(1), makeTime(2)));
  }

  @Test
  public void hasConcurrentFilesSingleDefMissAfterTest()
    throws MissingParamException, DataFileException {

    FileDefinition fileDef = makeFileDefinition(1L, "One");

    InstrumentFileSet fileDefinitions = makeFileDefinitions(fileDef);

    List<DataFile> files = new ArrayList<DataFile>();
    files.add(new TestDataFile(1L, fileDef, "file", makeTime(5), makeTime(10)));

    assertFalse(DataFile.hasConcurrentFiles(makeInstrument(fileDefinitions),
      files, makeTime(12), makeTime(20)));
  }

  @Test
  public void hasConcurrentFilesSingleDefFrontOverlapTest()
    throws MissingParamException, DataFileException {

    FileDefinition fileDef = makeFileDefinition(1L, "One");

    InstrumentFileSet fileDefinitions = makeFileDefinitions(fileDef);

    List<DataFile> files = new ArrayList<DataFile>();
    files.add(new TestDataFile(1L, fileDef, "file", makeTime(5), makeTime(10)));

    assertTrue(DataFile.hasConcurrentFiles(makeInstrument(fileDefinitions),
      files, makeTime(2), makeTime(6)));
  }

  @Test
  public void hasConcurrentFilesSingleDefBackOverlapTest()
    throws MissingParamException, DataFileException {

    FileDefinition fileDef = makeFileDefinition(1L, "One");

    InstrumentFileSet fileDefinitions = makeFileDefinitions(fileDef);

    List<DataFile> files = new ArrayList<DataFile>();
    files.add(new TestDataFile(1L, fileDef, "file", makeTime(5), makeTime(10)));

    assertTrue(DataFile.hasConcurrentFiles(makeInstrument(fileDefinitions),
      files, makeTime(8), makeTime(12)));
  }

  @Test
  public void hasConcurrentFilesSingleDefTotalOverlapTest()
    throws MissingParamException, DataFileException {

    FileDefinition fileDef = makeFileDefinition(1L, "One");

    InstrumentFileSet fileDefinitions = makeFileDefinitions(fileDef);

    List<DataFile> files = new ArrayList<DataFile>();
    files.add(new TestDataFile(1L, fileDef, "file", makeTime(5), makeTime(10)));

    assertTrue(DataFile.hasConcurrentFiles(makeInstrument(fileDefinitions),
      files, makeTime(1), makeTime(20)));
  }

  @Test
  public void hasConcurrentFilesSingleDefEncompassedTest()
    throws MissingParamException, DataFileException {

    FileDefinition fileDef = makeFileDefinition(1L, "One");

    InstrumentFileSet fileDefinitions = makeFileDefinitions(fileDef);

    List<DataFile> files = new ArrayList<DataFile>();
    files.add(new TestDataFile(1L, fileDef, "file", makeTime(5), makeTime(10)));

    assertTrue(DataFile.hasConcurrentFiles(makeInstrument(fileDefinitions),
      files, makeTime(6), makeTime(7)));
  }

  private static Stream<Arguments> hasConcurrentFilesMultipleFileDefsTestParams() {
    return Stream.of(Arguments.of(1, 3, false), Arguments.of(3, 6, false),
      Arguments.of(7, 8, false), Arguments.of(5, 8, false),
      Arguments.of(7, 9, false), Arguments.of(8, 11, true),
      Arguments.of(4, 11, true), Arguments.of(1, 11, true));
  }

  @ParameterizedTest
  @MethodSource("hasConcurrentFilesMultipleFileDefsTestParams")
  public void hasConcurrentFilesMultipleFileDefsTest(int periodStart,
    int periodEnd, boolean expectedResult)
    throws MissingParamException, DataFileException {

    FileDefinition fileDef1 = makeFileDefinition(1L, "One");
    FileDefinition fileDef2 = makeFileDefinition(2L, "Two");
    FileDefinition fileDef3 = makeFileDefinition(3L, "Three");

    InstrumentFileSet fileDefinitions = makeFileDefinitions(fileDef1, fileDef2,
      fileDef3);

    List<DataFile> files = new ArrayList<DataFile>();
    files.add(new TestDataFile(1L, fileDef1, "one", makeTime(2), makeTime(3)));
    files
      .add(new TestDataFile(2L, fileDef1, "two", makeTime(10), makeTime(11)));
    files
      .add(new TestDataFile(3L, fileDef2, "three", makeTime(4), makeTime(6)));
    files
      .add(new TestDataFile(4L, fileDef2, "four", makeTime(9), makeTime(10)));
    files
      .add(new TestDataFile(5L, fileDef3, "five", makeTime(7), makeTime(10)));

    assertEquals(expectedResult,
      DataFile.hasConcurrentFiles(makeInstrument(fileDefinitions), files,
        makeTime(periodStart), makeTime(periodEnd)));
  }
}
