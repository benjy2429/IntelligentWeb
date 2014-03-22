package exceptions;

/**
 * This exception is used if a link is expanded that is not a valid foursquare url
 * @author Luke Heavens & Ben Carr
 */
public class InvalidFoursquareUrlException extends Exception{
	private static final long serialVersionUID = 3942289351605995001L;
	public InvalidFoursquareUrlException(String message){
		super(message);
	}
}