package uk.ac.exeter.QuinCe.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Holds details of the various file export options
 * configured for the application.
 * 
 * @author Steve Jones
 *
 */
public class ExportConfig {

	private static ExportConfig instance = null;
	
	private List<ExportOption> options = null;
	
	private static String configFilename = null;
	
	private ExportConfig() throws ExportException {
		if (configFilename == null) {
			throw new ExportException("ExportConfig filename has not been set - must run init first");
		}

		options = new ArrayList<ExportOption>();
		try {
			readFile();
		} catch (FileNotFoundException e) {
			throw new ExportException("Could not find configuration file '" + configFilename + "'");
		}
	}
	
	public static void init(String configFile) throws ExportException {
		configFilename = configFile;
		instance = new ExportConfig();
	}
	
	public static ExportConfig getInstance() throws ExportException {
		
		if (null == instance) {
			throw new ExportException("Export options have not been configured");
		}
		
		return instance;
	}
	
	private void readFile() throws ExportException, FileNotFoundException {
		
		BufferedReader reader = new BufferedReader(new FileReader(configFilename));
		
		String regex = "(?<!\\\\)" + Pattern.quote(",");
		int lineCount = 0;
		
		try {
			String line = reader.readLine();
			lineCount++;
			
			while (null != line) {
				if (!StringUtils.isComment(line)) {
					List<String> fields = Arrays.asList(line.split(regex));
					fields = StringUtils.trimList(fields);
					
					String name = fields.get(0);
					String separator = fields.get(1);
					if (separator.equals("\\t")) {
						separator = "\t";
					}
					
					int co2Type = Integer.parseInt(fields.get(2));
					
					List<String> columns = fields.subList(3, fields.size());
					
					options.add(new ExportOption(options.size(), name, separator, columns, co2Type));
				}
				
				line = reader.readLine();
				lineCount++;
			}
		} catch (Exception e) {
			if (e instanceof ExportException) {
				throw (ExportException) e;
			} else {
				throw new ExportException("Error initialising export options (line " + lineCount + ")", e);
			}
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				// Shrug
			}
		}
	}
	
	public List<ExportOption> getOptions() {
		return options;
	}
	
	public ExportOption getOption(int index) {
		return options.get(index);
	}
}
