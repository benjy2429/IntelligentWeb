package projectFiles;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twitter4j.Status;
import twitter4j.TwitterException;

import com.google.gson.Gson;

/**
 * Servlet implementation class DatabaseServlet
 */
public class DatabaseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();		
    	String requestId = request.getParameter("requestId");    	
    	Gson gson = new Gson();
    	String json = "";
    	
    	//TODO Change json character encoding to utf-8
    	  	
    	if (requestId.equals("showUser")) {
    		try {
    			HashMap<String, String> user = new HashMap<String, String>();
    			List<HashMap<String, String>> retweetersOfUser = new LinkedList<HashMap<String, String>>();
    			List<HashMap<String, String>> userRetweets = new LinkedList<HashMap<String, String>>();
    			List<HashMap<String, String>> userLocations = new LinkedList<HashMap<String, String>>();
    			List<HashMap<String, String>> userKeywords = new LinkedList<HashMap<String, String>>();
    			String username = request.getParameter("username");

    			DatabaseConnector dbConn = new DatabaseConnector();
    			dbConn.establishConnection();
    			user = dbConn.showUser(username);
    			
    			long userId = Long.parseLong(user.get("userId"));
    			
    			retweetersOfUser = dbConn.getRetweetersOfUser(userId);
    			userRetweets = dbConn.getUserRetweets(userId);
    			userLocations = dbConn.getUserLocations(userId);
    			userKeywords = dbConn.getUserKeywords(userId);
    			
    			dbConn.closeConnection();
    			
    			json = gson.toJson( user );
    			json += "\n";
    			json += gson.toJson( retweetersOfUser );
    			json += "\n";
    			json += gson.toJson( userRetweets );
    			json += "\n";
    			json += gson.toJson( userLocations );
    			json += "\n";
    			json += gson.toJson( userKeywords );
    			
    		} catch (SQLException e) {
				System.out.println(e.getMessage());
			}    		

    	} else if (requestId.equals("showVenue")) {
    		try {
    			HashMap<String, String> venue = new HashMap<String, String>();
    			List<HashMap<String, String>> users = new LinkedList<HashMap<String, String>>();
    			String venueName = request.getParameter("venueName");

    			DatabaseConnector dbConn = new DatabaseConnector();
    			dbConn.establishConnection();
    			venue = dbConn.showVenue(venueName);
    			String venueId = venue.get("locId");
    			users = dbConn.getVenueVisitors(venueId);
    			dbConn.closeConnection();
    			
    			json = gson.toJson( venue );
    			json += "\n";
    			json += gson.toJson( users );
    			
    		} catch (SQLException e) {
				System.out.println(e.getMessage());
			}    		
    	}
    	
		out.print( json );
    	out.close();
	}

}
