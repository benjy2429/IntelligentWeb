package exceptions;
/**
 * This exception is used if there is a problem trying to access files 
 * @author Luke Heavens
 */
public class FileException extends Exception{
	public FileException(String message){
		super(message);
	}
}
