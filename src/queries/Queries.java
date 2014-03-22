package queries;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import projectFiles.Pair;
import projectFiles.StopList;
import projectFiles.Term;

import com.claygregory.api.google.places.*;
import com.claygregory.api.google.places.Place;

import exceptions.InvalidFoursquareUrlException;
import exceptions.QueryException;
import fi.foyt.foursquare.api.*;
import fi.foyt.foursquare.api.entities.*;
import twitter4j.*;


/**
 * This file provides methods for collecting and processing data through queries from the social web
 * @author Luke Heavens & Ben Carr
 */
public class Queries {
	//Social web objects
	private Twitter twitter;
	private FoursquareApi foursquare;
	//Logger
	private static final Logger LOGGER = Logger.getLogger(Queries.class.getName());
	
	
	/**
	 * Queries Constructor for a Twitter connection
	 * @param twitter
	 */
	public Queries(Twitter twitter){
		this.twitter = twitter;
	}
	
	
	/**
	 * Queries Constructor for Twitter and Foursquare connections
	 * @param twitter
	 * @param foursquare
	 */
	public Queries(Twitter twitter, FoursquareApi foursquare) {
		this.twitter = twitter;
		this.foursquare = foursquare;
	}
	
	
	/**
	 * getTwitterUser finds a user by their Twitter screen name and returns their user object
	 * @param username - The user's Twitter screen name
	 * @return A user object for the given username
	 * @throws QueryException
	 */
	public User getTwitterUser(String username) throws QueryException {
		try {
			return twitter.showUser(username);
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting twitter user from username");
		}
	}

	
	/**
	 * getTwitterUsers finds multiple Twitter users by a list of Twitter screen names 
	 * @param users - A List of Twitter screen names
	 * @return A list of Twitter user objects for the given usernames
	 * @throws QueryException
	 */
	public List<User> getTwitterUsers(List<String> users) throws QueryException {
		try {
			String[] userNamesArray = new String[users.size()];
			return twitter.lookupUsers(users.toArray(userNamesArray));
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting twitter users from a list of usernames");
		}
	}
	
	
	/**
	 * This function takes a tweetId and returns the tweet (status) object
	 * @param tweetId - The id of a tweet
	 * @return - The tweet (status) object
	 * @throws QueryException
	 */
	public Status getTweetFromId(long tweetId) throws QueryException {
		try {
			return twitter.showStatus(tweetId);
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting status from tweet id");
		}
	}
	
	
	/**
	 * This function gets the last 100 tweets made by a user  
	 * @param screenName - User whose tweets are desired
	 * @return The last 100 tweets made by the user
	 * @throws QueryException
	 */
	public List<Status> getUsersTweets(String screenName) throws QueryException{
		try {
			Query query = new Query("from:" + screenName);
			query.setCount(100);
			QueryResult result = twitter.search(query);
			return result.getTweets();
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting user tweets from username");
		}
	}
	
	
	/** 
	 * getTrendingTweets finds any tweets which match the query (and geolocation if available)
	 * @param queryString - Search term to be used to find tweets
	 * @param latitude - Geolocation latitude (can be NaN)
	 * @param longitude - Geolocation longitude (can be NaN)
	 * @param radius - Geolocation radius in kilometers (can be NaN)
	 * @return List of Status objects (tweets)
	 * @throws QueryException
	 */
	public List<Status> getTrendingTweets(String queryString, Double latitude, Double longitude, double radius) throws QueryException {
		try {
			Query query= new Query(queryString); 
			
			// Add geolocation if available
			if (!Double.isNaN(latitude) && !Double.isNaN(longitude) && !Double.isNaN(radius)) {
				query.setGeoCode(new GeoLocation(latitude, longitude), radius, Query.KILOMETERS);
			}
			
			query.setCount(10);
			
			QueryResult result = twitter.search(query);
	
			return result.getTweets();
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting tweets from query");
		}
	}
	
	
	/**
	 * getRetweeters finds 10 users who have retweeted a given tweet
	 * @param tweetId - The unique id of the tweet
	 * @return A list of user objects who have retweeted the tweet
	 * @throws QueryException
	 */
	public List<User> getRetweeters(long tweetId) throws QueryException {
		try {
			ResponseList<Status> retweetList = twitter.getRetweets(tweetId);
			LinkedList<User> users = new LinkedList<User>();
			for (Status retweet : retweetList) {
				users.add(retweet.getUser());
				if (users.size() == 10) break;
			}
			return users;
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting retweeters from tweet id");
		}
	}

	
	/**
	 * This function can review tweets from a number of users and extract words that are used frequently. This should indicate discussion topics that are popular.
	 * A stop list is used to filter out words that are not content specific
	 * @param users - The number of users to review
	 * @param termsDesired - The number of terms to extract
	 * @param daySpan - From how many days ago should tweets be extracted
	 * @return - A pair of lists of terms. The t item of the pair contains the desired ranked terms, the u contains the remaining unranked terms
	 * @throws QueryException
	 */
	public Pair<LinkedList<Term>,LinkedList<Term>> getDiscussedTopics(LinkedList<User> users, int termsDesired, int daySpan) throws QueryException{
		//Calculate the date "daySpan" days ago
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -daySpan);
		Date sinceDate = cal.getTime();
		String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(sinceDate);
		
