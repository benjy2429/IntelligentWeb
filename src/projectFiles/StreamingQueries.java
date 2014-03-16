package projectFiles;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.entities.CompleteVenue;

import twitter4j.*;

public class StreamingQueries {

	private TwitterStream twitterStream;
	private FoursquareApi foursquare;
	private List<Status> tweets = null;
	private List<CompleteVenue> venues = null;
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
	
	
	public StreamingQueries(TwitterStream twitterStream, FoursquareApi foursquare){
		this.twitterStream = twitterStream;
		this.foursquare = foursquare;
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
				
				// Extract foursquare links and retrieve foursquare checkin information
				for (URLEntity url : status.getURLEntities()) {
					try {
						String[] fsParams = expandFoursquareUrl( url.getExpandedURL() );
						Result<Checkin> fsResult = foursquare.checkin( fsParams[0], fsParams[1] );
						
						// Get venue data from foursquare checkin
						if (fsResult.getMeta().getCode() == 200) {
							//resultList.add( fsResult.getResult() );
							
							Result<CompleteVenue> venuesResult = foursquare.venue( fsResult.getResult().getVenue().getId() );
							
							if (venuesResult.getMeta().getCode() == 200) {
								venues.add( venuesResult.getResult() );
							}
						}
					} catch (Exception e) {
						//URL does not match 4sq.com
					}
				}			
				
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
		
		venues = new LinkedList<CompleteVenue>();
		
		int count = 0;
		long[] idToFollow = new long[1]; 
		idToFollow[0] = userId;
		String[] stringsToTrack = new String[1];
		stringsToTrack[0] = "foursquare";
		double[][] locationsToTrack = new double[0][0];
		twitterStream.filter(new FilterQuery(count, idToFollow, stringsToTrack, locationsToTrack));
	}
	
	
	/**
	 * expandFoursquareUrl takes a shortened Foursquare URL (4sq.com) and extracts user_id and authorisation code
	 * @param shortUrl - String of shortened Foursquare URL
	 * @return String array of user_id and authorisation code 
	 * @throws Exception
	 */
	private String[] expandFoursquareUrl(String shortUrl) throws Exception { //TODO use the one in queries class
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
	
	
	public List<Status> getTweets() {
		incrementShutdownTimer();
		return tweets;
	}
	
	public List<CompleteVenue> getVenues() {
		incrementShutdownTimer();
		return venues;
	}
	
	public void clearLists() {
		tweets.clear();
		venues.clear();
	}
	
	public boolean isShutdown() {
		return shutdown;
	}
}
