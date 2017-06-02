package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

/**
 * Handles all formats of longitudes, and the corresponding column assignments within a data file
 * @author Steve Jones
 *
 */
public class LongitudeSpecification extends PositionSpecification {

	/**
	 * Indicates that longitudes are between 0 and 360
	 */
	public static final int FORMAT_0_360 = 0;
	
	/**
	 * Indicates that longitudes are between -180 and 180
	 */
	public static final int FORMAT_MINUS180_180 = 1;
	
	/**
	 * Indicates that longitudes are between 0 and 180, with an extra
	 * field denoting East or West
	 */
	public static final int FORMAT_0_180 = 2;
	
	/**
	 * Basic constructor
	 */
	public LongitudeSpecification() {
		super();
	}

	@Override
	public boolean formatValid(int format) {
		return (format >= 0 && format <= 2);
	}

	@Override
	protected boolean hemisphereRequired() {
		return (getFormat() == FORMAT_0_180);
	}
}
