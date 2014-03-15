package projectFiles;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import twitter4j.*;

public class StreamingQueries {

	private TwitterStream twitterStream;
	private final List<Status> tweets;
	private Calendar shutdownTime;

	
	/**
	 * Queries Constructor for a Twitter connection
	 * @param twitter
	 */
	public StreamingQueries(TwitterStream twitterStream){
		this.twitterStream = twitterStream;
		tweets = new LinkedList<Status>();
		
		shutdownTime = Calendar.getInstance();
		shutdownTime.add(Calendar.SECOND, 10);
		
		addListener();
	}
	
	
	public TwitterStream getTwitterStream() {
		return twitterStream;
	}
	
	
	public void addListener() {
	StatusListener listener = new StatusListener() { 
		@Override public void onStatus(Status status) {
			System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText()); 
			tweets.add(status);
		} 
		@Override public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}  
		@Override public void onTrackLimitationNotice(int numberOfLimitedStatuses) {} 
		@Override public void onScrubGeo(long userId, long upToStatusId) {}   
		@Override public void onStallWarning(StallWarning warning) {}
		@Override public void onException(Exception ex) { ex.printStackTrace(); }
		};
		twitterStream.addListener(listener);
		
		int count = 0;
		long[] idToFollow = new long[0];
		String[] stringsToTrack = new String[1];
		stringsToTrack[0] = "foursquare";
		double[][] locationsToTrack = new double[0][0];
		twitterStream.filter(new FilterQuery(count, idToFollow, stringsToTrack, locationsToTrack));
	}
	
	
	public List<Status> getTweets() {
		return tweets;
	}
	
	public void clearTweets() {
		tweets.clear();
	}
}
