package projectFiles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import exceptions.FileException;

public class StopList {
	private final String FILEPATH = "stop_list.txt";
	private HashSet<String> stopList = new HashSet<String>();
	
	public StopList() throws FileException{
		try{
			InputStream inStream = this.getClass().getClassLoader().getResourceAsStream(FILEPATH);
			BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
			String line;
			while ((line = br.readLine()) != null) {
				stopList.add(line);
			}
			br.close();
		}catch(Exception ex){
			ex.printStackTrace();
			throw new FileException("Error reading stoplist");
		}	
	}
	
	public boolean wordInStopList(String word) {
		return stopList.contains(word);
	}

}
