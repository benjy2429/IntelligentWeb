package projectFiles;

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
	
	
	public User getTwitterUser(String username) throws TwitterException {
		return twitter.showUser(username);
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
		
		/*
		for (Status tweet : result.getTweets()) {
			resultString += "@" + tweet.getUser().getScreenName() + ": " + tweet.getText() + "<br>";
			
			/*TODO UNCOMMENT FOR RETWEETER USERNAMES
			long[] retweeters = twitter.getRetweeterIds( tweet.getId(), 10, -1 ).getIDs();
			for ( int i=0; i<retweeters.length; i++ ) {
				resultString += "&#9;" + twitter.showUser( retweeters[i] ).getName() + "<br>";
			}		
		}
		*/

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
	public LinkedList<String> getDiscussedTopics(LinkedList<String> users, int termsDesired, int daySpan) throws FileException, TwitterException {
		//Define a tuple for use in storing counts about keywords
		class Pair<T, U> {         
			public final T t;
			public final U u;

			public Pair(T t, U u) {         
				this.t= t;
				this.u= u;
			}
		}
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
		LinkedList<String> frequentTerms = new LinkedList<String>();
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
				String newString = (i+1) +". " + mostCommonWord + " - Total count: " + highestCount + " {";
				for (Entry<String, Integer> userCountEntry : termUserMap.get(mostCommonWord).u.entrySet()) {
					newString += " " + userCountEntry.getKey() + ":" + userCountEntry.getValue();
				}
				newString += " }";
				frequentTerms.add(newString);
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

		// Cycle through matching tweets
		for (Status tweet : result.getTweets()) { //TODO Check if need to iterate through pages
			
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
							resultList.add( venues.getResult() );
						}
					}
				} catch (Exception e) {
					//URL does not match 4sq.com
				}
			}
		}
		return resultList;
	}
	
	
	/**
	 * expandFoursquareUrl takes a shortened Foursquare URL (4sq.com) and extracts user_id and authorisation code
	 * @param shortUrl - String of shortened Foursquare URL
	 * @return String array of user_id and authorisation code 
	 * @throws Exception
	 */
	private String[] expandFoursquareUrl(String shortUrl) throws Exception {
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
        
        } else {
        	throw new Exception("URL does not match 4sq.com");
        }

		return expandedUrl;
	}
	
	
	// 3. Who is visiting venues in a specific geographic area (or visiting a named venue) or have done so in the last X days
	/**
	 * getUsersAtVenue finds users who have vistited a venue in the past X days
	 * Either latitude/longitude required OR location name required
	 * @param latitude - Geolocation latitude
	 * @param longitude - Geolocation longitude
	 * @param location - Geolocaiton location name
	 * @param venueName - Name of venue
	 * @param days - Number of past days to search 
	 * @return
	 */
	//TODO REWRITE TO SEARCH VIA TWITTER FIRST - CANNOT GET CHECKINS DIRECTLY FROM A FOURSQUARE VENUE
	public String getUsersAtVenue(double latitude, double longitude, String location, String venueName, int days) { //TODO Use streaming api if days==0
		String resultString = "";
		try {
			Result<VenuesSearchResult> result = null;
			if ( !Double.isNaN(latitude) && !Double.isNaN(longitude) ) {
				result = foursquare.venuesSearch(latitude+","+longitude, 10000.0, 0.0, 10000.0, venueName, 15, "checkin", "", "", "", "");
			} else if ( !location.isEmpty() ) {
				result = foursquare.venuesSearch(location, venueName, 15, "checkin", "", "", "", "");
			} else {
				//TODO Set default location to twitter user location??
				throw new Exception("Missing required fields");
			}
			
			if (result.getMeta().getCode() == 200) {
				for (CompactVenue venue : result.getResult().getVenues()) {
					resultString += venue.getName() + " (" + venue.getLocation().getAddress() + ")<br>";
					
					//TODO Get users who have visited in X days
				}
			} else {
				System.out.println(result.getMeta().getCode());
				System.out.println(result.getMeta().getErrorType());
				System.out.println(result.getMeta().getErrorDetail());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println( "Error searching for venues" );
		}
		
		return resultString;
	}
}