		//Generate an inverted index of terms. Each term maps to a pair containing the total occurrence count and an user count map. The user count map, maps a user name to their individual occurrence count
		Map<String, Pair<Integer,Map<Long,Integer>>> termUserMap = makeInvertedIndexOfTerms(users, formattedDate);
		
		//All the words have been appropriately added to the data structure so now we find the most frequent terms
		LinkedList<Term> frequentTerms = new LinkedList<Term>();
		//For as many words as needed, the most used word is obtained and stored
		for(int i=0; i<termsDesired;i++){
			//Find the most frequent term
			String mostCommonWord = "";
			int highestCount = Integer.MIN_VALUE;
			for (Entry<String, Pair<Integer,Map<Long, Integer>>> termMapEntry : termUserMap.entrySet()) {
				if(termMapEntry.getValue().t>highestCount){
					mostCommonWord = termMapEntry.getKey();
					highestCount = termMapEntry.getValue().t;
				}
			}
			
			//If there are less terms than we desired, then we can stop
			if(mostCommonWord.equals("")){break;}
			
			//Otherwise, create a new term object and store it
			Term newTerm = new Term(i+1,mostCommonWord,highestCount, extractUserCounts(termUserMap.get(mostCommonWord).u));
			frequentTerms.add(newTerm);
			
			//We remove this word from the map so the next most frequent can be obtained
			termUserMap.remove(mostCommonWord);
		}
		
