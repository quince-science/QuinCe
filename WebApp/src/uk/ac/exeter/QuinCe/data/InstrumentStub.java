package uk.ac.exeter.QuinCe.data;

public class InstrumentStub {

	/**
	 * The instrument's database ID
	 */
	private long id;
	
	/**
	 * The instrument's name
	 */
	private String name;
	
	/**
	 * Simple constructor
	 * @param id The instrument's database ID
	 * @param name The instrument's name
	 */
	public InstrumentStub(long id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Returns the instrument's database ID
	 * @return The instrument's database ID
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Return the instrument's name
	 * @return The instrument's name
	 */
	public String getName() {
		return name;
	}
}
