package servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import projectFiles.Pair;
import projectFiles.RDFConnector;
import projectFiles.Term;
import queries.Queries;
import queries.StreamingQueries;

import com.google.gson.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.*;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.VCARD;

import twitter4j.*;
import twitter4j.conf.*;
import exceptions.*;
import exceptions.QueryException;
import fi.foyt.foursquare.api.*;
import fi.foyt.foursquare.api.entities.*;

import com.claygregory.api.google.places.Place;


/**
 * This servlet deals with interactions between the web interface and the social web
 * @author Luke Heavens & Ben Carr
 */
public class WebServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private StreamingQueries twitterStream = null;
	//Logger
	private static final Logger LOGGER = Logger.getLogger(WebServlet.class.getName());
		
	private RDFConnector datastoreConn;
	
	

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
		FoursquareApi fs = new FoursquareApi(clientId, clientSecret, redirectUrl);
		fs.setoAuthToken("1R2QORAMBVJ3SS0MTCGG1FUROFW0MFPMKXB5HHOQUJLQ3JWL");
		return fs;
	}


	/**
	 * When the servlet receives a get request, it delivers the queryInterface html file
	 * @param request - HttpServletRequest object
	 * @param response - HttpServletResponse object
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect("queryInterface.html");
	}


	/**
	 * When the servlet receives a post request, it looks for and examines a requestId parameter. This determines the appropriate action to take.
	 * A JSON object is constructed which is communicated through the response containing relevant data
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response) 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		datastoreConn = (RDFConnector) getServletConfig().getServletContext().getAttribute("rdfConnector");
		//TODO some check to make sure that the attribute exists/ initialiased correctly
		// RDF TEST CODE //

		if (datastoreConn.establishConnection()) { 
			datastoreConn.test();
		}
		

        

	     
	     // ONTOLOGY TEST //
	     
	     String filePath = getServletConfig().getServletContext().getRealPath("") + "\\ontology.rdfs";
	     OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
	     ontology.read(new FileInputStream(filePath), Lang.RDFXML.getName());
	     ExtendedIterator<OntClass> clas = ontology.listClasses();
	     while (clas.hasNext()) {
	    	 System.out.println(clas.next().toString());
	     }
	     /*
	     Property sa = ontology.getProperty("streetAddress");
	     System.out.println(sa.getLocalName());
	     System.out.println(sa.getComment("EN"));
	     */
	     
	     
	     
        // ORIGINAL CODE BELOW //
        /*
		PrintWriter out = response.getWriter();		
		try {
			String requestId = request.getParameter("requestId");    	
			String json;
			switch (requestId){
				case "tweetFormRequest" : json = tweetFormRequest(request); break;
				case "retweetersForm" : json = retweetersForm(request); break;
				case "keywordFormRequest" : json = keywordFormRequest(request); break;
				case "checkinFormRequest" : json = checkinFormRequest(request); break;
				case "venuesFormRequest" : json = venuesFormRequest(request); break;
				case "fetchUserForProfile" : json = fetchUserForProfile(request); break;
				case "fetchTweetsForProfile" : json = fetchTweetsForProfile(request); break;
				case "getNearbyVenues" : json = getNearbyVenues(request); break;  
				default : json = "Invalid POST call"; break;
			}	    	
			out.print(json);
		} catch (FatalInternalException ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
		    out.println(ex.getMessage());
		}
		out.close();
		*/
	}

	
	/**
	 * Performs a twitter query using request data and returns tweets and their Ids in the form of a JSON string 
	 * Saves examined data in the system database in a background thread
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String tweetFormRequest(HttpServletRequest request) throws FatalInternalException {
		try {
			//Close any currently open twitter streams
			if (twitterStream != null) {
				twitterStream.getTwitterStream().shutdown();
				twitterStream = null;
			}
			
			//Create a new query object 
			Queries query = new Queries(initTwitter());
			
			//Collect location data if entered
			Double lat, lon, radius;
			lat = lon = radius = Double.NaN;
			if (request.getParameter("enableLocTweet") != null) {
				try {
					lat = Double.parseDouble(request.getParameter("latTweet"));
					lon = Double.parseDouble(request.getParameter("lonTweet"));
					radius = Double.parseDouble(request.getParameter("radiusTweet"));
					//Check if the radius is valid 
					if (radius <= 0) {
						radius = 1.0;
						LOGGER.log(Level.WARNING, "Invalid radius! Defaulting to 1km");
					}
				} catch (NumberFormatException nfe) {
					LOGGER.log(Level.WARNING, "Invalid location parameters, performing query without geolocation data");
				}
			}
			
			//Perform relevant query
			final List<Status> result;
			try {
				result = query.getTrendingTweets(request.getParameter("queryTweet"), lat, lon, radius);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				throw new FatalInternalException("An internal error has occured whilst dealing with a query");
			}
			
			//Have to extract and parse tweetIds as string in order to send separately as twitter4j does not support the id_str parameter and javascript cannot handle type long
			ArrayList<String> tweetIds = new ArrayList<String>();
			for(Status status : result) {
				tweetIds.add(String.valueOf(status.getId()));
			}
			
			//Create a new thread to store data in the database in the background without delaying response from webpage
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					LOGGER.log(Level.FINE, "Starting background thread for database storage");
					if (datastoreConn.establishConnection()) { 
						for(Status status : result) {
							datastoreConn.addUsers(status.getUser());
						}
						datastoreConn.closeConnection();
					}
					LOGGER.log(Level.FINE, "Ending background thread for database storage");
				}
			});
			thread.start();
			
			//Generate a JSON string using gson and the data collected
			Gson gson = new Gson();
			String json = gson.toJson(tweetIds);
			json += "\n";
			json += gson.toJson(result);
			return json;
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}
	}


	/**
	 * Gets a tweet from a tweetId and then finds a list of users who retweeted it in the form of a JSON string
	 * Saves examined data in the system database in a background thread
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String retweetersForm(HttpServletRequest request) throws FatalInternalException {
		try {
			//Close any currently open twitter streams
			if (twitterStream != null) {
				twitterStream.getTwitterStream().shutdown();
				twitterStream = null;
			}
			
			//Create a new query object 
			Queries query = new Queries(initTwitter());
			
			//Obtain the tweet id from the request parameters
			long tweetId = Long.parseLong(request.getParameter("tweetId"));
			
			//Get the tweeter and retweeters from tweet id 
			final User tweeter;
			final List<User> retweeters;
			try {
				tweeter = query.getTweetFromId(tweetId).getUser();
				retweeters = query.getRetweeters(tweetId);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				throw new FatalInternalException("An internal error has occured whilst dealing with a query");
			}
			
			//Create a new thread to store data in the database in the background without delaying response from web page
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					LOGGER.log(Level.FINE, "Starting background thread for database storage");
					if(datastoreConn.establishConnection()) { 			
						datastoreConn.addUsers(tweeter);
						for(User user : retweeters){
							datastoreConn.addUsers(user);
							datastoreConn.addContact(tweeter.getId(), user.getId());
						}
						datastoreConn.closeConnection();
					}
					LOGGER.log(Level.FINE, "Ending background thread for database storage");
				}
			});
			thread.start();
		
			//Generate a JSON string using gson and the data collected
			Gson gson = new Gson();
			return gson.toJson(retweeters);
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	
	/**
	 * Gets frequently used terms amongst several users in the form of a JSON string
	 * Saves examined data in the system database in a background thread
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String keywordFormRequest(HttpServletRequest request) throws FatalInternalException {
		try {
			//Close any currently open twitter streams
			if (twitterStream != null) {
				twitterStream.getTwitterStream().shutdown();
				twitterStream = null;
			}
			
			//Create a new query object 
			Queries query = new Queries(initTwitter()); 

			//Create a list of individual usernames the number of keywords to obtain and the maximum number of days in the past to search over
			List<String> usersNames = new LinkedList<String>(Arrays.asList(request.getParameter("usernamesKeyword").split(" ")));
			int keywords = Integer.parseInt(request.getParameter("keywordsKeyword"));
			int days = Integer.parseInt(request.getParameter("daysKeyword"));
			
			// Validation
			if (usersNames.size() > 10) {
				usersNames = usersNames.subList(0,9);
				LOGGER.log(Level.WARNING, "Too many usernames! Only searching with first 10");
			}
			if (keywords <= 0 || keywords >= 100) {
				keywords = 10;
				LOGGER.log(Level.WARNING, "Keywords invalid! Defaulting to 10 keywords");
			}
			if (days <= 0) {
				days = 1;
				LOGGER.log(Level.WARNING, "Days <= 0! Defaulting to 1 day");
			}
			
			//Obtain users from the usernames given, then find terms used frequently between them
			final LinkedList<User> users = new LinkedList<User>();
			Pair<LinkedList<Term>, LinkedList<Term>> terms;
			try {
				users.addAll(query.getTwitterUsers(usersNames));
				terms = query.getDiscussedTopics(users, keywords, days);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				throw new FatalInternalException("An internal error has occured whilst dealing with a query");
			}
	
			//Get the list of ranked terms to return through JSON string
			List<Term> rankedTerms = new LinkedList<Term>();
			rankedTerms.addAll(terms.t);

			//Get the list of all terms used (ranked and unranked) for storage in the database
			final List<Term> allTerms = new LinkedList<Term>();
			allTerms.addAll(terms.t);
			allTerms.addAll(terms.u);

			//Create a new thread to store data in the database in the background without delaying response from web page
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					LOGGER.log(Level.FINE, "Starting background thread for term database storage");
					if (datastoreConn.establishConnection()) { 
						//Add users to database
						for(User user : users){
							datastoreConn.addUsers(user);
						}
						for(Term term : allTerms){
							//Add terms to database and get id
							int wordId = datastoreConn.addWord(term.term);
							
							//If already exists, then look it up
							if (wordId == -1) {wordId = datastoreConn.getWordId(term.term);}
							
							//If we still don't have an id then something has gone wrong
							if (wordId != -1){
								//Add the pairings of user to word in the database
								for(Pair<Long, Integer> userCount : term.userCounts){
									//datastoreConn.addUserTermPair(userCount.t, wordId, userCount.u);
									//TODO fix
								}
							} else {
								LOGGER.log(Level.WARNING, "A term exists in the database but its id could not be obtained. Term: " + term.term);
							}
						}
						datastoreConn.closeConnection();
					}
					LOGGER.log(Level.FINE, "Ending background thread for term database storage");
				}
			});
			thread.start();

			//Generate a JSON string using gson and the data collected
			Gson gson = new Gson();
			String json = gson.toJson(rankedTerms);		
			json += "\n";
			json += gson.toJson(users);
			return json;

		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	
	/**
	 * Gets venues visited by a user over a number of days and return in the form of a JSON string
	 * The user is also returned if it is the first request
	 * Saves examined data in the system database in a background thread
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String checkinFormRequest(HttpServletRequest request) throws FatalInternalException {
		try {
			//Close any currently open twitter streams if necessary
			if (twitterStream != null && Boolean.parseBoolean(request.getParameter("shutdownStream"))) {
				twitterStream.getTwitterStream().shutdown();
				twitterStream = null;
			}
			
			//Create a new query object 
			Queries query = new Queries(initTwitter(), initFoursquare());
			
			//Over how many days should be looked
			int days = 0;
			try {
				days = Integer.parseInt(request.getParameter("daysCheckin"));
				//Validation
				if (days < 0) {
					LOGGER.log(Level.WARNING, "Invalid days parameter! Defaulting to 0 (live stream)");
				}
			} catch (NumberFormatException nfe) {
				LOGGER.log(Level.WARNING, "Invalid days parameter! Defaulting to 0 (live stream)");
			}
				
			//Get the user object from the passed username
			final User user;
			try {
				user = query.getTwitterUser(request.getParameter("usernameCheckin"));
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				throw new FatalInternalException("An internal error has occured whilst dealing with a query");
			}
			
			//Get list of venues
			final List<CompleteVenue> result;
			if (days > 0) {
				//If days is >0 then not a live stream 
				if (twitterStream != null) {
					twitterStream.getTwitterStream().shutdown();
					twitterStream = null;
				}
	
				//Perform query to obtain venues
				try {
					result = query.getUserVenues(user.getScreenName(), days);
				} catch (QueryException ex) {
					LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
					throw new FatalInternalException("An internal error has occured whilst dealing with a query");
				}
			
			} else {
				//Output warning if defaulted
				if(days < 0) {LOGGER.log(Level.WARNING, "Days must be greater or equal to zero, defaulting to 0 (live stream)");}
				
				//A live stream should be used, first check if a stream is already opened
				if (twitterStream == null || twitterStream.isShutdown()) {    	    			
						//Live stream not open so open live stream
						LOGGER.log(Level.FINE, "Opening Twitter stream..");
						twitterStream = new StreamingQueries(initTwitterStream());
						twitterStream.addUserVenuesListener(user.getId());
						LOGGER.log(Level.FINE, "Twitter stream opened");
	
						//Perform query to obtain venues visited today
						try {
							result = query.getUserVenues(user.getScreenName(), days);
						} catch (QueryException ex) {
							LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
							throw new FatalInternalException("An internal error has occured whilst dealing with a query");
						}
				} else {
					//Live stream is already opened so obtain tweets collected
					List<Status> liveTweets = twitterStream.getTweets();
					
					//Perform query to obtain venues visited today
					result = new LinkedList<CompleteVenue>();
					for (Status tweet : liveTweets) {
						try {
							result.add(query.getVenueFromTweet(tweet));
						} catch (QueryException ex) {
							LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
							throw new FatalInternalException("An internal error has occured whilst dealing with a query");
						}
					}
					
					//Clear recorded tweets, ready for new tweets to be recorded
					twitterStream.clearLists();
				} 
			}
			
			//Determine whether this is the first time this request has been made
			final boolean firstTime = request.getParameter("userRequest").equals("1");
			
			//Create a new thread to store data in the database in the background without delaying response from web page
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					LOGGER.log(Level.FINE, "Starting background thread for term database storage");
					if (datastoreConn.establishConnection()) { 
						//Add users to database
						if (firstTime) {
							datastoreConn.addUsers(user);
						}
						//Add venues to database
						for(CompleteVenue venue : result){
							datastoreConn.addVenues(venue);
							datastoreConn.addUserVenue(user.getId(),venue.getId());
						}
						datastoreConn.closeConnection();
					}
					LOGGER.log(Level.FINE, "Ending background thread for term database storage");
				}
			});
			thread.start();
			
			//Generate a JSON string using gson and the data collected
			String json = "";
			Gson gson = new Gson();
			
			// Send user if requested (Only needed first time for streaming)
			if (firstTime) {
				json = gson.toJson(user);
				json += "\n";
			}
			
			json += gson.toJson(result);
			
			return json;
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	
	/**
	 * Gets venues that match the conditions of the search and tweets from people who have checked into these venues in the form of a JSON string
	 * Saves examined data in the system database in a background thread
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String venuesFormRequest(HttpServletRequest request) throws FatalInternalException {
		try {
			//Close any currently open twitter streams if necessary
			if (twitterStream != null && Boolean.parseBoolean(request.getParameter("shutdownStream"))) {
				twitterStream.getTwitterStream().shutdown();
				twitterStream = null;
			}
			
			//Create a new query object 
			Queries query = new Queries(initTwitter(), initFoursquare());
	
			//Over how many days should be looked
			int days = 0;
			try {
				days = Integer.parseInt(request.getParameter("daysVenue"));
				if (days < 0) {
					LOGGER.log(Level.WARNING, "Invalid days parameter, defaulting to 0 (live stream)");
				}
			} catch (NumberFormatException nfe) {
				LOGGER.log(Level.WARNING, "Invalid days parameter, defaulting to 0 (live stream)");
			}
	
			String venueName = request.getParameter("venueNameVenue");
			
			//Get location parameters
			Double lat, lon, radius;
			lat = lon = radius = Double.NaN;
			if (request.getParameter("enableLocVenue") != null) {
				try {
					lat = Double.parseDouble(request.getParameter("latVenue"));
					lon = Double.parseDouble(request.getParameter("lonVenue"));
					radius = Double.parseDouble(request.getParameter("radiusVenue"));
					//Check if the radius is valid 
					if (radius <= 0) {
						radius = 1.0;
						LOGGER.log(Level.WARNING, "Invalid radius! Defaulting to 1km");
					}
				} catch (NumberFormatException nfe) {
					LOGGER.log(Level.WARNING, "Invalid location parameters, performing query with venue name");
				}
			}
			
			
			//Create data structures fo rstoring relevant information
			final Map<String, CompleteVenue> venues = new HashMap<String,CompleteVenue>();
			final Map<String,List<Status>> venueTweets = new HashMap<String, List<Status>>();
	
			
			if (days > 0) {
				//If days is >0 then not a live stream
				if (twitterStream != null) {
					twitterStream.getTwitterStream().shutdown();
					twitterStream = null;
				}
	
				//Perform query to see which users have visited a venue 
				try {
					query.getUsersAtVenue(venueName, lat, lon, radius, days, venues, venueTweets);
				} catch (QueryException ex) {
					LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
					throw new FatalInternalException("An internal error has occured whilst dealing with a query");
				}
				
			} else {
				//A live stream should be used, first check if a stream is already opened
				if (twitterStream == null || twitterStream.isShutdown()) {
					//Live stream not open so open live stream
					LOGGER.log(Level.FINE, "Opening Twitter stream..");
					twitterStream = new StreamingQueries(initTwitterStream());
					twitterStream.addUsersAtVenueListener(venueName, lat, lon, radius);
					LOGGER.log(Level.FINE, "Twitter stream opened");

	
					//Get users who have visited the venue today
					try {
						query.getUsersAtVenue(venueName, lat, lon, radius, days, venues, venueTweets);
					} catch (QueryException ex) {
						LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
						throw new FatalInternalException("An internal error has occured whilst dealing with a query");
					}

	
				} else {
					//Live stream is already opened so obtain tweets collected
					List<Status> liveTweets = new LinkedList<Status>();
					liveTweets.addAll(twitterStream.getTweets());
					//Clear list of recorded tweets ready for new tweets
					twitterStream.clearLists();
					
					//Perform query to get user venues form tweets
					try {
						query.getUserVenuesFromTweets(liveTweets, venueName, venues, venueTweets);
					} catch (QueryException ex) {
						LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
						throw new FatalInternalException("An internal error has occured whilst dealing with a query");
					}
				}  
			} 
	
			//Create a new thread to store data in the database in the background without delaying response from web page
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					LOGGER.log(Level.FINE, "Starting background thread for term database storage");
					if (datastoreConn.establishConnection()) { 
						for (Entry<String, CompleteVenue> entry : venues.entrySet()) {
							datastoreConn.addVenues(entry.getValue());
							for(Status tweet : venueTweets.get(entry.getKey())){
								datastoreConn.addUsers(tweet.getUser());
								datastoreConn.addUserVenue(tweet.getUser().getId(), entry.getKey());
							}
						}
						datastoreConn.closeConnection();
					}
					LOGGER.log(Level.FINE, "Ending background thread for term database storage");
				}
			});
			thread.start();
			
			//Generate a JSON string using gson and the data collected
			String json = "";
			Gson gson = new Gson();
			
			json = gson.toJson(venues);
			json += ("\n");
			json += gson.toJson(venueTweets);
			
			return json;
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}		
	} 

	
	/**
	 * Fetches user information from a given username in the form of a JSON string
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String fetchUserForProfile(HttpServletRequest request) throws FatalInternalException {
		try {
			//Create a new query object
			Queries query = new Queries(initTwitter()); 
			
			//Perform query to get user from screen name
			User user;
			try {
				user = query.getTwitterUser(request.getParameter("screenName"));
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				throw new FatalInternalException("An internal error has occured whilst dealing with a query");
			}
			
			//Generate a JSON string using gson and the data collected
			Gson gson = new Gson();
			return gson.toJson(user);
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	
	/**
	 * Fetches tweets made by a user in the form of a JSON string
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String fetchTweetsForProfile(HttpServletRequest request) throws FatalInternalException {
		try {
			//Create a new query object
			Queries query = new Queries(initTwitter()); 
			
			//Perform a query to get tweets from a username
			List<Status> tweets;
			try {
				tweets = query.getUsersTweets(request.getParameter("screenName"));
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				throw new FatalInternalException("An internal error has occured whilst dealing with a query");
			}
			
			//Generate a JSON string using gson and the data collected
			Gson gson = new Gson();
			return gson.toJson(tweets);
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	/**
	 * Gets a list of nearby places from coordinates provided in the request object. Returns in the form of a JSON string
	 * @param request - The HttpServletRequest object
	 * @return - JSON string
	 * @throws FatalInternalException
	 */
	private String getNearbyVenues(HttpServletRequest request) throws FatalInternalException {
		try {
			//Create a new query object
			Queries query = new Queries(initTwitter()); 

			//Obtain location information
			double lat = Double.parseDouble(request.getParameter("lat"));
			double lon = Double.parseDouble(request.getParameter("lon"));
			double radius = Double.parseDouble(request.getParameter("radius"));

			//Perform a query to obtain a list of places 
			List<Place> places = new LinkedList<Place>();
			try {
				places = query.getNearbyPlaces(lat, lon, radius);
			} catch (QueryException ex) {
				LOGGER.log(Level.WARNING, ex.getMessage());
			}
			
			//Generate a JSON string using gson and the data collected
			Gson gson = new Gson();
			return gson.toJson(places);
			
		} catch (Exception ex) {
			//If any other errors occur, log it and throw a fatal internal exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new FatalInternalException("An internal error has occured");
		}
	}
}