		//Add the remaining unranked terms into a different list, providing they have been used at least 5 times
		LinkedList<Term> unrankedTerms = new LinkedList<Term>();
		for (Entry<String, Pair<Integer,Map<Long, Integer>>> termMapEntry : termUserMap.entrySet()) {
			if(termMapEntry.getValue().t >= 5){
				Term newTerm = new Term(0, termMapEntry.getKey(), termMapEntry.getValue().t, extractUserCounts(termMapEntry.getValue().u)); 
				unrankedTerms.add(newTerm);
			}
		}
		return new Pair<LinkedList<Term>,LinkedList<Term>>(frequentTerms,unrankedTerms);
	}
	
	
	/**
	 * Takes a list of users and a date and returns a complex inverted index of terms
 	 * Each term maps to a pair containing the total occurrence count and an user count map. 
 	 * The user count map, maps a user name to their individual occurrence count
	 * @param users - A list of users whose tweets should be collected
	 * @param formattedDate - A date from which tweets should start to be collected
	 * @return - Inverted index
	 * @throws QueryException
	 */
	private Map<String, Pair<Integer,Map<Long,Integer>>> makeInvertedIndexOfTerms(LinkedList<User> users, String formattedDate) throws QueryException{
		//Define data structure to hold inverted index
		Map<String, Pair<Integer,Map<Long,Integer>>> termUserMap = new HashMap<String, Pair<Integer,Map<Long,Integer>>>();
		
		try {
			//For each of the passed users
			for(User user : users){
				//Get all their tweets in the last "daySpan" days
				Query query = new Query("from:" + user.getScreenName()).since(formattedDate);
				query.setCount(100);
				QueryResult result = twitter.search(query);
				while(query!=null){
					List<Status> statuses = result.getTweets();
					//For each tweet
					for(Status status : statuses){
						//Remove undesired characters and separate tweet into separate words
						String[] words = status.getText().replaceAll("[^\\w #@']", "").toLowerCase().split("\\s+");
						//For each of the words
						for(String word : words){
							if(word.isEmpty()){break;}
							//Check the word isn't in the stop list, doesn't start with an '@' character and isn't a link that starts with "http"
							StopList stopList = new StopList();
							if(!stopList.wordInStopList(word) && word.charAt(0) != '@' && !word.contains("http")){
								//Now we know that the word is one we wish to record, we add it to the data structure
								Pair<Integer,Map<Long, Integer>> pair = termUserMap.get(word);
								Map<Long, Integer> userCountMap;
								int totalCount = 1; 
								//Has the word been seen by anyone before?
								if(pair == null){
									//Word not seen before map so create a new user map
									userCountMap = new HashMap<Long,Integer>();
									userCountMap.put(user.getId(), 1);
								} else {
									//Word has been seen before so get the current user map
									userCountMap = pair.u;
									Integer termCount = userCountMap.get(user.getId());
									//Have we seen this word by this user before?
									if(termCount == null){ 
										//User has not used word before so add them to the user map for this word and update the total count
										userCountMap.put(user.getId(), 1);
										totalCount += termUserMap.get(word).t;
									} else {
										//User has used word before so increment the user count and the total count
										userCountMap.put(user.getId(), termCount+1);
										totalCount += termUserMap.get(word).t;
									}
								}
								//Add the new/updated counts to the map overwriting any existing information about the term
								pair = new Pair<Integer, Map<Long, Integer>>(totalCount, userCountMap);
								termUserMap.put(word, pair);
							}
						}
					}
					//Get next set of tweets if possible
					query=result.nextQuery();
					if(query!=null) {
						result=twitter.search(query);
					}
				}
			}
			return termUserMap;
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error generating frequent term counts");
		}
	}
	
	
	/**
	 * Takes a map of userTermCounts and returns a list of pairs
	 * @param userCountMap - Map of user counts
	 * @return - List of pairs
	 */
	private LinkedList<Pair<Long, Integer>> extractUserCounts(Map<Long, Integer> userCountMap){
		LinkedList<Pair<Long,Integer>> userCounts = new LinkedList<Pair<Long,Integer>>();
		for (Entry<Long, Integer> userCountEntry : userCountMap.entrySet()) {
			userCounts.add(new Pair<Long,Integer>(userCountEntry.getKey(), userCountEntry.getValue()));
		}
		return userCounts;
	}
	
	
	/**
	 * getUserVenues finds any venues that a user has visited in the past x days
	 * @param username - Twitter username to search for 
	 * @param days - Days in the past to search for checkins (0 uses live streaming)
	 * @return List of Checkins also containing venue information
	 * @throws QueryException
	 */
	public List<CompleteVenue> getUserVenues(String username, int days) throws QueryException {
		try {
			List<CompleteVenue> resultList = new LinkedList<CompleteVenue>();
			
			// Calculate date minus days parameter
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			if (days > 0) {
				cal.add(Calendar.DAY_OF_YEAR, -days);
			}
			
			Query query = new Query("from:" + username + " foursquare").since(dateFormat.format(cal.getTime()));
			QueryResult result = twitter.search(query);
			
			while(query!=null){
				// Cycle through matching tweets
				for (Status tweet : result.getTweets()) {
					CompleteVenue venue = getVenueFromTweet(tweet);
					if(venue!=null){
						resultList.add(venue);
					}
				}
				//Get next set of tweets if possible
				query=result.nextQuery();
				if(query!=null) {
					result=twitter.search(query);
				}
			}
			return resultList;
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting venues from username");
		}
	}
	
	
	/**
	 * This function attempts to get a venue based on a tweet that contains foursquare information
	 * @param tweet - A tweet that is suspected of containing foursquare information
	 * @return A venue object if a venue is found, null otherwise
	 * @throws QueryException
	 */
	public CompleteVenue getVenueFromTweet(Status tweet) throws QueryException {
		try {
			// Extract foursquare links and retrieve foursquare checkin information
			for (URLEntity url : tweet.getURLEntities()) {
				try {
					String[] fsParams = expandFoursquareUrl(url.getExpandedURL());
					Result<Checkin> fsResult = foursquare.checkin(fsParams[0], fsParams[1]);
					
					// Get venue data from foursquare checkin
					if (fsResult.getMeta().getCode() == 200) {
						//resultList.add(fsResult.getResult());
						
						Result<CompleteVenue> venues = foursquare.venue(fsResult.getResult().getVenue().getId());
						
						if (venues.getMeta().getCode() == 200) {
							return venues.getResult();
						}
					}
				} catch (InvalidFoursquareUrlException ex) {
					//This type of error is expected frequently as not all urls searched will be foursquare
					LOGGER.log(Level.FINE, ex.getMessage(), ex);
				}
			}
			return null;
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error getting venue from tweet");
		}
	}
	
	
	/**
	 * expandFoursquareUrl takes a shortened Foursquare URL (4sq.com) and extracts user_id and authorisation code
	 * @param shortUrl - String of shortened Foursquare URL
	 * @return String array of user_id and authorisation code 
	 * @throws QueryException
	 * @throws InvalidFoursquareUrlException 
	 * @throws IOException 
	 */
	private String[] expandFoursquareUrl(String shortUrl) throws InvalidFoursquareUrlException {
		try {
	        URL url = new URL(shortUrl);
	        String[] expandedUrl = {"",""};
	        
	        if (url.getHost().equals("4sq.com")) {
	        
		        HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
		        connection.setInstanceFollowRedirects(false);
		        connection.connect();
		        URL longUrl = new URL(connection.getHeaderField("Location"));
		        connection.getInputStream().close();
		        
		        expandedUrl[0] = longUrl.getPath().replace("?s=", "/").split("/")[3];
	    		expandedUrl[1] = longUrl.getQuery().substring(2,29);
	        
	        }
			return expandedUrl;
		} catch (NullPointerException | MalformedURLException ex) {
			//Expected, If the url doesnt contain the desired parameters then its not a valid url
			throw new InvalidFoursquareUrlException("Not a valid foursquare url: " + shortUrl);
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new InvalidFoursquareUrlException("Error expanding foursquare url: " + shortUrl);
		}
	}
	
	
	/**
	 * getUsersAtVenue finds users who have vistited a venues in a specific geographic area and/or with a certain name in the last X days
	 * @param venueName - Name of a venue as a String
	 * @param latitude - Geolocation latitude
	 * @param longitude - Geolocation longitude
	 * @param radius - Geolocation radius from lat/long
	 * @param days - Days in the past to search for (0 = Live Stream)
	 * @param venues - Hashmap of venueIds and CompleteVenue objects
	 * @param venueTweets - Hashmap of tweetIds and Status objects
	 * @throws QueryException
	 */
	public void getUsersAtVenue(String venueName, double latitude, double longitude, double radius, int days, Map<String, CompleteVenue> venues, Map<String, List<Status>> venueTweets) throws QueryException {
		try {
			Query query= new Query(); 
			String queryText = "";
			if (!venueName.isEmpty()) {
				queryText = "foursquare " + venueName + " ";
			} else {
				queryText = ("foursquare ");
			}
			// Add geolocation if available
			if (!Double.isNaN(latitude) && !Double.isNaN(longitude) && !Double.isNaN(radius)) {
				query.setGeoCode(new GeoLocation(latitude, longitude), radius, Query.KILOMETERS);
			} 
			query.setQuery(queryText);
	
			// Calculate date minus days parameter
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			cal.add(Calendar.DAY_OF_YEAR, -days);
			query.since(dateFormat.format(cal.getTime()));
			
			QueryResult result = twitter.search(query);
	
			getUserVenuesFromTweets(result.getTweets(), venues, venueTweets);	
		} catch (Exception ex) {
			//Catch any errors, log them, then throw a query exception
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new QueryException("Error users at venue");
		}
	}
	
	
	/**
	 * This method gets venues that users have visited from a series of tweets
	 * @param tweetsList
	 * @param venues
	 * @param venueTweets
	 * @throws QueryException
	 */
	public void getUserVenuesFromTweets(List<Status> tweetsList, Map<String, CompleteVenue> venues, Map<String, List<Status>> venueTweets) throws QueryException {
		// Cycle through matching tweets
		for (Status tweet : tweetsList) {
			try {
				CompleteVenue venue = getVenueFromTweet(tweet);
				if(venue!=null){
					if(!venues.containsKey(venue.getId())){
						venues.put(venue.getId(),venue);
						List<Status> tweetList = new LinkedList<Status>();
						tweetList.add(tweet);
						venueTweets.put(venue.getId(), tweetList);
					} else {
						venueTweets.get(venue.getId()).add(tweet);
					} 
				}
			} catch (Exception ex) {
				//Catch any errors, log them, then throw a query exception
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				throw new QueryException("Error getting venues from tweets");
			}
		}
	}
	
	
	/**
	 * Get the 10 most popular places in the geographic region passed using the Google Places api
	 * @param lat - Latitude
	 * @param lon - Longitude
	 * @param radius
	 * @return - A list of nearby places
	 * @throws QueryException
	 */
	public List<Place> getNearbyPlaces(double lat, double lon, double radius) throws QueryException{
		List<Place> placeList = new LinkedList<Place>();
		GooglePlaces placesApi = new GooglePlaces("AIzaSyAQSRWiDPQTAeFTilEGZuyouNaF0biz7ks");
		PlacesResult result = placesApi.search((float)lat, (float)lon, (int)Math.round(radius*1000), false);
		if (result.isOkay()) {
			for (Place place : result) {
				placeList.add(place);
				if (placeList.size() == 10) break;
			}
		} else {
			throw new QueryException("Error fetching nearby venues");
		}
		return placeList;
	}
}

