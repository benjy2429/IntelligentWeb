package exceptions;
/**
 * This exception is used if there is a problem trying to access files 
 * @author Luke Heavens & Ben Carr
 */
public class FileException extends Exception{
	private static final long serialVersionUID = -1076549387181594435L;
	public FileException(String message){
		super(message);
	}
}
