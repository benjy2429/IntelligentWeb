package projectFiles;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import twitter4j.*;

/**
 * StreamingQueries opens and closes new Twitter streams to fetch live tweets
 * Tweets are stored in a List until they are requested and fetched from the servlet
 * If a request does not occur for 60 seconds, the stream closes
 * @author Ben Carr & Luke Heavens
 * Last Updated: 20/03/2014
 */

public class StreamingQueries {
	private final int TIMEOUT_SECONDS = 60;
	private TwitterStream twitterStream;
	private List<Status> tweets = new LinkedList<Status>();
	private Calendar shutdownTime;
	private boolean shutdown = false; 
	//Logger
	private static final Logger LOGGER = Logger.getLogger(StreamingQueries.class.getName());

	
	/**
	 * Queries Constructor for a new Twitter stream connection
	 * @param twitter
	 */
	public StreamingQueries(TwitterStream twitterStream){
		this.twitterStream = twitterStream;
		incrementShutdownTimer();
	}
	
	
	/**
	 * This function adds a fixed number of seconds to the current time and stores it in a variable
	 * It is used to close the stream if no requests are recieved before that time
	 */
	private void incrementShutdownTimer() {	
		shutdownTime = Calendar.getInstance();
		shutdownTime.add(Calendar.SECOND, TIMEOUT_SECONDS);
	}
	
	
	/**
	 * getTwitterStream returns the Twitter stream object
	 * @return TwitterStream - The Twitter stream object
	 */
	public TwitterStream getTwitterStream() {
		return twitterStream;
	}
	
	
	/**
	 * This function adds a listener to the stream to record live tweets from a specific user 
	 * @param userId - The ID of the user
	 */
	public void addUserVenuesListener(long userId) {
	StatusListener listener = new StatusListener() { 
		@Override public void onStatus(Status status) {
			// If the shutdown time is after the current time, record the tweet, otherwise close the connection
			if (Calendar.getInstance().getTime().before( shutdownTime.getTime() )) {
				tweets.add(status);				
			} else {
				LOGGER.log(Level.FINE, "Shutting down Twitter stream..");
				clearLists();
				shutdown = true;
				twitterStream.shutdown();
			}
		} 
		@Override public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}  
		@Override public void onTrackLimitationNotice(int numberOfLimitedStatuses) {} 
		@Override public void onScrubGeo(long userId, long upToStatusId) {}   
		@Override public void onStallWarning(StallWarning warning) {}
		@Override public void onException(Exception ex) { ex.printStackTrace(); }
		};
		twitterStream.addListener(listener);
		
		// Filter the listener to tweets from the given user
		int count = 0;
		long[] idToFollow = new long[1];
		idToFollow[0] = userId; 
		String[] stringsToTrack = null;
		double[][] locationsToTrack = null;
		twitterStream.filter(new FilterQuery(count, idToFollow, stringsToTrack, locationsToTrack));
	}
	
	
	/**
	 * This function adds a listener to the stream to record live tweets in an area or about a venue
	 * @param venueName - The name of the venue to track
	 * @param latitude - The latitude of the location to track
	 * @param longitude - The longitude of the location to track
	 * @param radius - The radius of the location to track
	 */
	public void addUsersAtVenueListener(String venueName, double latitude, double longitude, double radius) {
	StatusListener listener = new StatusListener() { 
		@Override public void onStatus(Status status) {
			// If the shutdown time is after the current time, record the tweet, otherwise close the connection
			if (Calendar.getInstance().getTime().before( shutdownTime.getTime() )) {
				tweets.add(status);				
			} else {
				LOGGER.log(Level.FINE, "Shutting down Twitter stream..");
				clearLists();
				shutdown = true;
				twitterStream.shutdown();
			}
		} 
		@Override public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}  
		@Override public void onTrackLimitationNotice(int numberOfLimitedStatuses) {} 
		@Override public void onScrubGeo(long userId, long upToStatusId) {}   
		@Override public void onStallWarning(StallWarning warning) {}
		@Override public void onException(Exception ex) { ex.printStackTrace(); }
		};
		twitterStream.addListener(listener);

		int count = 0;
		long[] idToFollow = null; 
		String[] stringsToTrack = null;
		double[][] locationsToTrack = null;
		
		// If a geolocation is given, calculate the bounding box for that location and use as a filter,
		// otherwise listen for tweets that contain the venue name
		if (!Double.isNaN(latitude) && !Double.isNaN(longitude) && !Double.isNaN(radius)) {
			// Calculate geo bounding box from lat/lon coordinates and radius
			double oneKmDeg = 90/10001.965729;
			double radiusAdjustDeg = oneKmDeg*radius;
			double lat1 = latitude - radiusAdjustDeg;
			double lon1 = longitude - radiusAdjustDeg;
			double lat2 = latitude + radiusAdjustDeg;
			double lon2 = longitude + radiusAdjustDeg;
			locationsToTrack = new double[][]{{lon1, lat1}, {lon2, lat2}};
		} else {
			stringsToTrack = new String[]{"foursquare " + venueName};
		}		
		
		twitterStream.filter(new FilterQuery(count, idToFollow, stringsToTrack, locationsToTrack));
	}
	
	
	/**
	 * This function updates the shutdown time and returns the stored list of tweets 
	 * @return A list of tweets recorded since the last request
	 */
	public List<Status> getTweets() {
		incrementShutdownTimer();
		return tweets;
	}
	
	
	/**
	 * This function clears the recorded list of tweets, ready for new tweets to be recorded
	 */
	public void clearLists() {
		tweets.clear();
	}
	
	
	/**
	 * This function checks whether the Twitter stream is shutdown or currently running
	 * @return Boolean whether the stream is shutdown or not
	 */
	public boolean isShutdown() {
		return shutdown;
	}
}
