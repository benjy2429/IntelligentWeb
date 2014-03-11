package sandbox;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.*;

import twitter4j.*;
import twitter4j.conf.*;


public class Queries extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
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
    
	private Twitter initTwitter() throws Exception {
		return (new TwitterFactory(confTwitter().build()).getInstance());
	}
	
	private TwitterStream initTwitterStream() throws Exception {
		return (new TwitterStreamFactory(confTwitter().build()).getInstance());
	}
	
	private FoursquareApi initFoursquare() throws Exception {
		FoursquareApi fs = new FoursquareApi( "4QG35KZ2EGKKO4QD3F5TJ1UPA13Y5FZUNK1HLKKAXTLG52KU", 
			"WVKWQA031AG1USNFX0I5WULNQ2X3ZPHX0BEG52KGJCIBDU1F",
			"http://lukeheavens.co.uk/" );
		fs.setoAuthToken( "1R2QORAMBVJ3SS0MTCGG1FUROFW0MFPMKXB5HHOQUJLQ3JWL" );
		return fs;
	}
	
	// 1. Tracking public discussions on specific topics
	private String searchTrends(Twitter twitter){
		
		String resultString= "";
		
		try {
			
			Query query= new Query("#sheffield"); //TODO Use query from form  

			//query.setGeoCode(new GeoLocation(53.383, -1.483), 2, Query.KILOMETERS); // TODO Include geolocation if available
			
			query.setCount(10);
			//query.setResultType(Query.POPULAR); //TODO Check result ordering
			
			QueryResult result = twitter.search(query);

			for (Status tweet : result.getTweets()) {
				resultString += "@" + tweet.getUser().getScreenName() + ": " + tweet.getText() + "<br>";
				
				/*TODO UNCOMMENT FOR RETWEETER USERNAMES
				long[] retweeters = twitter.getRetweeterIds( tweet.getId(), 10, -1 ).getIDs();
				for ( int i=0; i<retweeters.length; i++ ) {
					resultString += "&#9;" + twitter.showUser( retweeters[i] ).getName() + "<br>";
				}
				*/		
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to search tweets:" +	e.getMessage());
			System.exit(-1);
		}
		return resultString;
	}
	
	// 2b. What venues a specific user has visited in the last X days
	private String userVenues(Twitter t, FoursquareApi fs) {
		String resultString = "";
		
		try {
			String username = "stevewoz"; //TODO To be passed in as param

			Query query = new Query("from:" + username + " foursquare").since("2014-02-03");
			QueryResult result = t.search(query);

			for (Status tweet : result.getTweets()) {
				
				for (URLEntity url : tweet.getURLEntities()) {
					String[] fsParams = expandUrl( url.getExpandedURL() );
					Result<Checkin> fsResult = fs.checkin( fsParams[0], fsParams[1] );
					
					if (fsResult.getMeta().getCode() == 200) {
						CompactVenue venue = fsResult.getResult().getVenue();
						resultString += venue.getName() + ", " + venue.getLocation().getCity() + " (" + url.getExpandedURL() + ")<br>";
					} else {
						throw new Exception( fsResult.getMeta().getCode() + ", " + fsResult.getMeta().getErrorType() );
					}
					
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to get user or visited venues:" + e.getMessage());
		}
		
		return resultString;
	}
	
	
	private String[] expandUrl(String shortUrl) throws Exception {
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
	private String searchVenues(FoursquareApi fs) {
		String resultString = "";
		try {
			//TODO Set default location to twitter user location
			Result<VenuesSearchResult> result = fs.venuesSearch("Sheffield", "pizza hut", 15, "checkin", "", "", "", ""); //TODO Replace with params
			
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		
		Twitter t = null;
		FoursquareApi fs = null;
		try {
			t = initTwitter();	
			fs = initFoursquare();
			
			/* LIVE FEED
			TwitterStream ts = initTwitterStream();
			StatusListener listener = new StatusListener() {	
				public void onException(Exception arg0) {}
				public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
				public void onStatus(Status status) {
					System.out.println( "@" + status.getUser().getName() + ": " + status.getText() );
				}
				public void onStallWarning(StallWarning warning) {}
				public void onScrubGeo(long userId, long upToStatusId) {}	
				public void onDeletionNotice(StatusDeletionNotice arg0) {}
			};
			FilterQuery fq = new FilterQuery();
	        String keywords[] = {"sheffield"};
	        fq.track(keywords);
			ts.addListener(listener);
			ts.filter(fq);
			*/
			
			/* TRACK MOST POPULAR TOPICS */
			//out.println(searchTrends(t));
			
			/* WHAT VENUES DOES USER VISIT */
			//out.println( userVenues(t, fs) );
			out.println( userVenues(t, fs) );
			
		} catch (Exception e) {
			out.println("Cannot initialise Twitter");
			e.printStackTrace();
		}
		
		
		/*
		
		try {
			fs = initFoursquare();
			
			//out.println( searchVenues(fs) );
			
			/* GET A USER PROFILE FROM USER_ID
			Result<CompleteUser> result = fs.user("80565377");
			
			if (result.getMeta().getCode() == 200) {
				CompleteUser user = result.getResult();
				out.println( "<img src='" + user.getPhoto() + "'/>" );
				out.println( user.getFirstName() + " " + user.getLastName() + " (" + user.getHomeCity() + ")" );
			} else {
				System.out.println(result.getMeta().getCode() + ": ");
				System.out.println(result.getMeta().getErrorType() + ": ");
				System.out.println(result.getMeta().getErrorDetail());
			}
			* /
				
		} catch (Exception e) {
			System.out.println("Cannot initialise Foursquare");
			e.printStackTrace();
		}
		*/
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
