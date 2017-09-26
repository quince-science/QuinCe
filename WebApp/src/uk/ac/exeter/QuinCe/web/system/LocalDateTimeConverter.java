package uk.ac.exeter.QuinCe.web.system;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
	 * The formatter
	 */
	private static DateTimeFormatter formatter = null;
	
	static {
		formatter = DateTimeFormatter.ofPattern(FORMAT);
	}
	
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) throws ConverterException {

		LocalDateTime result = null;
		
		if (null != arg2) {
			try {
				result = LocalDateTime.parse(arg2, formatter);
			} catch (DateTimeException e) {
				throw new ConverterException(e);
			}
		}
		
		return result;
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) throws ConverterException {
		String result = null;
		
		if (null != arg2) {
			try {
				result = ((LocalDateTime) arg2).format(formatter);
			} catch (DateTimeException e) {
				throw new ConverterException(e);
			}
		}
		return result;
	}
}
