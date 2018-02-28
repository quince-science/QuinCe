package uk.ac.exeter.QuinCe.web.system;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * Convert LocalDateTime objects to Strings, and back again
 * @author Steve Jones
 * @see "https://stackoverflow.com/questions/34883270/how-to-use-java-time-zoneddatetime-localdatetime-in-pcalendar"
 */
@FacesConverter(forClass=LocalDateTime.class)
public class LocalDateTimeConverter implements Converter {

	/**
	 * The format string to be used by the formatter
	 */
	private static final String FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * The attribute name for the flag that indicates a date-only value
	 */
	private static final String DATE_ONLY_ATTR = "dateOnly";

	/**
	 * The formatter
	 */
	private static DateTimeFormatter formatter = null;

	static {
		formatter = DateTimeFormatter.ofPattern(FORMAT);
	}

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
		LocalDateTime result = null;

		if (null != value) {
			try {
				Map<String, Object> attributes = component.getAttributes();
				Object dateOnly = attributes.get(DATE_ONLY_ATTR);

				String valueToConvert = value;

				if (null != dateOnly && (Boolean.parseBoolean((String) attributes.get(DATE_ONLY_ATTR)))) {
					valueToConvert = valueToConvert + " 00:00:00";
				}

				result = LocalDateTime.parse(valueToConvert, formatter);
			} catch (DateTimeException e) {
				e.printStackTrace();
				throw new ConverterException(e);
			}
		}

		return result;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
		String result = null;

		if (null != value) {
			try {
				Map<String, Object> attributes = component.getAttributes();
				Object dateOnly = attributes.get(DATE_ONLY_ATTR);

				result = ((LocalDateTime) value).format(formatter);

				if (null != dateOnly && (Boolean.parseBoolean((String) attributes.get(DATE_ONLY_ATTR)))) {
					result = result.substring(0, 10);
				}
			} catch (DateTimeException e) {
				e.printStackTrace();
				throw new ConverterException(e);
			}
		}
		return result;
	}
}
