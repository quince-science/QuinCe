package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.List;

/**
 * Specifies the latitude format for a data file
 * @author Steve Jones
 *
 */
public class LatitudeSpecification extends PositionSpecification {

	/**
	 * Indicates that latitudes are between -90 and 90
	 */
	public static final int FORMAT_MINUS90_90 = 0;
	
	/**
	 * Indicates that longitudes are between 0 and 90,
	 * with a separate column specifying the hemisphere
	 */
	public static final int FORMAT_0_90 = 1;
	
	/**
	 * Basic constructor
	 */
	public LatitudeSpecification() {
		super();
	}

	/**
	 * Constructor for a complete specification
	 * @param format The format
	 * @param valueColumn The value column
	 * @param hemisphereColumn The hemisphere column
	 * @throws PositionException If the specification is incomplete or invalid
	 */
	public LatitudeSpecification(int format, int valueColumn, int hemisphereColumn) throws PositionException {
		super(format, valueColumn, hemisphereColumn);
	}

	@Override
	public boolean formatValid(int format) {
		return (format >= 0 && format <= 1);
	}

	@Override
	public boolean hemisphereRequired() {
		return (getFormat() == FORMAT_0_90);
	}
	
	@Override
	public double getValue(List<String> line) throws PositionException {
		
		double value;
		
		try {
			value = Double.parseDouble(line.get(getValueColumn()));
		
			switch (format) {
			case FORMAT_MINUS90_90: {
				// No need to do anything!
				break;
			}
			case FORMAT_0_90: {
				String hemisphere = line.get(getHemisphereColumn());
				value = value * hemisphereMultiplier(hemisphere);
				break;
			}
			default: {
				throw new InvalidPositionFormatException(format);
			}
			}
		} catch (NumberFormatException e) {
			throw new PositionException("Invalid latitude value " + line.get(getValueColumn()));
		}
		
		if (value < -90 || value > 90) {
			throw new PositionException("Invalid latitude value " + value);
		}
		
		return value;
	}
	
	/**
	 * Calculate the longitude multiplier for a longitude value. East = 1, West = -1
	 * @param hemisphere The hemisphere
	 * @return The multiplier
	 * @throws PositionException If the hemisphere value is invalid
	 */
	private double hemisphereMultiplier(String hemisphere) throws PositionException {
		double multiplier = 1.0;
		
		if (null == hemisphere) {
			throw new PositionException("Missing hemisphere value");
		}
		
		switch (hemisphere.toLowerCase()) {
		case "n":
		case "north": {
			multiplier = 1.0;
			break;
		}
		case "s":
		case "south": {
			multiplier = -1.0;
		}
		default: {
			throw new PositionException("Invalid hemisphere value " + hemisphere);
		}
		}
		
		return multiplier;
	}
}
