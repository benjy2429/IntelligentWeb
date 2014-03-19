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
    	  	
    	if (requestId.equals("showUser")) {
    		try {
    			HashMap<String, String> user = new HashMap<String, String>();
    			List<HashMap<String, String>> retweeters = new LinkedList<HashMap<String, String>>();
    			String username = request.getParameter("username");

    			DatabaseConnector dbConn = new DatabaseConnector();
    			dbConn.establishConnection();
    			user = dbConn.showUser(username);
    			
    			long userId = Long.parseLong(user.get("userId"));
    			
    			retweeters = dbConn.getUserRetweets(userId);
    			dbConn.closeConnection();
    			
    			json = gson.toJson( user );
    			json += "\n";
    			json += gson.toJson( retweeters );
    			
    		} catch (SQLException e) {
				System.out.println(e.getMessage());
			}    		
    	}
    	
		out.print( json );
    	out.close();
	}

}
