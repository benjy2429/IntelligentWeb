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

/**
 * This servlet deals with interactions between the web interface for querying the datastore and the datastore
 * @author Luke Heavens & Ben Carr
 */
public class DatastoreServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Logger
	private static final Logger LOGGER = Logger.getLogger(DatastoreServlet.class.getName());
	private RDFConnector datastoreConn;
	
	/**
	 * When the servlet receives a get request, it delivers the datastoreInterface html file
	 * @param request - HttpServletRequest object
	 * @param response - HttpServletResponse object
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect("datastoreInterface.html");
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

    			//Open datastore connection
    			if (datastoreConn.establishConnection()) {
	    			//Get user and id
	    			user = datastoreConn.showUser(username);
	    			long userId = Long.parseLong(user.get("userId"));
	    			
	    			//Run datastore queries and store results
	    			retweetersOfUser = datastoreConn.getRetweetersOfUser(userId);
	    			userRetweets = datastoreConn.getUserRetweets(userId);
	    			userLocations = datastoreConn.getUserLocations(userId);
	    			userKeywords = datastoreConn.getUserKeywords(userId);
    			}
    			
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
    			
    		} catch (Exception ex) {
    			//An error occurred whilst communicating with the ddatastore so catch and log
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

    			//Open datastore connection
    			if (datastoreConn.establishConnection()) {
	    			//Get venue and id
	    			venue = datastoreConn.showVenue(venueName);
	    			String venueId = venue.get("venueId");
	    			
	    			//Run datastore queries
	    			users = datastoreConn.getVenueVisitors(venueId);
    			}
    			
    			//Store results for JASON
    			json = gson.toJson(venue);
    			json += "\n";
    			json += gson.toJson(users);
    	
    		} catch (Exception ex) {
    			//An error occured whilst communicating with the datastore so catch and log
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

