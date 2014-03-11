package lab3;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import twitter4j.*;
import twitter4j.conf.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Lab3 extends HttpServlet implements TwitterResponse, twitter4j.internal.http.HttpResponseCode {
	private static final long serialVersionUID = 1L;
	
	
	public String getSimpleTimeLine(Twitter twitter){
		String resultString= "";
		try {
			// it creates a query and sets the geocode
			//requirement
			Query query= new Query("#sheffield");
			query.setGeoCode(new GeoLocation(53.383, -1.483), 2,
			Query.KILOMETERS);
			//it fires the query
			QueryResult result = twitter.search(query);
			//it cycles on the tweets
			List<Status> tweets = result.getTweets();
			for (Status tweet : tweets) { ///gets the user
				User user = tweet.getUser();
				Status status= (user.isGeoEnabled())?user.getStatus():null;
				if (status==null) {
					resultString+="@" + tweet.getText() + " (" + user.getLocation()	+ ") - " + tweet.getText() + "<br>";
				} else {
					resultString+="@" + tweet.getText() + " (" + ((status!=null&&status.getGeoLocation()!=null) ? status.getGeoLocation().getLatitude()	+","+status.getGeoLocation().getLongitude()	: user.getLocation()) + ") - " + tweet.getText() + "<br>";
				}
			}
		} catch (Exception te) {
			te.printStackTrace();
			System.out.println("Failed to search tweets:" +
			te.getMessage());
			System.exit(-1);
		}
		return resultString;
	}
	
	
	
	private String getStephenFryTweets(Twitter t) throws TwitterException {
		String result = "";
		List<Status> tweets = t.getUserTimeline("stephenfry");
		for (int i=0; i<5; i++) {
			result += tweets.get(i).getText();
			result += "<br>";
		}
		return result;
	}
		
	
	private Twitter initTwitter() throws Exception {
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
		return (new TwitterFactory(cb.build()).getInstance());
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		Twitter twitter = null;
		try {
			twitter = initTwitter();
			//out.println( twitter.showUser("luke_heavens").getName() );
			//out.println( "<img src=\"" + twitter.showUser("luke_heavens").getProfileImageURLHttps() + "\" />" );
			//out.println( twitter.showStatus((long)400673938755567616.0) );
			
			//Status status = twitter.updateStatus("test");
			//out.println( "Successfully updated status to \"" + status.getText() + "\"" );
			
			//out.println( twitter.showStatus((long) 439454832261603328.0).getText() );
			
			//out.println( getStephenFryTweets(twitter) );
			
			out.println(getSimpleTimeLine(twitter));
		} catch (Exception e) {
			out.println("Cannot initialise Twitter");
			e.printStackTrace();
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public int getAccessLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RateLimitStatus getRateLimitStatus() {
		// TODO Auto-generated method stub
		return null;
	}

}
