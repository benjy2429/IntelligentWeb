package projectFiles;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import exceptions.FileException;


/**
 * This class creates a stop list by reading in stop words from a txt file
 * @author Luke Heavens & Ben Carr
 */
public class StopList {
	//Location of stop words
	private final String FILEPATH = "stop_list.txt";
	//Data structure to store words
	private HashSet<String> stopList = new HashSet<String>();
	//Logger
	private static final Logger LOGGER = Logger.getLogger(StopList.class.getName());
	
	
	/**
	 * Constructor - Create a stop list from a text file containing stop words
	 * @throws FileException
	 */
	public StopList() throws FileException{
		try{
			//Open file
			InputStream inStream = this.getClass().getClassLoader().getResourceAsStream(FILEPATH);
			BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
			String line;
			//Each word is on a separate line
			while ((line = br.readLine()) != null) {
				stopList.add(line);
			}
			//Close file
			br.close();
		}catch(Exception ex){
			//If something goes wrong, log and inform calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FileException("Error generating stoplist");
		}	
	}
	
	
	/**
	 * This method checks to see if a passed word is in the stop list
	 * @param word - A word to check
	 * @return - True if word is present in stop list, false otherwise
	 */
	public boolean wordInStopList(String word) {
		return stopList.contains(word);
	}
}

