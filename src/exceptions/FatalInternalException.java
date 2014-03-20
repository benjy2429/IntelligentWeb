package exceptions;

/**
 * This exception is used if there is a problem whilst executing a query
 * @author Luke Heavens & Ben Carr
 */
public class FatalInternalException extends Exception{
	private static final long serialVersionUID = -7935624732989760111L;
	public FatalInternalException(String message){
		super(message);
	}
}