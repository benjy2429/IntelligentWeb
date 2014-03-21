package exceptions;

/**
 * This exception is used if there is a problem that cannot be recovered from.
 * USed to indicate a HTTP response of 500 should be made
 * @author Luke Heavens & Ben Carr
 */
public class FatalInternalException extends Exception{
	private static final long serialVersionUID = -7935624732989760111L;
	public FatalInternalException(String message){
		super(message);
	}
}