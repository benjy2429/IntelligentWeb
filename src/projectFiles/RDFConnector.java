package projectFiles;

import java.util.HashMap;
import java.util.List;

import fi.foyt.foursquare.api.entities.CompleteVenue;
import twitter4j.User;

public class RDFConnector {

	public boolean establishConnection() {
		return false;
		// TODO Auto-generated method stub
		
	}

	public void addUsers(User user) {
		// TODO Auto-generated method stub
		
	}

	public void addContact(long id, long id2) {
		// TODO Auto-generated method stub
		
	}

	public int addWord(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getWordId(String term) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void addUserTermPair(Long t, int wordId, Integer u) {
		// TODO Auto-generated method stub
		
	}

	public void addVenues(CompleteVenue venue) {
		// TODO Auto-generated method stub
		
	}

	public void addUserVenue(long id, String id2) {
		// TODO Auto-generated method stub
		
	}
	
	public HashMap<String, String> showUser(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<HashMap<String, String>> getRetweetersOfUser(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<HashMap<String, String>> getUserRetweets(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<HashMap<String, String>> getUserLocations(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<HashMap<String, String>> getUserKeywords(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	public HashMap<String, String> showVenue(String venueName) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<HashMap<String, String>> getVenueVisitors(String venueId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void closeConnection() {
		// TODO Auto-generated method stub
		
	}
}
