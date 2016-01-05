package uk.ac.exeter.QuinCe.database.files;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Class to handle all things to do with storing data files on disk
 * @author Steve Jones
 *
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
	 */
	protected static void storeFile(Properties config, long instrumentID, RawDataFile dataFile) throws MissingParamException, FileStoreException {
		
		MissingParam.checkMissing(config, "config");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(dataFile, "dataFile");
		
		FileWriter fileWriter = null;
		File file = null;

		try {

			checkInstrumentDirectory(config.getProperty("filestore"), instrumentID);
			
			file = new File(getInstrumentDirectory(config.getProperty("filestore"), instrumentID) + File.separator + dataFile.getFileName());
			
			if (file.exists()) {
				file.delete();
			}
			
			fileWriter = new FileWriter(file);
			fileWriter.write(dataFile.getContentsAsString());
			fileWriter.close();
			
		} catch (Exception e) {
			
			closeWriter(fileWriter);
			deleteFile(file);
			
			throw new FileStoreException("An error occurred while storing the file", e);
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
	 * Delete a file from the file system
	 * @param file
	 */
	private static void deleteFile(File file) {
		if (null != file) {
			if (file.exists()) {
				file.delete();
			}
		}
	}
}
