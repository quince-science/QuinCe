package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.HashMap;

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
	private String qcMessage;

	/**
	 * Create an empty record for a given measurement
	 * @param measurement The measurement
	 */
	public DataReductionRecord(Measurement measurement) {
	  this.measurementId = measurement.getId();
	  this.variableId = measurement.getVariable().getId();
	  
	  this.calculationValues = new HashMap<String, Double>();
	  this.qcFlag = Flag.NEEDED;
	  this.qcMessage = null;
	}
	
	/**
	 * Set the QC details for the record
	 * @param flag The QC flag
	 * @param message The QC message
	 */
	protected void setQc(Flag flag, String message) {
	  this.qcFlag = flag;
	  this.qcMessage = message;
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
