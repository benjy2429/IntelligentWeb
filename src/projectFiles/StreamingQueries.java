package projectFiles;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import twitter4j.*;

public class StreamingQueries {

	private TwitterStream twitterStream;
	private List<Status> tweets = new LinkedList<Status>();
	private Calendar shutdownTime;
	private boolean shutdown = false; 

	
	/**
	 * Queries Constructor for a Twitter connection
	 * @param twitter
	 */
	public StreamingQueries(TwitterStream twitterStream){
		this.twitterStream = twitterStream;
		incrementShutdownTimer();
	}
	
	
	private void incrementShutdownTimer() {	
		shutdownTime = Calendar.getInstance();
		shutdownTime.add(Calendar.SECOND, 60);
	}
	
	
	public TwitterStream getTwitterStream() {
		return twitterStream;
	}
	
	
	public void addUserVenuesListener(long userId) {
	StatusListener listener = new StatusListener() { 
		@Override public void onStatus(Status status) {
			if (Calendar.getInstance().getTime().before( shutdownTime.getTime() )) {
				tweets.add(status);				
			} else {
				System.out.println("Shutting down Twitter stream..");
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
		long[] idToFollow = new long[1];
		idToFollow[0] = userId; 
		String[] stringsToTrack = null;
		double[][] locationsToTrack = null;
		twitterStream.filter(new FilterQuery(count, idToFollow, stringsToTrack, locationsToTrack));
	}
	
	
	public void addUsersAtVenueListener(String venueName, double latitude, double longitude, double radius) {
	StatusListener listener = new StatusListener() { 
		@Override public void onStatus(Status status) {
			if (Calendar.getInstance().getTime().before( shutdownTime.getTime() )) {
	System.out.println(status.getUser().getScreenName() + "/status/" + status.getId());
				tweets.add(status);				
			} else {
				System.out.println("Shutting down Twitter stream..");
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
		
		if (!Double.isNaN(latitude) && !Double.isNaN(longitude) && !Double.isNaN(radius)) {
			// Calculate geo bounding box from lat/lon coordinates and radius
			double oneKmDeg = 90/10001.965729;
			double radiusAdjustDeg = oneKmDeg*(radius/2);
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
	
	
	public List<Status> getTweets() {
		incrementShutdownTimer();
		return tweets;
	}
	
	
	public void clearLists() {
		tweets.clear();
	}
	
	
	public boolean isShutdown() {
		return shutdown;
	}
}
