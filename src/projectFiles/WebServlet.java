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
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * This servlet deals with interactions between the web interface and the social web
 * @author Luke Heavens & Ben Carr
 */
public class WebServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private StreamingQueries twitterStream = null;
	//Logger
	private static final Logger LOGGER = Logger.getLogger(WebServlet.class.getName());


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
		PrintWriter out = response.getWriter();		
		try {
			String requestId = request.getParameter("requestId");    	
			Gson gson = new Gson();
			String json;
			switch (requestId){
				case "tweetFormRequest" : json = tweetFormRequest(gson, request); break;
				case "retweetersForm" : json = retweetersForm(gson, request); break;
				case "keywordFormRequest" : json = keywordFormRequest(gson, request); break;
				case "checkinFormRequest" : json = checkinFormRequest(gson, request); break;
				case "venuesFormRequest" : json = venuesFormRequest(gson, request); break;
				case "fetchUserForProfile" : json = fetchUserForProfile(gson, request); break;
				case "fetchTweetsForProfile" : json = fetchTweetsForProfile(gson, request); break;
				case "getNearbyVenues" : json = getNearbyVenues(gson, request); break;  
				default : json = "Invalid POST call"; break;
			}
			out.print(json);
		} catch (FatalInternalException ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
		    out.println(ex.getMessage());
		}
		out.close();
	}

	private String tweetFormRequest(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
			if (twitterStream != null) {
				twitterStream.getTwitterStream().shutdown();
				twitterStream = null;
			}

			Queries query = new Queries(initTwitter());

			Double lat, lon, radius;
			try {
				lat = Double.parseDouble(request.getParameter("latTweet"));
				lon = Double.parseDouble(request.getParameter("lonTweet"));
				radius = Double.parseDouble(request.getParameter("radiusTweet"));
			} catch (NumberFormatException nfe) {
				System.out.println("WARNING: Invalid location parameters, performing query without geolocation data");
				lat = Double.NaN;
				lon = Double.NaN;
				radius = Double.NaN;
			}
			
			List<Status> result;
			
			try {
				result = query.getTrendingTweets(request.getParameter("queryTweet"), lat, lon, radius);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}
			
			//Have to parse ids as string and send them separately as twitter4j does not support the id_str parameter and javascript cannot handle type long
			ArrayList<String> tweetIds = new ArrayList<String>();


			for(Status status : result) {
				dbConn.addUsers(status.getUser());
				tweetIds.add(String.valueOf(status.getId()));
			}

			dbConn.closeConnection();
			
			String json = gson.toJson(tweetIds);
			json += "\n";
			json += gson.toJson(result);

			return json;
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}
	}


	private String retweetersForm(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
			
			Queries query = new Queries(initTwitter());
			long tweetId = Long.parseLong(request.getParameter("tweetId"));
			
			Status tweet;

			try {
				tweet = query.getTweetFromId(tweetId);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}

			List<User> retweeters;
			try {
				retweeters = query.getRetweeters(tweetId);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}
			
			dbConn.addUsers(tweet.getUser());
			long tweeterId = tweet.getUser().getId();
			for(User user : retweeters){
				dbConn.addUsers(user);
				dbConn.addContact(tweeterId,user.getId());
			}

			dbConn.closeConnection();
			
			return gson.toJson(retweeters);
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	private String keywordFormRequest(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			if (twitterStream != null) {
				twitterStream.getTwitterStream().shutdown();
				twitterStream = null;
			}

			Queries query = new Queries(initTwitter()); 
			LinkedList<String> usersNames = new LinkedList<String>(Arrays.asList(request.getParameter("usernamesKeyword").split(" ")));
			
			final LinkedList<User> users = new LinkedList<User>();
			try {
				users.addAll(query.getTwitterUsers(usersNames));
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}

			int keywords = Integer.parseInt(request.getParameter("keywordsKeyword"));
			int days = Integer.parseInt(request.getParameter("daysKeyword"));


			Pair<LinkedList<Term>, LinkedList<Term>> terms;
			try {
				terms = query.getDiscussedTopics(users, keywords, days);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}
			//TODO only recording top ten
			
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


			String json = gson.toJson(rankedTerms);		
			json += "\n";
			json += gson.toJson(users);
			return json;

		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}

	} 

	private String checkinFormRequest(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			String json = "";
			
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
	
			Queries query = new Queries(initTwitter(), initFoursquare());
			
			int days = 0;
			try {
				days = Integer.parseInt(request.getParameter("daysCheckin"));
			} catch (NumberFormatException nfe) {
				LOGGER.log(Level.WARNING, "Invalid days parameter, defaulting to 0 (live stream)");
			}
	
			User user;
			try {
				user = query.getTwitterUser(request.getParameter("usernameCheckin"));
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}
			
			dbConn.addUsers(user);
	
			// Send user if requested (Only needed first time for streaming)
			if (request.getParameter("userRequest").equals("1")) {
				json = gson.toJson(user);
				json += "\n";
			}
	
	
			if (days > 0) {
				if (twitterStream != null) {
					twitterStream.getTwitterStream().shutdown();
					twitterStream = null;
				}
	
				List<CompleteVenue> result;
				try {
					result = query.getUserVenues(user.getScreenName(), days);
				} catch (QueryException ex) {
					LOGGER.log(Level.SEVERE, ex.getMessage());
					throw new FatalInternalException("An internal error has occured whilst dealing with your query");
				}
				
				for(CompleteVenue venue : result){
					dbConn.addVenues(venue);
					dbConn.addUserVenue(user.getId(),venue.getId());
				}
				json += gson.toJson(result);
	
			} else if (days == 0) {
				if (twitterStream == null || twitterStream.isShutdown()) {    	    			
						// Open live stream
						System.out.println("Opening Twitter stream..");
						twitterStream = new StreamingQueries(initTwitterStream());
						twitterStream.addUserVenuesListener(user.getId());
						System.out.println("Twitter stream opened");
	
						// Get venues visited today
						List<CompleteVenue> result;
						try {
							result = query.getUserVenues(user.getScreenName(), days);
						} catch (QueryException ex) {
							LOGGER.log(Level.SEVERE, ex.getMessage());
							throw new FatalInternalException("An internal error has occured whilst dealing with your query");
						}
						
						for(CompleteVenue venue : result){
							dbConn.addVenues(venue);
							dbConn.addUserVenue(user.getId(),venue.getId());
						}
						
						json += gson.toJson(result);
						
				} else {
					List<Status> liveTweets = twitterStream.getTweets();
					List<CompleteVenue> result = new LinkedList<CompleteVenue>();
					for (Status tweet : liveTweets) {
						try {
							result.add(query.getVenueFromTweet(tweet));
						} catch (QueryException ex) {
							LOGGER.log(Level.SEVERE, ex.getMessage());
							throw new FatalInternalException("An internal error has occured whilst dealing with your query");
						}
					}
					for(CompleteVenue venue : result){
						dbConn.addVenues(venue);
						dbConn.addUserVenue(user.getId(),venue.getId());
					}
					json += gson.toJson(result);
					twitterStream.clearLists();
				}  
	
	
			} else {
				throw new Exception("Days must be greater or equal to zero");
			}
			
			dbConn.closeConnection();
			
			return json;
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	private String venuesFormRequest(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			String json = "";
			DatabaseConnector dbConn = new DatabaseConnector();
			dbConn.establishConnection(); 
	
			Queries query = new Queries(initTwitter(), initFoursquare());
	
			int days = 0;
			try {
				days = Integer.parseInt(request.getParameter("daysVenue"));
			} catch (NumberFormatException nfe) {
				LOGGER.log(Level.WARNING, "Invalid days parameter, defaulting to 0 (live stream)");
			}
	
			Double lat, lon, radius;
			String venueName = request.getParameter("venueNameVenue");
			try {
				lat = Double.parseDouble(request.getParameter("latVenue"));
				lon = Double.parseDouble(request.getParameter("lonVenue"));
				radius = Double.parseDouble(request.getParameter("radiusVenue"));
			} catch (NumberFormatException nfe) {
				LOGGER.log(Level.WARNING, "Invalid location parameters, performing query with venue name");
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
	
				try {
					query.getUsersAtVenue(venueName, lat, lon, radius, days, venues, venueTweets);
				} catch (QueryException ex) {
					LOGGER.log(Level.SEVERE, ex.getMessage());
					throw new FatalInternalException("An internal error has occured whilst dealing with your query");
				}
				
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
					twitterStream = new StreamingQueries(initTwitterStream());
					twitterStream.addUsersAtVenueListener(venueName, lat, lon, radius);
					System.out.println("Twitter stream opened");
	
					// Get venues visited today
					try {
						query.getUsersAtVenue(venueName, lat, lon, radius, days, venues, venueTweets);
					} catch (QueryException ex) {
						LOGGER.log(Level.SEVERE, ex.getMessage());
						throw new FatalInternalException("An internal error has occured whilst dealing with your query");
					}
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
					try {
						query.getUserVenuesFromTweets(liveTweets, venues, venueTweets);
					} catch (QueryException ex) {
						LOGGER.log(Level.SEVERE, ex.getMessage());
						throw new FatalInternalException("An internal error has occured whilst dealing with your query");
					}
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
	
			try {
				query.getNearbyPlaces(lat, lon, radius);
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}
	
			dbConn.closeConnection();
			
			return json;
			
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}		
	} 

	private String fetchUserForProfile(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			Queries query = new Queries(initTwitter()); 
			User user;
			try {
				user = query.getTwitterUser(request.getParameter("screenName"));
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}
			return gson.toJson(user);
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	private String fetchTweetsForProfile(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			Queries query = new Queries(initTwitter()); 
			List<Status> tweets;
			try {
				tweets = query.getUsersTweets(request.getParameter("screenName"));
			} catch (QueryException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
				throw new FatalInternalException("An internal error has occured whilst dealing with your query");
			}
			return gson.toJson(tweets);
		} catch (FatalInternalException ex) {
			throw ex;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}
	} 

	private String getNearbyVenues(Gson gson, HttpServletRequest request) throws FatalInternalException {
		try {
			Queries query = new Queries(initTwitter()); 

			double lat = Double.parseDouble(request.getParameter("lat"));
			double lon = Double.parseDouble(request.getParameter("lon"));
			double radius = Double.parseDouble(request.getParameter("radius"));

			List<Place> places = new LinkedList<Place>();
			try {
				places = query.getNearbyPlaces(lat, lon, radius);
			} catch (QueryException ex) {
				LOGGER.log(Level.WARNING, ex.getMessage());
			}
			return gson.toJson(places);
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
			throw new FatalInternalException("An internal error has occured");
		}
	}
}
