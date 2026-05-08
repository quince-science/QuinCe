package uk.ac.exeter.QuinCe.data.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.FileUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Class to handle storage, retrieval and management of data file on disk.
 *
 * <p>
 * Note that these methods are not publicly accessible; all calls to this class
 * are made through the {@code DataFileDB} class.
 * </p>
 *
 * @see DataFileDB
 */
public class FileStore {

  /**
   * Store a file in the file store. This will overwrite any existing file.
   *
   * @param fileStore
   *          The location of the file store
   * @param fileDefinitionId
   *          The ID of the file's {@link FileDefinition}.
   * @param fileId
   *          The ID of the file.
   * @throws MissingParamException
   *           If any of the parameters are missing
   * @throws FileStoreException
   *           If an error occurs while storing the file
   * @see DataFileDB#storeFile(DataSource, Properties, DataFile)
   */
  protected static void storeFile(String fileStore, DataFile dataFile)
    throws MissingParamException, FileStoreException {

    MissingParam.checkMissing(fileStore, "fileStore");
    MissingParam.checkMissing(dataFile, "dataFile");

    FileWriter fileWriter = null;
    File file = null;

    try {
      checkInstrumentDirectory(fileStore,
        dataFile.getFileDefinition().getDatabaseId());

      file = getFileObject(fileStore,
        dataFile.getFileDefinition().getDatabaseId(), dataFile.getDatabaseId());
      if (file.exists()) {
        file.delete();
      }

      fileWriter = new FileWriter(file);
      fileWriter.write(dataFile.getContentsAsString());
      fileWriter.close();

    } catch (Exception e) {

      closeWriter(fileWriter);
      deleteFile(file);

      throw new FileStoreException("An error occurred while storing the file",
        e);
    }
  }

  /**
   * Deletes a file from the file store
   *
   * @param fileStore
   *          The location of the file store
   * @param dataFile
   *          The data file
   * @throws MissingParamException
   *           If any of the parameters are missing
   * @see DataFileDB#deleteFile(DataSource, Properties, DataFile)
   */
  protected static void deleteFile(String fileStore, DataFile dataFile)
    throws MissingParamException {

    MissingParam.checkMissing(fileStore, "fileStore");
    MissingParam.checkMissing(dataFile, "dataFile");

    File fileToDelete = getFileObject(fileStore,
      dataFile.getFileDefinition().getDatabaseId(), dataFile.getDatabaseId());
    deleteFile(fileToDelete);
  }

  /**
   * Get the raw bytes for a file
   *
   * @param fileStore
   *          The file store
   * @param dataFile
   *          The file to be retrieved
   * @return The file bytes
   * @throws IOException
   *           If the file cannot be read
   */
  protected static byte[] getBytes(String fileStore, long fileDefinitionId,
    long fileId) throws IOException {

    byte[] fileData;
    File readFile = getFileObject(fileStore, fileDefinitionId, fileId);

    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(readFile);
      int fileLength = (int) readFile.length();
      fileData = new byte[fileLength];
      int bytesRead = inputStream.read(fileData);
      if (bytesRead < fileLength) {
        throw new IOException(
          "Too few bytes read from file " + readFile.getAbsolutePath()
            + ": got " + bytesRead + ", expected " + fileLength);
      }
    } catch (IOException e) {
      throw e;
    } finally {
      if (null != inputStream) {
        inputStream.close();
      }
    }

    return fileData;
  }

  /**
   * Ensure that the directory for a given instrument's files exists
   *
   * @param fileStorePath
   *          The root path of the file store
   * @param fileDefinitionId
   *          The instrument ID
   * @throws FileStoreException
   *           If the directory doesn't exist and can't be created
   */
  private static void checkInstrumentDirectory(String fileStorePath,
    long fileDefinitionId) throws FileStoreException {

    File file = new File(getStorageDirectory(fileStorePath, fileDefinitionId));
    if (!file.exists()) {
      boolean dirMade = file.mkdirs();
      if (!dirMade) {
        throw new FileStoreException(
          "Unable to create directory for file definition ID "
            + fileDefinitionId);
      }
    } else if (!file.isDirectory()) {
      throw new FileStoreException(
        "The path to the instrument directory is not a directory!");
    }
  }

  protected static void deleteFolder(String fileStore, long fileDefinitionId)
    throws FileStoreException, IOException {

    File dir = new File(getStorageDirectory(fileStore, fileDefinitionId));
    if (dir.exists()) {
      if (!dir.isDirectory()) {
        throw new FileStoreException(
          "The path to the instrument directory is not a directory!");
      }

      if (!FileUtils.isDirectoryEmpty(dir)) {
        throw new FileStoreException("Directory is not empty");
      }

      dir.delete();
    }
  }

  /**
   * Returns the path to the directory where a given instrument's files are
   * stored
   *
   * @param fileStorePath
   *          The root of the file store
   * @param fileDefinitionId
   *          The file definition database ID
   * @return The directory path
   */
  private static String getStorageDirectory(String fileStorePath,
    long fileDefinitionId) {
    return fileStorePath + File.separator + fileDefinitionId;
  }

  /**
   * Get the Java File object for a data file
   *
   * @param fileStorePath
   *          The path to the data file within the file store
   * @param dataFile
   *          The data file
   * @return The Java File object
   */
  private static File getFileObject(String fileStorePath, long fileDefinitionId,
    long fileId) {
    return new File(getStorageDirectory(fileStorePath, fileDefinitionId)
      + File.separator + fileId);
  }

  /**
   * Close a writer. Not strictly a database thing, but it's in the same spirit
   * and used in the same places.
   *
   * @param writer
   *          The writer
   */
  private static void closeWriter(Writer writer) {
    if (null != writer) {
      try {
        writer.close();
      } catch (IOException e) {
        // Do nothing
      }
    }
  }

  /**
   * Delete a file from the file system. If the file does not exist, no action
   * is taken.
   *
   * @param file
   *          The file to be deleted
   */
  private static void deleteFile(File file) {
    if (null != file) {
      if (file.exists()) {
        file.delete();
      }
    }
  }
}
