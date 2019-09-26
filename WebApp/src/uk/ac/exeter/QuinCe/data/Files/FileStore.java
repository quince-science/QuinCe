package uk.ac.exeter.QuinCe.data.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.sql.DataSource;

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
 * @author Steve Jones
 * @see DataFileDB
 */
public class FileStore {

  /**
   * Store a file in the file store. This will overwrite any existing file.
   *
   * @param fileStore
   *          The location of the file store
   * @param dataFile
   *          The data file
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

      file = getFileObject(fileStore, dataFile);
      if (file.exists()) {
        file.delete();
      }

      fileWriter = new FileWriter(file);
      fileWriter.write(dataFile.getContents());
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

    File fileToDelete = getFileObject(fileStore, dataFile);
    deleteFile(fileToDelete);
  }

  /**
   * Retrieve a file from the file store
   * 
   * @param fileStore
   *          The location of the file store
   * @param dataFile
   *          The file whose contents are to be loaded
   * @throws IOException
   *           If a disk I/O error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  protected static void loadFileContents(String fileStore, DataFile dataFile)
    throws IOException, MissingParamException {
    dataFile.setContents(new String(getBytes(fileStore, dataFile)));
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
  protected static byte[] getBytes(String fileStore, DataFile dataFile)
    throws IOException {

    byte[] fileData;
    File readFile = getFileObject(fileStore, dataFile);

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
  private static File getFileObject(String fileStorePath, DataFile dataFile) {
    return new File(getStorageDirectory(fileStorePath,
      dataFile.getFileDefinition().getDatabaseId()) + File.separator
      + dataFile.getDatabaseId());
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
