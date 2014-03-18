package projectFiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import fi.foyt.foursquare.api.entities.CompleteVenue;
import twitter4j.*;

public class DatabaseConnector {
	private String dbName = "team007";
	private String dbUsername = "team007";
	private String dbPassword = "a57078a1";
	Connection dbConnection;
	
	public boolean establishConnection(){
	 	try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String DB="jdbc:mysql://stusql.dcs.shef.ac.uk/" + dbName + "?user=" + dbUsername + "&password=" + dbPassword;
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
		String sql = "REPLACE INTO Users VALUES (?,?,?,?,?,?)";
		try (PreparedStatement preStmt = dbConnection.prepareStatement(sql)) {
			preStmt.setString(1, String.valueOf(user.getId()));
			preStmt.setString(2, user.getName());
			preStmt.setString(3, user.getScreenName());
			preStmt.setString(4, user.getLocation());
			preStmt.setString(5, user.getOriginalProfileImageURL());
			preStmt.setString(6, user.getDescription());
			
			preStmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addVenues(CompleteVenue venue){
		String sql = "REPLACE INTO Locations VALUES (?,?,?,?,?,?)";
		try (PreparedStatement preStmt = dbConnection.prepareStatement(sql)) {
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
		String sql = "INSERT IGNORE INTO UserUserContact VALUES (?,?)";
		try (PreparedStatement preStmt = dbConnection.prepareStatement(sql)) {
			preStmt.setString(1, String.valueOf(tweeterId));
			preStmt.setString(2, String.valueOf(retweeterId));

			preStmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public int addWord(String term) {
		String sql = "INSERT IGNORE INTO Keywords(word) VALUES (?)";
		try (PreparedStatement preStmt = dbConnection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
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
			String sql = "INSERT IGNORE INTO UserKeyword VALUES (?,?,?)";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, String.valueOf(wordId));
			preStmt.setString(2, String.valueOf(userId));
			preStmt.setString(3, String.valueOf(userCount));
			preStmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private long getUserId(String userName){
		try {
			String sql = "SELECT userId FROM User WHERE screenName = ?";
			PreparedStatement preStmt = dbConnection.prepareStatement(sql);
			preStmt.setString(1, userName);
			ResultSet result = preStmt.executeQuery();	
			 if (result.next()) {
				 return result.getLong(1);
			 } else {
				 return -1;
			 }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
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

	
	
	
	
	
	
}
