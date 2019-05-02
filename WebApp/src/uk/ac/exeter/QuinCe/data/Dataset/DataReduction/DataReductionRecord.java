package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class DataReductionRecord {

	/**
	 * The database ID of the measurement
	 */
	private long measurementId;
	
	/**
	 * The database ID of the variable being processed
	 */
	private long variableId;
	
	/**
	 * Intermediate calculation values
	 */
	private HashMap<String, Double> calculationValues;
	
	/**
	 * QC Flag
	 */
	private Flag qcFlag;
	
	/**
	 * QC Message
	 */
	private List<String> qcMessages;

	/**
	 * Create an empty record for a given measurement
	 * @param measurement The measurement
	 */
	public DataReductionRecord(Measurement measurement) {
	  this.measurementId = measurement.getId();
	  this.variableId = measurement.getVariable().getId();
	  
	  this.calculationValues = new HashMap<String, Double>();
	  this.qcFlag = Flag.NEEDED;
	  this.qcMessages = new ArrayList<String>();
	}
	
	/**
	 * Set the QC details for the record
	 * @param flag The QC flag
	 * @param message The QC messages
	 */
	protected void setQc(Flag flag, List<String> messages) {
	  this.qcFlag = flag;
	  this.qcMessages = messages;
	}
	
	/**
	 * Store a calculation value
	 * @param parameter The parameter
	 * @param value The value
	 */
	protected void put(String parameter, Double value) {
	  calculationValues.put(parameter, value);
	}
}
