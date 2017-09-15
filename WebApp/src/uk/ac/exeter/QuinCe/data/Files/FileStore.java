package uk.ac.exeter.QuinCe.data.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Class to handle storage, retrieval and management of data file on disk.
 * 
 * <p>
 *   Note that these methods are not publicly accessible; all calls to this
 *   class are made through the {@code DataFileDB} class.
 * </p>
 * @author Steve Jones
 * @see DataFileDB
 */
public class FileStore {

	/**
	 * Store a file in the file store.
	 * This will overwrite any existing file.
	 * 
	 * @param config The application configuration
	 * @param instrumentID The instrument ID
	 * @param dataFile The data file
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws FileStoreException If an error occurs while storing the file
	 * @see DataFileDB#storeFile(DataSource, Properties, User, long, RawDataFile)
	 */
	protected static void storeFile(Properties config, long instrumentID, RawDataFile dataFile) throws MissingParamException, FileStoreException {
		
		MissingParam.checkMissing(config, "config");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(dataFile, "dataFile");
		
		FileWriter fileWriter = null;
		File file = null;

		try {

			checkInstrumentDirectory(config.getProperty("filestore"), instrumentID);
			
			file = getFileObject(config.getProperty("filestore"), instrumentID, dataFile.getFileName());
			
			if (file.exists()) {
				file.delete();
			}
			
			fileWriter = new FileWriter(file);
			fileWriter.write(dataFile.getContentsAsString(true));
			fileWriter.close();
			
		} catch (Exception e) {
			
			closeWriter(fileWriter);
			deleteFile(file);
			
			throw new FileStoreException("An error occurred while storing the file", e);
		}
	}
	
	/**
	 * Deletes a file from the file store
	 * @param config The application configuration
	 * @param fileDetails The file details
	 * @throws MissingParamException If any of the parameters are missing
	 * @see DataFileDB#deleteFile(DataSource, Properties, FileInfo)
	 */
	protected static void deleteFile(Properties config, FileInfo fileDetails) throws MissingParamException {
		
		MissingParam.checkMissing(config, "config");
		MissingParam.checkMissing(fileDetails, "fileDetails");
		
		File fileToDelete = getFileObject(config.getProperty("filestore"), fileDetails.getInstrumentId(), fileDetails.getFileName());
		deleteFile(fileToDelete);
	}
	
	/**
	 * Retrieve a file from the file store
	 * @param dataSource A data source
	 * @param config The application configuration
	 * @param fileInfo The details of the file to load
	 * @return The data file
	 * @throws IOException If a disk I/O error occurs
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RawDataFileException If there is a fault in processing the data file
	 * @throws RecordNotFoundException If any required database records cannot be found
	 * @throws ResourceException If the application configuration cannot be retrieved
	 * @throws InstrumentException If any instrument details are invalid
	 * @see DataFileDB#getRawDataFile(DataSource, Properties, long)
	 */
	protected static RawDataFile getFile(DataSource dataSource, Properties config, FileInfo fileInfo) throws IOException, MissingParamException, DatabaseException, RawDataFileException, RecordNotFoundException, InstrumentException, ResourceException {
		Instrument instrument = InstrumentDB.getInstrument(dataSource, fileInfo.getInstrumentId(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
		File readFile = getFileObject(config.getProperty("filestore"), fileInfo.getInstrumentId(), fileInfo.getFileName());

		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(readFile);
			int fileLength = (int) readFile.length();
			byte[] fileData = new byte[fileLength];
			int bytesRead = inputStream.read(fileData);
			if (bytesRead < fileLength) {
				throw new IOException("Too few bytes read from file " + readFile.getAbsolutePath() + ": got " + bytesRead + ", expected " + fileLength);
			}
			return new RawDataFile(instrument, fileInfo.getFileName(), fileData, true);
		} catch (IOException|RawDataFileException e) {
			throw e;
		} finally {
			if (null != inputStream) {
				inputStream.close();
			}
		}
	}
	
	/**
	 * Ensure that the directory for a given instrument's files exists
	 * @param fileStorePath The root path of the file store
	 * @param instrumentID The instrument ID
	 * @throws FileStoreException If the directory doesn't exist and can't be created
	 */
	private static void checkInstrumentDirectory(String fileStorePath, long instrumentID) throws FileStoreException {
		
		File file = new File(getInstrumentDirectory(fileStorePath, instrumentID));
		if (!file.exists()) {
			boolean dirMade = file.mkdirs();
			if (!dirMade) {
				throw new FileStoreException("Unable to create directory for instrument ID " + instrumentID);
			}
		} else if (!file.isDirectory()) {
			throw new FileStoreException("The path to the instrument directory is not a directory!");
		}
	}
	
	/**
	 * Returns the path to the directory where a given instrument's files are stored
	 * @param fileStorePath The root of the file store
	 * @param instrumentID The instrument ID
	 * @return The directory path
	 */
	private static String getInstrumentDirectory(String fileStorePath, long instrumentID) {
		return fileStorePath + File.separator + instrumentID;
	}
	
	/**
	 * Get the Java File object for a stored data file
	 * @param fileStorePath The path to the data file within the file store
	 * @param instrumentId The database ID of the instrument to which the file belongs
	 * @param fileName The filename of the data file
	 * @return The Java File object
	 */
	private static File getFileObject(String fileStorePath, long instrumentId, String fileName) {
		return new File(getInstrumentDirectory(fileStorePath, instrumentId) + File.separator + fileName);
	}
	
	/**
	 * Close a writer. Not strictly a database thing,
	 * but it's in the same spirit and used in the same places.
	 * @param writer The writer
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
	 * Delete a file from the file system. If the file does not exist, no action is taken.
	 * @param file The file to be deleted
	 */
	private static void deleteFile(File file) {
		if (null != file) {
			if (file.exists()) {
				file.delete();
			}
		}
	}
}
