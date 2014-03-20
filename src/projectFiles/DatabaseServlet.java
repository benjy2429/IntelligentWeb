package projectFiles;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import exceptions.DatabaseException;


/**
 * This class deals with interactions between the web interface for querying the database and the database
 * @author Luke Heavens & Ben Carr
 */
public class DatabaseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Logger
	private static final Logger LOGGER = Logger.getLogger(DatabaseServlet.class.getName());

	
	/**
	 * When the servlet receives a get request, it delivers the databaseInterface html file
	 * @param request - HttpServletRequest object
	 * @param response - HttpServletResponse object
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect("databaseInterface.html");
	}

	
	/**
	 * When the servlet receives a post request, it looks for and examines a requestId parameter. This determines the appropriate action to take.
	 * A JSON object is constructed which is communicated through the response containing relevant data
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();		
    	String requestId = request.getParameter("requestId");    	
    	Gson gson = new Gson();
    	String json = "";
    	
    	//TODO Change json character encoding to utf-8
    	
    	//If the request indicates a search of a user then we fetch data concerning the user entered
    	if (requestId.equals("showUser")) {
    		try {
    			//Create data structures for results
    			HashMap<String, String> user = new HashMap<String, String>();
    			List<HashMap<String, String>> retweetersOfUser = new LinkedList<HashMap<String, String>>();
    			List<HashMap<String, String>> userRetweets = new LinkedList<HashMap<String, String>>();
    			List<HashMap<String, String>> userLocations = new LinkedList<HashMap<String, String>>();
    			List<HashMap<String, String>> userKeywords = new LinkedList<HashMap<String, String>>();
    			String username = request.getParameter("username");

    			//Open database connection
    			DatabaseConnector dbConn = new DatabaseConnector();
    			dbConn.establishConnection();
    			
    			//Get user and id
    			user = dbConn.showUser(username);
    			long userId = Long.parseLong(user.get("userId"));
    			
    			//Run database queries and store results
    			retweetersOfUser = dbConn.getRetweetersOfUser(userId);
    			userRetweets = dbConn.getUserRetweets(userId);
    			userLocations = dbConn.getUserLocations(userId);
    			userKeywords = dbConn.getUserKeywords(userId);
    			
    			//Close database connection
    			dbConn.closeConnection();
    			
    			//Store results for JASON
    			json = gson.toJson(user);
    			json += "\n";
    			json += gson.toJson(retweetersOfUser);
    			json += "\n";
    			json += gson.toJson(userRetweets);
    			json += "\n";
    			json += gson.toJson(userLocations);
    			json += "\n";
    			json += gson.toJson(userKeywords);
    			
    		} catch (DatabaseException ex) {
    			//An error occurred whilst accessing the database so catch and log
				LOGGER.log(Level.SEVERE, ex.getMessage());
			} 
    		
    	//If the request indicates a search of a venue then we fetch data concerning the venue entered
    	} else if (requestId.equals("showVenue")) {
    		try {
    			//Create data structures for results
    			HashMap<String, String> venue = new HashMap<String, String>();
    			List<HashMap<String, String>> users = new LinkedList<HashMap<String, String>>();
    			String venueName = request.getParameter("venueName");

    			//Open database connection
    			DatabaseConnector dbConn = new DatabaseConnector();
    			dbConn.establishConnection();
    			
    			//Get venue and id
    			venue = dbConn.showVenue(venueName);
    			String venueId = venue.get("locId");
    			
    			//Run database queries
    			users = dbConn.getVenueVisitors(venueId);
    			
    			//Close database connection
    			dbConn.closeConnection();
    			
    			//Store results for JASON
    			json = gson.toJson(venue);
    			json += "\n";
    			json += gson.toJson(users);
    	
    		} catch (DatabaseException ex) {
    			//An error occured whilst accessing the database so catch and log
				LOGGER.log(Level.SEVERE, ex.getMessage());
			}   		
    	}
    	
		//Output JSON
		out.print(json);
    	out.close();
	}
}

