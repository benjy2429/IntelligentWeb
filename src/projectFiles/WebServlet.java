package projectFiles;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import projectFiles.Queries.Term;
import com.google.gson.*;
import twitter4j.*;
import twitter4j.conf.*;
import exceptions.*;
import fi.foyt.foursquare.api.*;
import fi.foyt.foursquare.api.entities.*;
import com.claygregory.api.google.places.Place;


/**
 * Servlet implementation class Queries
 */
public class WebServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private StreamingQueries twitterStream = null;
       
	/** 
	 * confTwitter provides the oAuth configuration settings for a Twitter connection
	 * @return ConfigurationBuilder
	 */
	private ConfigurationBuilder confTwitter() {
		String consumerKey = "VyRPGrHFjTPxJlDuB0HkA";
		String consumerSecret = "DXTnfQbFupg9HQS1PQQwx09u43nZu2zbKH3ruFbuoc";
		String accessToken = "2365733802-3yvuA54Jpbfik236KWfJaKwfJcpzAuALWAoQBF3";
		String accessTokenSecret = "MMfdUr2zuBeXxeoRf1GFcsj2pOxYfXdhheogV34D6xrbt";
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey(consumerKey)
		.setOAuthConsumerSecret(consumerSecret)
		.setOAuthAccessToken(accessToken)
		.setOAuthAccessTokenSecret(accessTokenSecret)
		.setJSONStoreEnabled(true);	
		return cb;
	}
    
	/** 
	 * initTwitter provides a TwitterAPI connection
	 * @return TwitterFactory
	 */
	private Twitter initTwitter() throws TwitterException{
		return (new TwitterFactory(confTwitter().build()).getInstance());
	}
	
	/**
	 * initTwitterStream provides a TwitterStreamAPI connection
	 * @return TwitterStreamFactory
	 * @throws Exception
	 */
	private TwitterStream initTwitterStream() throws TwitterException {
		return (new TwitterStreamFactory(confTwitter().build()).getInstance());
	}
	
	/**
	 * initFoursquare provides a connection to Foursquare
	 * @return FoursquareApi
	 * @throws Exception
	 */
	private FoursquareApi initFoursquare() throws FoursquareApiException {
		String clientId = "4QG35KZ2EGKKO4QD3F5TJ1UPA13Y5FZUNK1HLKKAXTLG52KU";
		String clientSecret = "WVKWQA031AG1USNFX0I5WULNQ2X3ZPHX0BEG52KGJCIBDU1F";
		String redirectUrl = "http://lukeheavens.co.uk/"; 
		FoursquareApi fs = new FoursquareApi( clientId, clientSecret, redirectUrl );
		fs.setoAuthToken( "1R2QORAMBVJ3SS0MTCGG1FUROFW0MFPMKXB5HHOQUJLQ3JWL" );
		return fs;
	}


	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect("queryInterface.html");
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();		
    	String requestId = request.getParameter("requestId");    	
    	Gson gson = new Gson();
    	String json = "";
    	
    	if(requestId.equals("topicForm")){
    		try {
    			if (twitterStream != null) {
    				twitterStream.getTwitterStream().shutdown();
    				twitterStream = null;
    			}
    			
    			Queries query = new Queries( initTwitter() );

    			Double lat, lon, radius;
    			try {
    				lat = Double.parseDouble( request.getParameter("lat") );
    				lon = Double.parseDouble( request.getParameter("lon") );
    				radius = Double.parseDouble( request.getParameter("radius") );
    			} catch ( NumberFormatException nfe ) {
    				System.out.println( "WARNING: Invalid location parameters, performing query without geolocation data" );
    				lat = Double.NaN;
    				lon = Double.NaN;
    				radius = Double.NaN;
    			}

    			List<Status> result = query.getTrendingTweets( request.getParameter("query"), lat, lon, radius );
    			//Have to parse ids as string and send them separately as twitter4j does not support the id_str parameter and javascript cannot handle type long
    			ArrayList<String> tweetIds = new ArrayList<String>();
    			
    			DatabaseConnector dbConn = new DatabaseConnector();
    			dbConn.establishConnection();

    			for(Status status : result) {
        			dbConn.addUsers(status.getUser());
    				tweetIds.add(String.valueOf(status.getId()));
    			}
    			
    			dbConn.closeConnection();
    			
    			json = gson.toJson( tweetIds );
    			json += "\n";
    			json += gson.toJson( result );
    		} catch ( TwitterException te ) {
    			json = gson.toJson( te.getErrorMessage() );
    		}
    		
    	} else if (requestId.equals("retweetersForm")){
    		try {
    			
    			Queries query = new Queries(initTwitter());
    			long tweetId = Long.parseLong( request.getParameter("tweetId") );
    			Status tweet = query.getTwitterFromId(tweetId);
    			List<User> retweeters = query.getRetweeters(tweetId);
    			
    			DatabaseConnector dbConn = new DatabaseConnector();
    			dbConn.establishConnection();   			
    			dbConn.addUsers(tweet.getUser());
    			long tweeterId = tweet.getUser().getId();
    			for(User user : retweeters){
    				dbConn.addUsers(user);
    				dbConn.addContact(tweeterId,user.getId());
    			}
    			dbConn.closeConnection();
    			
        		json = gson.toJson(retweeters);
    		} catch (TwitterException te) {
    			json = gson.toJson( te.getErrorMessage() );
    			System.out.println( te.getStatusCode() + te.getErrorMessage() + te.getStackTrace().toString());
    		} catch (NumberFormatException nfe) {
    			json = gson.toJson( nfe.getMessage() );
    		} catch (Exception e) {
    			json = gson.toJson( e.getMessage() );
    		}

    		
    	} else if (requestId.equals("discussionForm")){
			try {
    			if (twitterStream != null) {
    				twitterStream.getTwitterStream().shutdown();
    				twitterStream = null;
    			}
				
        		Queries query = new Queries(initTwitter()); 
				LinkedList<String> users = new LinkedList<String>( Arrays.asList( request.getParameter("users").split(" ") ) );
				
				int keywords = Integer.parseInt( request.getParameter("keywords") );
				int days = Integer.parseInt( request.getParameter("days") );
    						
				//users.add("Science_Factoid");
				//users.add("CathalSheridan");
				//users.add("twalsh92");
				//users.add("Sarah_Falconer");
				//users.add("Jonomnom");
				//users.add("thomasdeanwhite");
				//users.add("chloe100893");
				//users.add("luke_heavens");
				
				//users.add("BBCBreaking");
				//users.add("Channel4News");
				//users.add("itvnews");

				List<Term> frequentTerms;
				frequentTerms = query.getDiscussedTopics( users, keywords, days );
				json = gson.toJson(frequentTerms);		
				json += "\n";
				json += gson.toJson(query.getTwitterUsers(users));
			} catch (TwitterException e) {
				json = gson.toJson("Error communicating with Twitter");
				e.printStackTrace();
			} catch (FileException e){
				json = gson.toJson("Error accessing files on the server");
				e.printStackTrace();
			} catch ( NumberFormatException nfe ) {
				json = gson.toJson("Error, keywords and days must be integers");
			}
			
    	} else if (requestId.equals("userVenueForm")){
    		try {
    			Queries query = new Queries( initTwitter(), initFoursquare() );
    			int days = 0;
    			try {
    				days = Integer.parseInt( request.getParameter("days") );
    			} catch ( NumberFormatException nfe ) {
    				System.out.println( "WARNING: Invalid days parameter, defaulting to 0 (live stream)" );
    			}
    			
    			User user = query.getTwitterUser( request.getParameter("username") ); 
    			
    			// Send user if requested (Only needed first time for streaming)
    			if ( request.getParameter("userRequest").equals("1") ) {
	    			json = gson.toJson( user );
	    			json += "\n";
    			}
    			
    			
    			if (days > 0) {
        			if (twitterStream != null) {
        				twitterStream.getTwitterStream().shutdown();
        				twitterStream = null;
        			}
        			
	    			List<CompleteVenue> result = query.getUserVenues( user.getScreenName(), days );
	    			json += gson.toJson( result );
	    			
    			} else if (days == 0) {
	    			if (twitterStream == null || twitterStream.isShutdown()) {
	    				try {	    	    			
	    	    			// Open live stream
	    					System.out.println("Opening Twitter stream..");
	    					twitterStream = new StreamingQueries( initTwitterStream() );
	    					twitterStream.addUserVenuesListener( user.getId() );
	    					System.out.println("Twitter stream opened");
	    					
	    					// Get venues visited today
	    	    			List<CompleteVenue> result = query.getUserVenues( user.getScreenName(), days );
	    	    			json += gson.toJson( result );

	    				} catch (TwitterException e) {
	    					e.printStackTrace();
	    				}
	    			} else {
	    				List<Status> liveTweets = twitterStream.getTweets();
	    				List<CompleteVenue> result = new LinkedList<CompleteVenue>();
	    				for (Status tweet : liveTweets) {
	    					result.add( query.getVenueFromTweet(tweet) );
	    				}
		    			json += gson.toJson( result );
	    				twitterStream.clearLists();
	    			}  
    			
    			
    			} else {
     				throw new Exception("Days must be greater or equal to zero");
     			}
		
	    			
    		} catch ( TwitterException te ) {
    			json = gson.toJson(te.getErrorMessage() );
    		} catch ( FoursquareApiException fse ) {
    			json = gson.toJson(fse.getMessage() );
    		} catch ( Exception e ) {
    			json = gson.toJson(e.getMessage() );
    		}

    	} else if (requestId.equals("venuesForm")){
			try {




    			
				Queries query = new Queries( initTwitter(), initFoursquare() );
				
				int days = 0;
				try {
					days = Integer.parseInt( request.getParameter("days") );
				} catch ( NumberFormatException nfe ) {
					System.out.println( "WARNING: Invalid days parameter, defaulting to 0 (live stream)" );
				}
				
    			Double lat, lon, radius;
    			String venueName = request.getParameter("venueName");
    			try {
    				lat = Double.parseDouble( request.getParameter("lat") );
    				lon = Double.parseDouble( request.getParameter("lon") );
    				radius = Double.parseDouble( request.getParameter("radius") );
    			} catch ( NumberFormatException nfe ) {
    				System.out.println( "WARNING: Invalid location parameters, performing query with venue name" );
    				lat = Double.NaN;
    				lon = Double.NaN;
    				radius = Double.NaN;
    			}
				Map<String, CompleteVenue> venues= new HashMap<String,CompleteVenue>();
				Map<String,List<Status>> venueTweets= new HashMap<String, List<Status>>();

				
				if (days > 0) {
        			if (twitterStream != null) {
        				twitterStream.getTwitterStream().shutdown();
        				twitterStream = null;
        			}
        			
					query.getUsersAtVenue(venueName, lat, lon, radius, days, venues, venueTweets);
					
	    			json = gson.toJson(venues);
	    			json += ("\n");
	    			json += gson.toJson(venueTweets);
	    			
				} else if (days == 0) {
	    			if (twitterStream == null || twitterStream.isShutdown()) {
    	    			// Open live stream
    					System.out.println("Opening Twitter stream..");
    					twitterStream = new StreamingQueries( initTwitterStream() );
    					twitterStream.addUsersAtVenueListener( venueName, lat, lon, radius );
    					System.out.println("Twitter stream opened");
    					
    					// Get venues visited today
    	    			query.getUsersAtVenue(venueName, lat, lon, radius, days, venues, venueTweets);
    	    			json = gson.toJson(venues);
    	    			json += ("\n");
    	    			json += gson.toJson(venueTweets);

	    			} else {
	    				List<Status> liveTweets = twitterStream.getTweets();
	    				query.getUserVenuesAndTweets(liveTweets, venues, venueTweets);
    	    			json = gson.toJson(venues);
    	    			json += ("\n");
    	    			json += gson.toJson(venueTweets);
	    				twitterStream.clearLists();
	    			}  
    			
    			
    			} else {
     				throw new Exception("Days must be greater or equal to zero");
     			}

				query.getNearbyPlaces(lat, lon, radius);
				
			} catch (TwitterException te) {
				json = gson.toJson(te.getErrorMessage());
			} catch (FoursquareApiException e) {
				json = gson.toJson(e.getMessage());
			} catch (Exception e) {
				json = gson.toJson(e.getMessage());
			}
    		
    	} else if (requestId.equals("fetchUserForProfile")){
			try {
	    		Queries query = new Queries(initTwitter()); 
				User user = query.getTwitterUser(request.getParameter("screenName"));
				json = gson.toJson( user );
    		} catch ( TwitterException te ) {
    			json = gson.toJson(te.getErrorMessage() );
			}
    	} else if (requestId.equals("fetchTweetsForProfile")){
			try {
	    		Queries query = new Queries(initTwitter()); 
				List<Status> tweets = query.getUsersTweets(request.getParameter("screenName"));
				json = gson.toJson(tweets);
    		} catch ( TwitterException te ) {
    			json = gson.toJson(te.getErrorMessage() );
			}
    	} else if (requestId.equals("getNearbyVenues")){
			try {
	    		Queries query = new Queries(initTwitter()); 
	    		
				double lat = Double.parseDouble( request.getParameter("lat") );
				double lon = Double.parseDouble( request.getParameter("lon") );
				double radius = Double.parseDouble( request.getParameter("radius") );
    			
				List<Place> places = query.getNearbyPlaces(lat, lon, radius);
				json = gson.toJson(places);
    		} catch ( TwitterException te ) {
    			json = gson.toJson(te.getErrorMessage());
			} catch ( NumberFormatException nfe ) {
				json = gson.toJson(nfe.getMessage());
			}
    	}
    	
		out.print( json );
    	out.close();
	}

}
