package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	  if (flag.equals(qcFlag)) {
	    qcMessages.addAll(messages);
	  } else if (flag.moreSignificantThan(qcFlag)) {
	    qcFlag = flag;
	    qcMessages = messages;
	  }
	}
	
	/**
	 * Store a calculation value
	 * @param parameter The parameter
	 * @param value The value
	 */
	protected void put(String parameter, Double value) {
	  calculationValues.put(parameter, value);
	}
	
	protected void putAll(Map<String, Double> values) {
	  calculationValues.putAll(values);
	}
	
	/**
	 * Get the QC flag
	 * @return The QC flag
	 */
	public Flag getQCFlag() {
	  return qcFlag;
	}
	
	/**
	 * Get the QC messages
	 * @return The QC messages
	 */
	public List<String> getQCMessages() {
	  return qcMessages;
	}
}
