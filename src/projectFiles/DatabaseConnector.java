package projectFiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import exceptions.DatabaseException;
import fi.foyt.foursquare.api.entities.CompleteVenue;
import twitter4j.*;


/**
 * This class provides a collection of methods for connecting and communicating with the system database
 * @author Luke Heavens & Ben Carr
 */
public class DatabaseConnector {
	//Database credentials
	private static final String DBSERVER = "stusql.dcs.shef.ac.uk";
	private static final String DBNAME = "team007";
	private static final String DBUSERNAME = "team007";
	private static final String DBPASSWORD = "a57078a1";
	//Logger
	private static final Logger LOGGER = Logger.getLogger(DatabaseConnector.class.getName());
	//The database connection
	Connection dbConnection;	
	
	/**
	 * This function attempts to establish a connection to a database 
	 * @return returns true if the connection was established else false
	 */
	public boolean establishConnection(){
	 	try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String DB="jdbc:mysql://"+DBSERVER+"/" + DBNAME + "?user=" + DBUSERNAME + "&password=" + DBPASSWORD;
			dbConnection = DriverManager.getConnection(DB);
			return true;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Error establishing connection with database" + ex.getMessage(), ex);
			return false;
		} 
	}
	
	
	/**
	 * This method closes the database connection
	 */
	public void closeConnection(){
		try {
			dbConnection.close();
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Error closing connection with database" + ex.getMessage(), ex);
		}
	}
	
	
	/**
	 * This method takes a user and will add desired fields into the database using a prepared statement
	 * If the user already exists with this id, the information will be updated
	 * @param user - The user object to store
	 */
	public void addUsers(User user){
		try {
			String sql = "REPLACE INTO Users VALUES (?,?,?,?,?,?,?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(user.getId()));
			preStmt.setString(2, user.getName());
			preStmt.setString(3, user.getScreenName());
			preStmt.setString(4, user.getLocation());
			preStmt.setString(5, user.getProfileImageURL());
			preStmt.setString(6, user.getBiggerProfileImageURL());
			preStmt.setString(7, user.getProfileBannerRetinaURL()); 
			preStmt.setString(8, user.getDescription());
			preStmt.executeUpdate();
		} catch (Exception ex) {
			//If any error occurs during a save process, we don't want to do anything other than log the error
			LOGGER.log(Level.SEVERE, "Error saving user information to database" + ex.getMessage(), ex);
		}
	}
	
	
	/**
	 * This method takes a venue and will add desired fields into the database using a prepared statement
	 * If the venue already exists with this id, the information will be updated
	 * @param venue - the venue object to store
	 */
	public void addVenues(CompleteVenue venue){
		try {
			String sql = "REPLACE INTO Locations VALUES (?,?,?,?,?,?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, venue.getId());
			preStmt.setString(2, venue.getName());
			String venuePhotoUrl;
			try {
				venuePhotoUrl = venue.getPhotos().getGroups()[1].getItems()[0].getUrl();
				preStmt.setString(3, venuePhotoUrl);
			} catch (Exception e) { 
				//If there are any problems getting a venue picture just set the field null
				preStmt.setNull(3, java.sql.Types.VARCHAR); //TODO test this
			}
			preStmt.setString(4, venue.getLocation().getAddress());
			preStmt.setString(5, venue.getLocation().getCity());
			preStmt.setString(6, venue.getUrl());
			preStmt.setString(7, venue.getDescription());
			preStmt.executeUpdate();
		} catch (Exception ex) {
			//If any error occurs during a save process, we don't want to do anything other than log the error
			LOGGER.log(Level.SEVERE, "Error saving venue information to database" + ex.getMessage(), ex);
		}
	}


	/**
	 * This method stores the ids of two twitter users to indicate that there has been some interaction
	 * If the combination of user ids exists then we ignore the insertion
	 * @param tweeterId - The id of a user who has tweeted
	 * @param retweeterId - The id of a user who has retweeted the tweeters tweet
	 */
	public void addContact(long tweeterId, long retweeterId) {
		try {
			String sql = "INSERT IGNORE INTO UserUserContact VALUES (?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(tweeterId));
			preStmt.setString(2, String.valueOf(retweeterId));
			preStmt.executeUpdate();
		} catch (Exception ex) {
			//If any error occurs during a save process, we don't want to do anything other than log the error
			LOGGER.log(Level.SEVERE, "Error storing a user-user contact to database" + ex.getMessage(), ex);
		}
	}


	/**
	 * This method adds a frequently used term to the database that doesn't already exist
	 * Returns the id of the term if it was added
	 * @param term - The term used
	 * @return returns wordId of word if added, -1 otherwise
	 */
	public int addWord(String term) {
		try {
			String sql = "INSERT IGNORE INTO Keywords(word) VALUES (?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			preStmt.setString(1, term);
			preStmt.executeUpdate();
			ResultSet generatedKeys = preStmt.getGeneratedKeys();
	        if (generatedKeys.next()) {
	        	//Term was addded so return key
	            return generatedKeys.getInt(1);
	        } else {
	        	//Term wasnt added so return -1
	            return -1;
	        }
		} catch (Exception ex) {
			//If any error occurs during a save process, we don't want to do anything other than log the error
			LOGGER.log(Level.SEVERE, "Error storing a term to database" + ex.getMessage(), ex);
			//Something went wrong so we dont have an id hence return -1
			return -1;
		}			
	}


	/**
	 * This method adds a pairing between a user and a term along with an occurrence count 
	 * If the pairing already exists, the occurrence count is only updated if it is larger than previously seen
	 * This allows significant terms to be identified
	 * @param userId - The id of a user
	 * @param wordId - The id of a term
	 * @param userCount - The number of times the user has seen the word
	 */
	public void addUserTermPair(long userId, int wordId, int userCount) {
		try {		
			String sql = "INSERT IGNORE INTO UserKeyword SET wordId = ?, userId = ?, count = ? ON DUPLICATE KEY UPDATE count = IF (count < ?, ?, count)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(wordId));
			preStmt.setString(2, String.valueOf(userId));
			preStmt.setString(3, String.valueOf(userCount));
			preStmt.setString(4, String.valueOf(userCount));
			preStmt.setString(5, String.valueOf(userCount));
			preStmt.executeUpdate();
		} catch (Exception ex) {
			//If any error occurs during a save process, we don't want to do anything other than log the error
			LOGGER.log(Level.SEVERE, "Error storing a user-term pair to database" + ex.getMessage(), ex);
		}
	}


	/**
	 * This method adds a user venue pairing if the pairing doesnt already exist
	 * This indicates a user has visited a location
	 * @param userId
	 * @param venueId
	 */
	public void addUserVenue(long userId, String venueId) {
		try {		
			String sql = "INSERT IGNORE INTO UserLocation VALUES (?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			preStmt.setString(2, venueId);
			preStmt.executeUpdate();
		} catch (Exception ex) {
			//If any error occurs during a save process, we don't want to do anything other than log the error
			LOGGER.log(Level.SEVERE, "Error storing a user-venue pair to database" + ex.getMessage(), ex);
		}
	}


	/**
	 * This method will look up a word in the database and will return the wordId if it exists.
	 * If it cannot find an index, it will return -1
	 * @param term - The word to look up
	 * @return - Index of word or -1 if not found
	 */
	public int getWordId(String term) {
		try {
			String sql = "SELECT wordId FROM Keywords WHERE word = ?";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, term);
			ResultSet result = preStmt.executeQuery();	
			 if (result.next()) {
				 return result.getInt(1);
			 } else {
				 return -1;
			 }
		} catch (Exception ex) {
			//If any error occurs during a save process, we don't want to do anything other than log the error
			LOGGER.log(Level.SEVERE, "Error storing a user-venue pair to database" + ex.getMessage(), ex);
			//Something went wrong so we dont have an id hence return -1
			return -1;
		}
	}


	/**
	 * This function takes a username and performs a lookup in the database
	 * If a user is found, details are stored in a hashmap and returned
	 * @param username - The user to look for
	 * @return - A hashmap of details
	 * @throws DatabaseException
	 */
	public HashMap<String,String> showUser(String username) throws DatabaseException {
		try {
			HashMap<String,String> userResult = new HashMap<String,String>();
			String sql = "SELECT * FROM Users WHERE screenName = ?";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, username);
			ResultSet result = preStmt.executeQuery();
			if (result.next()) {
				userResult.put("userId", result.getString("userId"));
				userResult.put("fullName", result.getString("fullName"));
				userResult.put("screenName", result.getString("screenName"));
				userResult.put("hometown", result.getString("hometown"));
				userResult.put("bigProfileImgUrl", result.getString("bigProfileImgUrl"));
				userResult.put("bannerImgUrl", result.getString("bannerImgUrl"));
				userResult.put("description", result.getString("description"));
			}
			return userResult;
		} catch (Exception ex) {
			//An error occured, we don't want to do anything other than log the error and inform the calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new DatabaseException("Error fetching user from database");
		}
	}

	
	/**
	 * This function gets user details for users that have retweeted a given user
	 * If users are found, details are stored in a list of hashmaps and returned
	 * @param userId - The tweeter id
	 * @return A List (one for each retweeter) of hash maps (user details) 
	 * @throws DatabaseException
	 */
	public LinkedList<HashMap<String,String>> getRetweetersOfUser(long userId) throws DatabaseException {
		try {
			LinkedList<HashMap<String,String>> retweeters = new LinkedList<HashMap<String,String>>();
			String sql = "SELECT Users.* FROM Users, UserUserContact WHERE UserUserContact.userA = ? AND Users.userId = UserUserContact.userB";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();	
			while (result.next()) {
				HashMap<String,String> userHashMap = new HashMap<String,String>();
				userHashMap.put("fullName", result.getString("fullName"));
				userHashMap.put("screenName", result.getString("screenName"));
				userHashMap.put("profileImgUrl", result.getString("profileImgUrl"));
				retweeters.add(userHashMap);
			}
			return retweeters;
		} catch (Exception ex) {
			//An error occured, we don't want to do anything other than log the error and inform the calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new DatabaseException("Error fetching retweeters from database");
		}
	}
	
	
	/**
	 * This function gets user details for users that have been retweeted by a given user
	 * If users are found, details are stored in a list of hashmaps and returned
	 * @param userId - The retweeter id
	 * @return A list (one for each tweeter) of hash maps (user details)
	 * @throws DatabaseException
	 */
	public LinkedList<HashMap<String,String>> getUserRetweets(long userId) throws DatabaseException {
		try {
			LinkedList<HashMap<String,String>> retweeters = new LinkedList<HashMap<String,String>>();
			String sql = "SELECT Users.* FROM Users, UserUserContact WHERE UserUserContact.userB = ? AND Users.userId = UserUserContact.userA";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();	
			while (result.next()) {
				HashMap<String,String> userHashMap = new HashMap<String,String>();
				userHashMap.put("fullName", result.getString("fullName"));
				userHashMap.put("screenName", result.getString("screenName"));
				userHashMap.put("profileImgUrl", result.getString("profileImgUrl"));
				retweeters.add(userHashMap);
			}
			return retweeters;
		} catch (Exception ex) {
			//An error occured, we don't want to do anything other than log the error and inform the calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new DatabaseException("Error fetching tweets from database");
		}
	}
	
	
	/**
	 * This function takes a userId and gets information of venues they have visited
	 * @param userId - The if of the user to lookup
	 * @return - A list of (one for each venue visted) hash maps (venue details)
	 * @throws DatabaseException
	 */
	public LinkedList<HashMap<String,String>> getUserLocations(long userId) throws DatabaseException {
		try {
			LinkedList<HashMap<String,String>> locations = new LinkedList<HashMap<String,String>>();
			String sql = "SELECT Locations.* FROM Locations, UserLocation WHERE UserLocation.userId = ? AND Locations.locId = UserLocation.locId";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();
			while (result.next()) {
				HashMap<String,String> locationHashMap = new HashMap<String,String>();
				locationHashMap.put("name", result.getString("name"));
				locationHashMap.put("imageUrl", result.getString("imageUrl"));
				locationHashMap.put("address", result.getString("address"));
				locationHashMap.put("city", result.getString("city"));
				locationHashMap.put("websiteUrl", result.getString("websiteUrl"));
				locationHashMap.put("description", result.getString("description"));
				locations.add(locationHashMap);
			}
			return locations;
		} catch (Exception ex) {
			//An error occured, we don't want to do anything other than log the error and inform the calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new DatabaseException("Error fetching user locations from database");
		}
	}
	

	/**
	 * This function takes a userId and gets frequently user terms
	 * @param userId - The if of the user to lookup
	 * @return - A list of (one for each term used) hash maps (term and count)
	 * @throws DatabaseException
	 */
	public LinkedList<HashMap<String,String>> getUserKeywords(long userId) throws DatabaseException {
		try {
			LinkedList<HashMap<String,String>> keywords = new LinkedList<HashMap<String,String>>();
			String sql = "SELECT Keywords.*, UserKeyword.* FROM Keywords, UserKeyword WHERE UserKeyword.userId = ? AND Keywords.wordId = UserKeyword.wordId ORDER BY UserKeyword.count DESC LIMIT 0, 10";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();
			while (result.next()) {
				HashMap<String,String> keywordHashMap = new HashMap<String,String>();
				keywordHashMap.put("word", result.getString("word"));
				keywordHashMap.put("count", result.getString("count"));
				keywords.add(keywordHashMap);
			}
			return keywords;
		} catch (Exception ex) {
			//An error occured, we don't want to do anything other than log the error and inform the calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new DatabaseException("Error fetching user keywords from database");
		}
	}	
	
	
	/**
	 * This function takes a venue name and performs a lookup in the database
	 * If a venue is found, details are stored in a hashmap and returned
	 * @param venueName - The venue to look for
	 * @return - A hashmap of details
	 * @throws DatabaseException
	 */
	public HashMap<String,String> showVenue(String venueName) throws DatabaseException {
		try {
			HashMap<String,String> venue = new HashMap<String,String>();
			String sql = "SELECT * FROM Locations WHERE name = ?";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, venueName);
			ResultSet result = preStmt.executeQuery();
			if (result.next()) {
				venue.put("locId", result.getString("locId"));
				venue.put("name", result.getString("name"));
				venue.put("imageUrl", result.getString("imageUrl"));
				venue.put("address", result.getString("address"));
				venue.put("city", result.getString("city"));
				venue.put("websiteUrl", result.getString("websiteUrl"));
				venue.put("description", result.getString("description"));
			}
			return venue;
		} catch (Exception ex) {
			//An error occured, we don't want to do anything other than log the error and inform the calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new DatabaseException("Error fetching venue from database");
		}
	}	
	
	
	/**
	 * This function takes a venueId and gets information of users that have visited
	 * @param venueId - The id of the venue to lookup
	 * @return - A list of (one for each user who has visited) hash maps (user details)
	 * @throws DatabaseException
	 */
	public LinkedList<HashMap<String,String>> getVenueVisitors(String venueId) throws DatabaseException {
		try {
			LinkedList<HashMap<String,String>> users = new LinkedList<HashMap<String,String>>();
			String sql = "SELECT Users.* FROM Users, UserLocation WHERE UserLocation.locId = ? AND Users.userId = UserLocation.userId";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, venueId);
			ResultSet result = preStmt.executeQuery();
			while (result.next()) {
				HashMap<String,String> userHashMap = new HashMap<String,String>();
				userHashMap.put("userId", result.getString("userId"));
				userHashMap.put("fullName", result.getString("fullName"));
				userHashMap.put("screenName", result.getString("screenName"));
				userHashMap.put("hometown", result.getString("hometown"));
				userHashMap.put("profileImgUrl", result.getString("profileImgUrl"));
				userHashMap.put("description", result.getString("description"));
				users.add(userHashMap);
			}	
			return users;
		} catch (Exception ex) {
			//An error occured, we don't want to do anything other than log the error and inform the calling class
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new DatabaseException("Error fetching user vists to venues from database");
		}
	}
}

