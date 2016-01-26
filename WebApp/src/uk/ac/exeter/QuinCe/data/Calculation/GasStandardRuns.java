package uk.ac.exeter.QuinCe.data.Calculation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RunType;

/**
 * Represents a set of gas standard runs from a data file
 * @author Steve Jones
 *
 */
public class GasStandardRuns {

	/**
	 * The database ID of the data file to which these gas standard runs belong
	 */
	private long fileId;
	
	/**
	 * The set of gas standard means for the data file
	 */
	private Map<String,TreeSet<GasStandardMean>> standardMeans;
	
	/**
	 * Creates an empty set of gas standard runs for a given data file
	 * @param fileId The database ID of the file
	 */
	public GasStandardRuns(long fileId, Instrument instrument) {
		this.fileId = fileId;
		standardMeans = new HashMap<String,TreeSet<GasStandardMean>>();
		for (RunType runType : instrument.getRunTypes()) {
			standardMeans.put(runType.getName(), new TreeSet<GasStandardMean>());
		}
	}
	
	/**
	 * Returns the database ID of the data file to which these gas standard runs belong
	 * @return The data file ID
	 */
	public long getFileId() {
		return fileId;
	}

	/**
	 * Add the mean results of a specific gas standard run to the object
	 * @param standardMean The gas standard results
	 */
	public void addStandardMean(GasStandardMean standardMean) {
		TreeSet<GasStandardMean> runMeans = standardMeans.get(standardMean.getRunName());		
		runMeans.add(standardMean);
	}
}
