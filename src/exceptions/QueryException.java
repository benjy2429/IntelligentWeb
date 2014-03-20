package exceptions;

/**
 * This exception is used if there is a problem whilst executing a query
 * @author Luke Heavens & Ben Carr
 */
public class QueryException extends Exception{
	private static final long serialVersionUID = 3942289351605995001L;
	public QueryException(String message){
		super(message);
	}
}
