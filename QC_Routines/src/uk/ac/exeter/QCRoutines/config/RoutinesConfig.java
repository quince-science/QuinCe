package uk.ac.exeter.QCRoutines.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.exeter.QCRoutines.Routine;
import uk.ac.exeter.QCRoutines.RoutineException;


/**
 * Represents the configuration of the sanity checkers to be run against
 * input files.
 */
public class RoutinesConfig {
	
	/**
	 * The name of the package in which all sanity checker classes will be stored
	 */
	private static final String ROUTINE_CLASS_ROOT = "uk.ac.exeter.QCRoutines.routines.";

	/**
	 * All sanity check class names must end with the same text
	 */
	private static final String ROUTINE_CLASS_TAIL = "Routine";

	/**
	 * The list of routine objects.
	 */
	private List<CheckerInitData> routineClasses;
	
	/**
	 * The name of the configuration file for the sanity checks
	 */
	private static String itsConfigFilename = null;
	
	/**
	 * The singleton instance of this class
	 */
	private static RoutinesConfig instance = null;
	
	/**
	 * Initialises the sanity checker configuration. This cannot be run
	 * until {@link RoutinesConfig#init(String, Logger)} has been called.
	 * @throws ConfigException If the configuration cannot be loaded
	 */
	public RoutinesConfig() throws ConfigException {
		if (itsConfigFilename == null) {
			throw new ConfigException(null, "SanityCheckConfig filename has not been set - must run init first");
		}
		
		routineClasses = new ArrayList<CheckerInitData>();
		readFile();
	}
	
	/**
	 * Initialise the variables required to bootstrap the SanityCheckerConfig
	 * @param filename The name of the configuration file
	 * @param logger A logger instance
	 */
	public static void init(String filename) throws ConfigException {
		itsConfigFilename = filename;
		instance = new RoutinesConfig();
	}
	
	/**
	 * Retrieves the singleton instance of the SanityCheckerConfig, creating it if it
	 * does not exist
	 * @return An instance of the SanityCheckerConfig
	 * @throws ConfigException If the configuration cannot be loaded
	 */
	public static RoutinesConfig getInstance() throws ConfigException {
		if (null == instance) {
			throw new ConfigException(null, "RoutinesConfig has not been initialised");
		}
		
		return instance;
	}
	
	/**
	 * Destroys the singleton instance of the SanityCheckerConfig
	 */
	public static void destroy() {
		instance = null;
	}
	
	/**
	 * Read and parse the configuration file
	 * @throws ConfigException If the configuration cannot be loaded
	 */
	private void readFile() throws ConfigException {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(itsConfigFilename));
			try {
				String line = reader.readLine();
				int lineCount = 1;
				
				while (null != line) {
					if (!isComment(line)) {
						List<String> fields = Arrays.asList(line.split(","));
						fields = trimList(fields);
						
						// The first field is the class name. Grab it and remove
						// it from the list, so what's left is the parameters.
						String className = fields.remove(0);
						String fullClassName = ROUTINE_CLASS_ROOT + className + ROUTINE_CLASS_TAIL;

						if (className.equalsIgnoreCase("")) {
							throw new ConfigException(itsConfigFilename, lineCount, "Sanity Check class name cannot be empty");
						} else {
							try {
								// Instantiate the class and call the initialise method
								// to make sure everything's OK.
								Class<?> routineClass = Class.forName(fullClassName);
								Routine routineInstance = (Routine) routineClass.newInstance();
								routineInstance.initialise(fields);
								
								// Add the checker class to the list of all known checkers.
								// These will be instantiated in the getInstances() method.
								routineClasses.add(new CheckerInitData(routineClass, fields));
							} catch(ClassNotFoundException e) {
								throw new ConfigException(itsConfigFilename, lineCount, "Sanity check class '" + fullClassName + "' does not exist");
							} catch(Exception e) {
								throw new ConfigException(itsConfigFilename, lineCount, "Error creating Sanity check class", e);
							}
						}
					}
					
					line = reader.readLine();
					lineCount++;
				}
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw new ConfigException(itsConfigFilename, "I/O Error while reading file", e);
		}
	}
	
	/**
	 * Returns a list containing fresh instances of all the configured sanity checker classes
	 * @return A list containing fresh instances of all the configured sanity checker classes
	 */
	public List<Routine> getRoutines() throws RoutineException {
		List<Routine> checkers = new ArrayList<Routine>(routineClasses.size());
		
		try {
			for (CheckerInitData checkerData: routineClasses) {
				Routine checkInstance = (Routine) checkerData.checkerClass.newInstance();
				checkInstance.initialise(checkerData.params);
				checkers.add(checkInstance);
			}
		} catch (Exception e) {
			if (e instanceof RoutineException) {
				throw (RoutineException) e;
			} else {
				throw new RoutineException("Error initialising sanity checker instance", e);
			}
		}
		
		return checkers;
	}
	
	/**
	 * A helper class to hold details of a given sanity checker.
	 * A new instance of each sanity checker is created for each file,
	 * and this class contains the details required to construct it.
	 */
	private class CheckerInitData {
		
		/**
		 * The class of the sanity checker
		 */
		private Class<?> checkerClass;
		
		/**
		 * The parameters for the sanity checker
		 */
		private List<String> params;
		
		/**
		 * Builds an object containing all the details required to initialise
		 * a given sanity checker.
		 * @param checkerClass The class of the sanity checker
		 * @param params The parameters for the sanity checker
		 */
		private CheckerInitData(Class<?> checkerClass, List<String> params) {
			this.checkerClass = checkerClass;
			this.params = params;
		}
	}

	/**
	 * Determines whether or not a line is a comment, signified by it starting with {@code #} or {@code !} or {@code //}
	 * @param line The line to be checked
	 * @return {@code true} if the line is a comment; {@code false} otherwise.
	 */
	private boolean isComment(String line) {
		String trimmedLine = line.trim();
		return trimmedLine.length() == 0 || trimmedLine.charAt(0) == '#' || trimmedLine.charAt(0) == '!' || trimmedLine.startsWith("//", 0);
	}
	
	/**
	 * Trims all items in a list of strings
	 * @param source The strings to be converted 
	 * @return The converted strings
	 */
	private List<String> trimList(List<String> source) {
		
		List<String> result = new ArrayList<String>(source.size());
		
		for (int i = 0; i < source.size(); i++) {
			result.add(source.get(i).trim());
		}
		
		return result;
	}
}

