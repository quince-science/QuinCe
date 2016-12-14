package uk.ac.exeter.QuinCe.database.files;


public class FileStoreException extends Exception {

	private static final long serialVersionUID = -269122182568751400L;

	public FileStoreException(String message) {
		super(message);
	}
	
	public FileStoreException(String message, Throwable cause) {
		super(message, cause);
	}
}
