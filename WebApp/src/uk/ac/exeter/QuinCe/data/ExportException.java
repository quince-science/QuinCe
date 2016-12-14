package uk.ac.exeter.QuinCe.data;

public class ExportException extends Exception {
	
	private static final long serialVersionUID = -3739928561949968607L;

	public ExportException(String message) {
		super(message);
	}
	
	public ExportException(String name, String message) {
		super("Error in exporter '" + name + "': " + message);
	}
	
	public ExportException(String name, Throwable cause) {
		super("Error in exporter '" + name + "': " + cause.getMessage(), cause);
	}

}
