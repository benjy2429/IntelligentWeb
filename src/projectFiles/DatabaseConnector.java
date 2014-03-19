package projectFiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;

import fi.foyt.foursquare.api.entities.CompleteVenue;
import twitter4j.*;

public class DatabaseConnector {
	private static final String DBSERVER = "stusql.dcs.shef.ac.uk";
	private static final String DBNAME = "team007";
	private static final String DBUSERNAME = "team007";
	private static final String DBPASSWORD = "a57078a1";
	Connection dbConnection;
	
	public boolean establishConnection(){
	 	try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String DB="jdbc:mysql://"+DBSERVER+"/" + DBNAME + "?user=" + DBUSERNAME + "&password=" + DBPASSWORD;
			dbConnection = DriverManager.getConnection(DB);
			return true;
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException  | SQLException e) {
			e.printStackTrace();
			return false;
		} 
	}
	
	
	public void closeConnection(){
		try {
			dbConnection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
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
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void addVenues(CompleteVenue venue){
		try {
			String sql = "REPLACE INTO Locations VALUES (?,?,?,?,?,?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, venue.getId());
			preStmt.setString(2, venue.getName());
			String venuePhotoUrl;
			try {
				venuePhotoUrl = venue.getPhotos().getGroups()[1].getItems()[0].getUrl();
			} catch (Exception e) {
				venuePhotoUrl = "";
			}
			preStmt.setString(3, venuePhotoUrl);
			preStmt.setString(4, venue.getLocation().getAddress());
			preStmt.setString(5, venue.getLocation().getCity());
			preStmt.setString(6, venue.getUrl());
			preStmt.setString(7, venue.getDescription());
			
			preStmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void addContact(long tweeterId, long retweeterId) {
		try {
			String sql = "INSERT IGNORE INTO UserUserContact VALUES (?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(tweeterId));
			preStmt.setString(2, String.valueOf(retweeterId));

			preStmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public int addWord(String term) {
		try {
			String sql = "INSERT IGNORE INTO Keywords(word) VALUES (?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			preStmt.setString(1, term);
			preStmt.executeUpdate();
			
			ResultSet generatedKeys = preStmt.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            return generatedKeys.getInt(1);
	        } else {
	            return -1;
	        }
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	
	}


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
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void addUserVenue(long userId, String venueId) {
		try {		
			String sql = "INSERT IGNORE INTO UserLocation VALUES (?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			preStmt.setString(2, venueId);
			preStmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


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
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}


	public HashMap<String,String> showUser(String username) throws SQLException {
		HashMap<String,String> userResult = new HashMap<String,String>();
		String sql = "SELECT * FROM Users WHERE screenName = ?";
		PreparedStatement preStmt = dbConnection.prepareStatement(sql);
		preStmt.setString(1, username);
		
		ResultSet result = preStmt.executeQuery();
		if (result.next()) {
			userResult.put( "userId", result.getString("userId") );
			userResult.put( "fullName", result.getString("fullName") );
			userResult.put( "screenName", result.getString("screenName") );
			userResult.put( "hometown", result.getString("hometown") );
			userResult.put( "bigProfileImgUrl", result.getString("bigProfileImgUrl") );
			userResult.put( "bannerImgUrl", result.getString("bannerImgUrl") );
			userResult.put( "description", result.getString("description") );
		}
		
		return userResult;
	}
	
	//TODO do error throwing things
	public LinkedList<HashMap<String,String>> getRetweetersOfUser(long userId) {
		LinkedList<HashMap<String,String>> retweeters = new LinkedList<HashMap<String,String>>();
		
		try {
			String sql = "SELECT Users.* FROM Users, UserUserContact WHERE UserUserContact.userA = ? AND Users.userId = UserUserContact.userB";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();	
			while (result.next()) {
				HashMap<String,String> userHashMap = new HashMap<String,String>();
				userHashMap.put( "fullName", result.getString("fullName") );
				userHashMap.put( "screenName", result.getString("screenName") );
				userHashMap.put( "profileImgUrl", result.getString("profileImgUrl") );
				retweeters.add(userHashMap);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retweeters;
	}
	
	
	public LinkedList<HashMap<String,String>> getUserRetweets(long userId) {
		LinkedList<HashMap<String,String>> retweeters = new LinkedList<HashMap<String,String>>();
		
		try {
			String sql = "SELECT Users.* FROM Users, UserUserContact WHERE UserUserContact.userB = ? AND Users.userId = UserUserContact.userA";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();	
			while (result.next()) {
				HashMap<String,String> userHashMap = new HashMap<String,String>();
				userHashMap.put( "fullName", result.getString("fullName") );
				userHashMap.put( "screenName", result.getString("screenName") );
				userHashMap.put( "profileImgUrl", result.getString("profileImgUrl") );
				retweeters.add(userHashMap);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retweeters;
	}
	
	
	public LinkedList<HashMap<String,String>> getUserLocations(long userId) {
		LinkedList<HashMap<String,String>> locations = new LinkedList<HashMap<String,String>>();
		
		try {
			String sql = "SELECT Locations.* FROM Locations, UserLocation WHERE UserLocation.userId = ? AND Locations.locId = UserLocation.locId";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();
			while (result.next()) {
				HashMap<String,String> locationHashMap = new HashMap<String,String>();
				locationHashMap.put( "name", result.getString("name") );
				locationHashMap.put( "imageUrl", result.getString("imageUrl") );
				locationHashMap.put( "address", result.getString("address") );
				locationHashMap.put( "city", result.getString("city") );
				locationHashMap.put( "websiteUrl", result.getString("websiteUrl") );
				locationHashMap.put( "description", result.getString("description") );
				locations.add(locationHashMap);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return locations;
	}
	

	public LinkedList<HashMap<String,String>> getUserKeywords(long userId) {
		LinkedList<HashMap<String,String>> keywords = new LinkedList<HashMap<String,String>>();
		
		try {
			String sql = "SELECT Keywords.*, UserKeyword.* FROM Keywords, UserKeyword WHERE UserKeyword.userId = ? AND Keywords.wordId = UserKeyword.wordId ORDER BY UserKeyword.count DESC LIMIT 0, 10";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(userId));
			ResultSet result = preStmt.executeQuery();
			while (result.next()) {
				HashMap<String,String> keywordHashMap = new HashMap<String,String>();
				keywordHashMap.put( "word", result.getString("word") );
				keywordHashMap.put( "count", result.getString("count") );
				keywords.add(keywordHashMap);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keywords;
	}	
	
	
	public HashMap<String,String> showVenue(String venueName) throws SQLException {
		HashMap<String,String> venue = new HashMap<String,String>();

		String sql = "SELECT * FROM Locations WHERE name = ?";
		PreparedStatement preStmt = dbConnection.prepareStatement(sql);
		preStmt.setString(1, venueName);
		
		ResultSet result = preStmt.executeQuery();
		if (result.next()) {
			venue.put( "locId", result.getString("locId") );
			venue.put( "name", result.getString("name") );
			venue.put( "imageUrl", result.getString("imageUrl") );
			venue.put( "address", result.getString("address") );
			venue.put( "city", result.getString("city") );
			venue.put( "websiteUrl", result.getString("websiteUrl") );
			venue.put( "description", result.getString("description") );
		}

		return venue;
	}	
	
	
	public LinkedList<HashMap<String,String>> getVenueVisitors(String venueId) throws SQLException {
		LinkedList<HashMap<String,String>> users = new LinkedList<HashMap<String,String>>();

		String sql = "SELECT Users.* FROM Users, UserLocation WHERE UserLocation.locId = ? AND Users.userId = UserLocation.userId";
		PreparedStatement preStmt = dbConnection.prepareStatement(sql);
		preStmt.setString(1, venueId);
		ResultSet result = preStmt.executeQuery();
		while (result.next()) {
			HashMap<String,String> userHashMap = new HashMap<String,String>();
			userHashMap.put( "userId", result.getString("userId") );
			userHashMap.put( "fullName", result.getString("fullName") );
			userHashMap.put( "screenName", result.getString("screenName") );
			userHashMap.put( "hometown", result.getString("hometown") );
			userHashMap.put( "profileImgUrl", result.getString("profileImgUrl") );
			userHashMap.put( "description", result.getString("description") );
			users.add(userHashMap);
		}
			
		return users;
	}
	
}
