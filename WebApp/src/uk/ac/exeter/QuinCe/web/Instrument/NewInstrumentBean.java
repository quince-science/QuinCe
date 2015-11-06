package uk.ac.exeter.QuinCe.web.Instrument;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 *
 */
public class NewInstrumentBean extends BaseManagedBean {

	private static final String NAMES_PAGE = "names";
	
	private String name = null;
	
	
	public String start() {
		clearData();
		return NAMES_PAGE;
	}
	/**
	 * Clear all the data from the bean
	 */
	private void clearData() {
		name = null;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
