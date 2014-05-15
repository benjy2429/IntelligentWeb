package exceptions;

/**
 * This exception is used if there is a problem trying to interact with the datastore
 * @author Luke Heavens & Ben Carr
 */
public class DatastoreException extends Exception{
	private static final long serialVersionUID = -3046489910681922204L;
	public DatastoreException(String message){
		super(message);
	}
}