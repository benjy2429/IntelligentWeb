package projectFiles;

import java.io.IOException;
import java.net.HttpURLConnection;
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

import com.claygregory.api.google.places.*;
import com.claygregory.api.google.places.Place;

import exceptions.FileException;
import fi.foyt.foursquare.api.*;
import fi.foyt.foursquare.api.entities.*;
import twitter4j.*;

public class Queries {

	
	private Twitter twitter;
	private FoursquareApi foursquare;

	
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
	 * @throws TwitterException
	 */
	public User getTwitterUser(String username) throws TwitterException {
		return twitter.showUser(username);
	}

	/**
	 * getTwitterUsers finds multiple Twitter users by a list of Twitter screen names 
	 * @param users - A List of Twitter screen names
	 * @return A list of Twitter user objects for the given usernames
	 * @throws TwitterException
	 */
	public List<User> getTwitterUsers(List<String> users) throws TwitterException {
		String[] userNamesArray = new String[users.size()];
		return twitter.lookupUsers(users.toArray(userNamesArray));
	}
	
	public List<Status> getUsersTweets(String screenName) throws TwitterException{
		Query query = new Query("from:" + screenName);
		query.setCount(100);
		QueryResult result = twitter.search(query);
		return result.getTweets();
	}
	
	// 1. Tracking public discussions on specific topics
	/** 
	 * getTrendingTweets finds any tweets which match the query (and geolocation if available)
	 * @param queryString - Search term to be used to find tweets
	 * @param latitude - Geolocation latitude (can be NaN)
	 * @param longitude - Geolocation longitude (can be NaN)
	 * @param radius - Geolocation radius in kilometers (can be NaN)
	 * @return List of Status objects (tweets)
	 */
	public List<Status> getTrendingTweets(String queryString, Double latitude, Double longitude, double radius) throws TwitterException {
		Query query= new Query( queryString ); 
		
		// Add geolocation if available
		if ( !Double.isNaN(latitude) && !Double.isNaN(longitude) && !Double.isNaN(radius) ) {
			query.setGeoCode(new GeoLocation(latitude, longitude), radius, Query.KILOMETERS); //TODO Maybe add ability to choose between Km or Miles
		}
		
		query.setCount(10);
		//query.setResultType(Query.POPULAR); //TODO Check result ordering
		
		QueryResult result = twitter.search(query);

		return result.getTweets();
	}
	
	
	/**
	 * getRetweeters finds 10 users who have retweeted a given tweet
	 * @param tweetId - The unique id of the tweet
	 * @return A list of user objects who have retweeted the tweet
	 * @throws TwitterException
	 */
	public List<User> getRetweeters(long tweetId) throws TwitterException {
		ResponseList<Status> retweetList = twitter.getRetweets( tweetId );
		LinkedList<User> users = new LinkedList<User>();
		for (Status retweet : retweetList ) {
			users.add( retweet.getUser() );
			if (users.size() == 10) break;
		}
		return users;
	}

	
	// 2a. What users are discussing
	/**
	 * This function can review tweets from a number of users and extract words that are used frequently. This should indicate discussion topics that are popular.
	 * A stop list is used to filter out words that are not content specific
	 * @param users - The number of users to review
	 * @param termsDesired - The number of terms to extract
	 * @param daySpan - From how many days ago should tweets be extracted
	 * @return - A list of strings containing keywords and counts
	 * @throws TwitterException - An error relating to the use of the twitter API
	 * @throws FileException - An error that has occurred as a result of accessing files on the server
	 */
	public LinkedList<Term> getDiscussedTopics(LinkedList<String> users, int termsDesired, int daySpan) throws FileException, TwitterException {
		//Calculate the date "daySpan" days ago
		Calendar cal = Calendar.getInstance();
		cal.add( Calendar.DAY_OF_YEAR, -daySpan);
		Date sinceDate = cal.getTime();
		String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(sinceDate);
		//Make an inverted index of terms. Each term maps to a pair containing the total count and a user count map. The user count map, maps a user name to their term count
		Map<String, Pair<Integer,Map<String,Integer>>> termUserMap = new HashMap<String, Pair<Integer,Map<String,Integer>>>();
		//For each of the passed users
		for(String userName : users){//TODO strip out naughty things in string
			//Get all their tweets in the last "daySpan" days
			Query query = new Query("from:" + userName).since(formattedDate);
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
						if(!stopList.wordInStopList(word) && word.charAt(0) != '@' && !word.contains("http")){ //TODO charAt(0) out of range error
							//Now we know that the word is one we wish to record, we add it to the data structure
							Pair<Integer,Map<String, Integer>> pair = termUserMap.get(word);
							Map<String, Integer> userCountMap;
							int totalCount = 1; 
							//Has the word been seen by anyone before?
							if(pair == null){
								//Word not seen before map so create a new user map
								userCountMap = new HashMap<String,Integer>();
								userCountMap.put(userName, 1);
							} else {
								//Word has been seen before so get the current user map
								userCountMap = pair.u;
								Integer termCount = userCountMap.get(userName);
								//Have we seen this word by this user before?
								if(termCount == null){ 
									//User has not used word before so add them to the user map for this word and update the total count
									userCountMap.put(userName, 1);
									totalCount += termUserMap.get(word).t;
								} else {
									//User has used word before so increment the user count and the total count
									userCountMap.put(userName, termCount+1);
									totalCount += termUserMap.get(word).t;
								}
							}
							//Add the new/updated counts to the map overwriting any existing information about the term
							pair = new Pair<Integer, Map<String, Integer>>(totalCount, userCountMap);
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
		//All the words have been appropriately added to the data structure so now we find the most frequent terms
		LinkedList<Term> frequentTerms = new LinkedList<Term>();
		//For as many words as needed, the most used word is obtained, utilised then removed so the next most frequent can be obtained
		for(int i=0; i<termsDesired;i++){
			String mostCommonWord = "";
			int highestCount = Integer.MIN_VALUE;
			for (Entry<String, Pair<Integer,Map<String, Integer>>> termMapEntry : termUserMap.entrySet()) {
				if(termMapEntry.getValue().t>highestCount){
					mostCommonWord = termMapEntry.getKey();
					highestCount = termMapEntry.getValue().t;
					
				}
			}
			//If there are less terms than desired then we dont need to try and generate a string for null data
			if(!mostCommonWord.equals("")){
				Term newTerm = new Term();
				newTerm.rank = i+1;
				newTerm.term = mostCommonWord;
				newTerm.totalCount = highestCount;
				for (Entry<String, Integer> userCountEntry : termUserMap.get(mostCommonWord).u.entrySet()) {
					newTerm.userCounts.add(new Pair<String,Integer>(userCountEntry.getKey(), userCountEntry.getValue()));
				}
				frequentTerms.add(newTerm);
			}
			termUserMap.remove(mostCommonWord);
		}
		return frequentTerms;
	}
	
	
	// 2b. What venues a specific user has visited in the last X days
	/**
	 * getUserVenues finds any venues that a user has visited in the past x days
	 * @param username - Twitter username to search for 
	 * @param days - Days in the past to search for checkins (0 uses live streaming)
	 * @return List of Checkins also containing venue information
	 */
	public List<CompleteVenue> getUserVenues(String username, int days) throws Exception, TwitterException, FoursquareApiException { //TODO Use twitter streaming api if days==0
		List<CompleteVenue> resultList = new LinkedList<CompleteVenue>();
		
		// Calculate date minus days parameter
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		if (days > 0) {
			cal.add( Calendar.DAY_OF_YEAR, -days);
		}
		
		Query query = new Query("from:" + username + " foursquare").since( dateFormat.format( cal.getTime() ) );
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
	}
	
	public CompleteVenue getVenueFromTweet(Status tweet){
		// Extract foursquare links and retrieve foursquare checkin information
		for (URLEntity url : tweet.getURLEntities()) {
			try {
				String[] fsParams = expandFoursquareUrl( url.getExpandedURL() );
				Result<Checkin> fsResult = foursquare.checkin( fsParams[0], fsParams[1] );
				
				// Get venue data from foursquare checkin
				if (fsResult.getMeta().getCode() == 200) {
					//resultList.add( fsResult.getResult() );
					
					Result<CompleteVenue> venues = foursquare.venue( fsResult.getResult().getVenue().getId() );
					
					if (venues.getMeta().getCode() == 200) {
						return venues.getResult();
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		return null;
	}
	
	
	/**
	 * expandFoursquareUrl takes a shortened Foursquare URL (4sq.com) and extracts user_id and authorisation code
	 * @param shortUrl - String of shortened Foursquare URL
	 * @return String array of user_id and authorisation code 
	 * @throws IOException, ArrayIndexOutOfBoundsException 
	 * @throws Exception
	 */
	private String[] expandFoursquareUrl(String shortUrl) throws IOException, ArrayIndexOutOfBoundsException {
        URL url = new URL(shortUrl);
        String[] expandedUrl = {"",""};
        
        if (url.getHost().equals("4sq.com")) {
        
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
	        connection.setInstanceFollowRedirects(false);
	        connection.connect();
	        URL longUrl = new URL( connection.getHeaderField("Location") );
	        connection.getInputStream().close();
	        
	        expandedUrl[0] = longUrl.getPath().replace("?s=", "/").split("/")[3];
    		expandedUrl[1] = longUrl.getQuery().substring(2,29);
        
        }

		return expandedUrl;
	}
	
	
	
	
	// 3. Who is visiting venues in a specific geographic area (or visiting a named venue) or have done so in the last X days
	/**
	 * getUsersAtVenue finds users who have vistited a venue in the past X days
	 * @param venueName - Name of a venue as a String
	 * @param latitude - Geolocation latitude
	 * @param longitude - Geolocation longitude
	 * @param radius - Geolocation radius from lat/long
	 * @param days - Days in the past to search for (0 = Live Stream)
	 * @param venues - Hashmap of venueIds and CompleteVenue objects
	 * @param venueTweets - Hashmap of tweetIds and Status objects
	 * @throws TwitterException 
	 */
	public void getUsersAtVenue(String venueName, double latitude, double longitude, double radius, int days, Map<String, CompleteVenue> venues, Map<String, List<Status>> venueTweets) throws TwitterException {
		Query query= new Query(); 
		String queryText = "";
		if ( !venueName.isEmpty() ) {
			queryText = "foursquare " + venueName + " ";
		} else {
			queryText = ("foursquare ");
		}
		// Add geolocation if available
		if ( !Double.isNaN(latitude) && !Double.isNaN(longitude) && !Double.isNaN(radius) ) {
			query.setGeoCode(new GeoLocation(latitude, longitude), radius, Query.KILOMETERS); //TODO Maybe add ability to choose between Km or Miles
		} 
		query.setQuery(queryText);

		// Calculate date minus days parameter
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		cal.add( Calendar.DAY_OF_YEAR, -days);
		query.since( dateFormat.format( cal.getTime() ) );
		
		//query.setCount(10);
		
		QueryResult result = twitter.search(query);

		getUserVenuesAndTweets(result.getTweets(), venues, venueTweets);		
			
	}
	
	
	public void getUserVenuesAndTweets(List<Status> tweetsList, Map<String, CompleteVenue> venues, Map<String, List<Status>> venueTweets) {
		// Cycle through matching tweets
		for (Status tweet : tweetsList) {
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
		}
	}
	
	
	public List<Place> getNearbyPlaces(double lat, double lon, double radius) {
		List<Place> placeList = new LinkedList<Place>();
		GooglePlaces placesApi = new GooglePlaces("AIzaSyAQSRWiDPQTAeFTilEGZuyouNaF0biz7ks");
		PlacesResult result = placesApi.search((float)lat, (float)lon, (int)Math.round(radius*1000), false);
		
		if (result.isOkay()) {
			for (Place place : result) {
				//System.out.println(place.getName() + ", " + place.getVicinity() + ", " + place.getGeometry().getLocation());
				placeList.add(place);
				if (placeList.size() == 10) break;
			}
		} else {
			System.out.println("Error fetching nearby venues!");
		}
		
		return placeList;
	}
	
	
	//Define a tuple for use in storing counts about keywords
	class Pair<T, U> {         
		public final T t;
		public final U u;

		public Pair(T t, U u) {         
			this.t= t;
			this.u= u;
		}
	}
	
	class Term{
		public int rank;
		public String term;
		public int totalCount;
		public LinkedList<Pair<String,Integer>> userCounts = new LinkedList<Pair<String,Integer>>();
	}
}
