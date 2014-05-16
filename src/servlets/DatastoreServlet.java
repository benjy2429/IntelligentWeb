package servlets;

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

import projectFiles.RDFConnector;

import com.google.gson.Gson;

import exceptions.DatastoreException;


/**
 * This servlet deals with interactions between the web interface for querying the database and the database
 * @author Luke Heavens & Ben Carr
 */
public class DatastoreServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Logger
	private static final Logger LOGGER = Logger.getLogger(DatastoreServlet.class.getName());
	private RDFConnector datastoreConn;
	
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
		datastoreConn = (RDFConnector) getServletConfig().getServletContext().getAttribute("rdfConnector");
		//TODO check attribute as in webservlet
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
    			//DatabaseConnector datastoreConn = new DatabaseConnector(); //TODO remove
    			datastoreConn.establishConnection();
    			
    			//Get user and id
    			user = datastoreConn.showUser(username);
    			long userId = Long.parseLong(user.get("userId"));
    			
    			//Run database queries and store results
    			retweetersOfUser = datastoreConn.getRetweetersOfUser(userId);
    			userRetweets = datastoreConn.getUserRetweets(userId);
    			userLocations = datastoreConn.getUserLocations(userId);
    			userKeywords = datastoreConn.getUserKeywords(userId);
    			
    			//Close database connection
    			datastoreConn.closeConnection();
    			
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
    			
    		} catch (DatastoreException ex) {
    			//An error occurred whilst accessing the database so catch and log
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); 
			    out.println(ex.getMessage());
			} 
    		
    	//If the request indicates a search of a venue then we fetch data concerning the venue entered
    	} else if (requestId.equals("showVenue")) {
    		try {
    			//Create data structures for results
    			HashMap<String, String> venue = new HashMap<String, String>();
    			List<HashMap<String, String>> users = new LinkedList<HashMap<String, String>>();
    			String venueName = request.getParameter("venueName");

    			//Open database connection
    			//DatabaseConnector datastoreConn = new DatabaseConnector(); //TODO remove
    			datastoreConn.establishConnection();
    			
    			//Get venue and id
    			venue = datastoreConn.showVenue(venueName);
    			String venueId = venue.get("locId");
    			
    			//Run database queries
    			users = datastoreConn.getVenueVisitors(venueId);
    			
    			//Close database connection
    			datastoreConn.closeConnection();
    			
    			//Store results for JASON
    			json = gson.toJson(venue);
    			json += "\n";
    			json += gson.toJson(users);
    	
    		} catch (DatastoreException ex) {
    			//An error occured whilst accessing the database so catch and log
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); 
			    out.println(ex.getMessage());
			}   		
    	}
    	
		//Output JSON
		out.print(json);
    	out.close();
	}
}

