package exceptions;
/**
 * This exception is used if there is a problem trying to interact with the database 
 * @author Luke Heavens & Ben Carr
 */
public class DatabaseException extends Exception{
	private static final long serialVersionUID = -3046489910681922204L;
	public DatabaseException(String message){
		super(message);
	}
}