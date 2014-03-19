package projectFiles;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import projectFiles.Queries.Pair;
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
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
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
    		

    			for(Status status : result) {
        			dbConn.addUsers(status.getUser());
    				tweetIds.add(String.valueOf(status.getId()));
    			}
    			
    			
    			json = gson.toJson( tweetIds );
    			json += "\n";
    			json += gson.toJson( result );
    		} catch ( TwitterException te ) {
    			json = gson.toJson( te.getErrorMessage() );
    		}
    		dbConn.closeConnection();
    	} else if (requestId.equals("retweetersForm")){
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
    		try {
    			
    			Queries query = new Queries(initTwitter());
    			long tweetId = Long.parseLong( request.getParameter("tweetId") );
    			Status tweet = query.getTwitterFromId(tweetId);
    			List<User> retweeters = query.getRetweeters(tweetId);
    					
    			dbConn.addUsers(tweet.getUser());
    			long tweeterId = tweet.getUser().getId();
    			for(User user : retweeters){
    				dbConn.addUsers(user);
    				dbConn.addContact(tweeterId,user.getId());
    			}
    			
        		json = gson.toJson(retweeters);
    		} catch (TwitterException te) {
    			json = gson.toJson( te.getErrorMessage() );
    			System.out.println( te.getStatusCode() + te.getErrorMessage() + te.getStackTrace().toString());
    		} catch (NumberFormatException nfe) {
    			json = gson.toJson( nfe.getMessage() );
    		} catch (Exception e) {
    			json = gson.toJson( e.getMessage() );
    		}

    		dbConn.closeConnection();
    	} else if (requestId.equals("discussionForm")){

    		try {
    			if (twitterStream != null) {
    				twitterStream.getTwitterStream().shutdown();
    				twitterStream = null;
    			}
				
        		Queries query = new Queries(initTwitter()); 
				LinkedList<String> usersNames = new LinkedList<String>( Arrays.asList( request.getParameter("users").split(" ") ) );
				final LinkedList<User> users = new LinkedList<User>();
				users.addAll(query.getTwitterUsers(usersNames));
							
				int keywords = Integer.parseInt( request.getParameter("keywords") );
				int days = Integer.parseInt( request.getParameter("days") );
    						

				Pair<LinkedList<Term>, LinkedList<Term>> terms = query.getDiscussedTopics(users, keywords, days ); //TODO only recording top ten
				List<Term> rankedTerms = new LinkedList<Term>();
				rankedTerms.addAll(terms.t);
				
				final List<Term> allTerms = new LinkedList<Term>();
				allTerms.addAll(terms.t);
				allTerms.addAll(terms.u);
				
			    Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						System.out.println("Starting Thread");
						DatabaseConnector dbConn = new DatabaseConnector();
						dbConn.establishConnection(); 
		    			for(User user : users){
		    				dbConn.addUsers(user);
		    			}
		    			for(Term term : allTerms){
		    				int wordId = dbConn.addWord(term.term);
		    				if (wordId == -1){
		    					wordId = dbConn.getWordId(term.term);
		    				}
		    				if (wordId != -1){
			    				for(Pair<String, Integer> userCount : term.userCounts){
			    					long userId = -1;
			    	    			for(User user : users){
			    	    				if(user.getScreenName().equals(userCount.t)){
			    	    					userId = user.getId();
			    	    				}
			    	    			}
			    	    			if(userId != -1){
			    	    				dbConn.addUserTermPair(userId,wordId, userCount.u);
			    	    			} else {
			    	    				try {
											throw new Exception("A discrepancy with a user has occured.");
										} catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
			    	    			}
			    				}
		    				} else {
			    				try {
									throw new Exception("A discrepancy with a term has occured.");
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
		    				}
		    			}
		        		dbConn.closeConnection();
		        		System.out.println("Ending Thread");
					}
			    });
			    thread.start();

				
				json = gson.toJson(rankedTerms);		
				json += "\n";
				json += gson.toJson(users);
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
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
    		try { 
    			Queries query = new Queries( initTwitter(), initFoursquare() );
    			int days = 0;
    			try {
    				days = Integer.parseInt( request.getParameter("days") );
    			} catch ( NumberFormatException nfe ) {
    				System.out.println( "WARNING: Invalid days parameter, defaulting to 0 (live stream)" );
    			}
    			
    			User user = query.getTwitterUser( request.getParameter("username") ); 
    			dbConn.addUsers(user);
    			
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
	    			for(CompleteVenue venue : result){
	    				dbConn.addVenues(venue);
	    				dbConn.addUserVenue(user.getId(),venue.getId());
	    			}
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
	    	    			for(CompleteVenue venue : result){
	    	    				dbConn.addVenues(venue);
	    	    				dbConn.addUserVenue(user.getId(),venue.getId());
	    	    			}
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
		    			for(CompleteVenue venue : result){
		    				dbConn.addVenues(venue);
		    				dbConn.addUserVenue(user.getId(),venue.getId());
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
    		
    		dbConn.closeConnection();
  
    	} else if (requestId.equals("venuesForm")){
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
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
					for (Entry<String, CompleteVenue> entry : venues.entrySet()) {
					    dbConn.addVenues(entry.getValue());
					    for(Status tweet : venueTweets.get(entry.getKey())){
					    	dbConn.addUsers(tweet.getUser());
					    	dbConn.addUserVenue(tweet.getUser().getId(), entry.getKey());
					    }
					}
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
    					for (Entry<String, CompleteVenue> entry : venues.entrySet()) {
    					    dbConn.addVenues(entry.getValue());
    					    for(Status tweet : venueTweets.get(entry.getKey())){
    					    	dbConn.addUsers(tweet.getUser());
    					    	dbConn.addUserVenue(tweet.getUser().getId(), entry.getKey());
    					    }
    					}
    	    			json = gson.toJson(venues);
    	    			json += ("\n");
    	    			json += gson.toJson(venueTweets);

	    			} else {
	    				List<Status> liveTweets = twitterStream.getTweets();
	    				query.getUserVenuesFromTweets(liveTweets, venues, venueTweets);
						for (Entry<String, CompleteVenue> entry : venues.entrySet()) {
						    dbConn.addVenues(entry.getValue());
						    for(Status tweet : venueTweets.get(entry.getKey())){
						    	dbConn.addUsers(tweet.getUser());
						    	dbConn.addUserVenue(tweet.getUser().getId(), entry.getKey());
						    }
						}
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
			dbConn.closeConnection();
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
