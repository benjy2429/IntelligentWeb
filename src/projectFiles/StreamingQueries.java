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
	
	
	public void addUsersAtVenueListener(String venueName, double latitude, double longitude) {
	StatusListener listener = new StatusListener() { 
		@Override public void onStatus(Status status) {
			if (Calendar.getInstance().getTime().before( shutdownTime.getTime() )) {
	System.out.println(status.getText());
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
		 
		if (Double.isNaN(latitude) && Double.isNaN(longitude)) {
			locationsToTrack = new double[][]{{latitude, longitude}}; //TODO Calculate bounding box from lat/lon
		} else {
			stringsToTrack = new String[]{"foursquare", venueName};
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
